package io.tickerstorm.data;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import io.tickerstorm.DataLoadSchedulerConfig;
import io.tickerstorm.dao.MarketDataDao;
import io.tickerstorm.entity.Candle;
import io.tickerstorm.entity.MarketData;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.cassandra.core.CassandraOperations;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.io.Files;

@ContextConfiguration(classes = { DataLoadSchedulerConfig.class })
public class GoogleDataQueryITCase extends AbstractTestNGSpringContextTests {

  GoogleDataQuery query;

  @Autowired
  private DataQueryClient client;

  @Qualifier("historical")
  @Autowired
  EventBus bus;

  @Autowired
  private MarketDataDao dao;

  @Autowired
  private CassandraOperations session;

  Object verifier;

  boolean verified = false;

  @BeforeMethod
  public void setup() throws Exception {
    verified = false;
    FileUtils.forceMkdir(new File("./data/Google"));
  }

  @AfterMethod
  public void tearDown() {
    bus.unregister(verifier);
    session.getSession().execute("TRUNCATE marketdata");
    FileUtils.deleteQuietly(new File("./data/Google/TOL.csv"));
  }

  @Test
  public void downloadGloabForext() throws Exception {

    verifier = new GoogleDataVerifier();
    bus.register(verifier);

    query = new GoogleDataQuery("TOL");
    client.query(query);

    Thread.sleep(5000);
    assertTrue(verified);

    Long count = dao.count();
    assertTrue(count > 0);

  }

  @Test
  public void testParseGoogleFile() throws Exception {

    verifier = new GoogleDataVerifier();
    bus.register(verifier);

    Files.copy(new File("./src/test/resources/data/Google/TOL.csv"), new File("./data/Google/TOL.csv"));

    Thread.sleep(20000);
    assertTrue(verified);

    Long count = dao.count();
    assertTrue(count > 0);
  }

  private class GoogleDataVerifier {

    @Subscribe
    public void onEvent(MarketData md) {

      assertNotNull(md.getSymbol());
      assertEquals(md.getSource(), "Google");
      assertNotNull(md.getTimestamp());

      Candle c = (Candle) md;
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
      verified = true;

    }

  }
}