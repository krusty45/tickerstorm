package io.tickerstorm;

import static org.testng.Assert.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.data.cassandra.core.CassandraOperations;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import io.tickerstorm.common.data.eventbus.Destinations;
import io.tickerstorm.common.data.query.ModelDataQuery;
import io.tickerstorm.common.entity.BaseMarker;
import io.tickerstorm.common.entity.Candle;
import io.tickerstorm.common.entity.Markers;
import io.tickerstorm.common.entity.Session;
import io.tickerstorm.common.entity.SessionFactory;
import io.tickerstorm.data.dao.MarketDataDao;
import io.tickerstorm.data.dao.ModelDataDao;
import io.tickerstorm.data.dao.ModelDataDto;
import io.tickerstorm.strategy.processor.flow.NumericChangeProcessor;

@ContextConfiguration(classes = {IntegrationTestContext.class})
@IntegrationTest
public class ModelDataEndToEndITCase extends AbstractTestNGSpringContextTests {

  private static final Logger logger = LoggerFactory.getLogger(ModelDataEndToEndITCase.class);

  @Autowired
  private CassandraOperations session;

  @Autowired
  private ModelDataDao modelDao;

  @Autowired
  private MarketDataDao dataDao;

  @Autowired
  private SessionFactory sFactory;

  private ModelDataDto dto;
  private Session s;

  @Qualifier(Destinations.BROKER_MARKETDATA_BUS)
  @Autowired
  private EventBus brokderFeed;

  @Qualifier(Destinations.NOTIFICATIONS_BUS)
  @Autowired
  private EventBus notificationBus;

  @Qualifier(Destinations.COMMANDS_BUS)
  @Autowired
  private EventBus queryBus;

  @BeforeMethod
  public void init() throws Exception {

    notificationBus.register(this);

    // let everything start up.
    session.getSession().execute("TRUNCATE marketdata");
    session.getSession().execute("TRUNCATE modeldata");
    Thread.sleep(5000);

    s = sFactory.newSession();
    s.config.put(NumericChangeProcessor.PERIODS_CONFIG_KEY, "2");
    s.start();

  }

  @Test
  public void verifyModelDataStored() throws Exception {

    AtomicBoolean triggeredModel = new AtomicBoolean(false);
    AtomicBoolean triggeredMarket = new AtomicBoolean(false);
    AtomicBoolean triggeredRetro = new AtomicBoolean(false);
    VerifyModelDataStoredHandler handler1 = new VerifyModelDataStoredHandler(triggeredModel);
    VerifyMarketDataStoredHandler handler2 = new VerifyMarketDataStoredHandler(triggeredMarket);

    notificationBus.register(handler1);
    notificationBus.register(handler2);


    Candle c = new Candle("goog", "google", Instant.now(), BigDecimal.ONE, BigDecimal.TEN, BigDecimal.ONE, BigDecimal.ONE, "1m", 1000);
    brokderFeed.post(c);
    Thread.sleep(25000);

    Assert.assertTrue(triggeredMarket.get());
    Assert.assertTrue(triggeredModel.get());

    ModelDataQuery q = new ModelDataQuery(s.stream);
    VerifyRetroModelQueryEnded handler3 = new VerifyRetroModelQueryEnded(triggeredRetro, q);
    notificationBus.register(handler3);
    queryBus.post(q);

    Thread.sleep(2000);
    Assert.assertTrue(triggeredRetro.get());

    notificationBus.register(handler1);
    notificationBus.register(handler2);
    notificationBus.register(handler3);
  }

  private class VerifyMarketDataStoredHandler {

    AtomicBoolean result;

    public VerifyMarketDataStoredHandler(AtomicBoolean result) {
      this.result = result;
    }

    @Subscribe
    public void onData(BaseMarker marker) throws Exception {
      if ("google".equals(marker.stream) && marker.markers.contains(Markers.MARKET_DATA_SAVED.toString())) {
        Assert.assertEquals(new Integer(1), marker.expect);
        result.set(true);
      }
    }
  }

  private class VerifyRetroModelQueryEnded {

    AtomicBoolean result;
    ModelDataQuery q;

    public VerifyRetroModelQueryEnded(AtomicBoolean result, ModelDataQuery q) {
      this.result = result;
      this.q = q;
    }

    @Subscribe
    public void onData(BaseMarker marker) throws Exception {

      if (marker.id.equals(q.id) && marker.markers.contains(Markers.QUERY_END.toString())) {
        result.set(true);
      }
    }

  }

  private class VerifyModelDataStoredHandler {

    AtomicBoolean result;

    public VerifyModelDataStoredHandler(AtomicBoolean result) {
      this.result = result;
    }

    @Subscribe
    public void onData(BaseMarker marker) throws Exception {

      if (s.stream.equals(marker.stream) && marker.markers.contains(Markers.MODEL_DATA_SAVED.toString())) {

        Assert.assertTrue(marker.expect > 0);

        Thread.sleep(2000);

        long count = modelDao.count();
        assertTrue(count > 0);

        Set<String> fieldNames = new java.util.HashSet<>();

        modelDao.findAll(s.stream).forEach(d -> {
          d.asFields().stream().forEach(f -> {
            fieldNames.add(f.getName());
          });
        });

        Assert.assertTrue(fieldNames.size() > 0);

        result.set(true);
      }

    }

  }


  @AfterMethod
  public void end() {
    s.end();
  }

}
