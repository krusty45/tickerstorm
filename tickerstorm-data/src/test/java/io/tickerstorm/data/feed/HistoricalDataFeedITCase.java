package io.tickerstorm.data.feed;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.io.File;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.cassandra.core.CassandraOperations;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.io.Files;

import io.tickerstorm.common.data.eventbus.Destinations;
import io.tickerstorm.common.data.query.HistoricalFeedQuery;
import io.tickerstorm.common.entity.BaseMarker;
import io.tickerstorm.common.entity.Candle;
import io.tickerstorm.common.entity.Markers;
import io.tickerstorm.data.TestMarketDataServiceConfig;

@ContextConfiguration(classes = {TestMarketDataServiceConfig.class})
public class HistoricalDataFeedITCase extends AbstractTestNGSpringContextTests {

  @Qualifier(Destinations.REALTIME_MARKETDATA_BUS)
  @Autowired
  private EventBus realtimeBus;

  @Qualifier(Destinations.NOTIFICATIONS_BUS)
  @Autowired
  private EventBus notificationsBus;

  @Qualifier(Destinations.COMMANDS_BUS)
  @Autowired
  private EventBus queryBus;

  BaseMarker start;
  BaseMarker end;

  @Autowired
  private CassandraOperations session;

  AtomicInteger count = new AtomicInteger(0);
  int expCount = 778;

  @BeforeClass
  public void dataSetup() throws Exception {
    HistoricalDataFeedVerifier verifier = new HistoricalDataFeedVerifier();
    realtimeBus.register(verifier);
    notificationsBus.register(verifier);

    session.getSession().execute("TRUNCATE marketdata");
    FileUtils.forceMkdir(new File("./data/Google"));
    Files.copy(new File("./src/test/resources/data/Google/TOL.csv"), new File("./data/Google/TOL.csv"));
    Thread.sleep(10000);
    FileUtils.deleteQuietly(new File("./data/Google/TOL.csv"));
  }

  @Test
  public void testSimpleCandleQuery() throws Exception {

    assertEquals(count.get(), 0L);

    HistoricalFeedQuery query = new HistoricalFeedQuery("google", new String[] {"TOL"});
    query.from = LocalDateTime.of(2015, 6, 10, 0, 0);
    query.until = LocalDateTime.of(2015, 6, 11, 0, 0);
    query.periods.add(Candle.MIN_1_INTERVAL);
    query.zone = ZoneOffset.ofHours(-7);
    queryBus.post(query);

    Thread.sleep(10000);

    assertEquals(count.get(), expCount);

    assertNotNull(start);
    assertNotNull(end);
    assertEquals(start.id, query.id);
    assertEquals(end.id, query.id);
    assertEquals(start.expect, Integer.valueOf(expCount));
    assertEquals(end.expect, Integer.valueOf(0));

  }



  public class HistoricalDataFeedVerifier {

    @Subscribe
    public void onNotification(BaseMarker md) {

      if (md.getMarkers().contains(Markers.QUERY_START.toString()))
        start = (BaseMarker) md;

      if (md.getMarkers().contains(Markers.QUERY_END.toString()))
        end = (BaseMarker) md;

    }

    @Subscribe
    public void onMarketData(Candle c) {

      assertNotNull(c.getSymbol());
      assertEquals(c.getStream(), "google");
      assertNotNull(c.getTimestamp());

      assertNotNull(c.close);
      assertTrue(c.close.longValue() > 0);
      assertNotNull(c.open);
      assertTrue(c.open.longValue() > 0);
      assertNotNull(c.low);
      assertTrue(c.low.longValue() > 0);
      assertNotNull(c.high);
      assertTrue(c.high.longValue() > 0);
      assertNotNull(c.volume);
      assertTrue(c.volume.longValue() > 0);
      assertEquals(c.interval, Candle.MIN_1_INTERVAL);
      count.incrementAndGet();
    }


  }

}
