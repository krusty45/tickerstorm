package io.tickerstorm.data.query;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.io.File;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.cassandra.core.CassandraOperations;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.io.Files;

import io.tickerstorm.data.TestMarketDataServiceConfig;
import io.tickerstorm.data.dao.MarketDataDao;
import io.tickerstorm.entity.Candle;
import io.tickerstorm.entity.MarketData;
import net.engio.mbassy.bus.MBassador;
import net.engio.mbassy.listener.Handler;
import net.engio.mbassy.listener.Listener;
import net.engio.mbassy.listener.References;

@DirtiesContext
@ContextConfiguration(classes = {TestMarketDataServiceConfig.class})
public class StooqHistoricalForexQueryITCase extends AbstractTestNGSpringContextTests {

  @Autowired
  private DataQueryClient client;

  @Autowired
  private MarketDataDao dao;

  @Qualifier("historical")
  @Autowired
  MBassador<MarketData> bus;

  @Autowired
  private CassandraOperations session;

  Object verifier;

  private AtomicLong count = new AtomicLong(0);

  @BeforeMethod
  public void setup() throws Exception {
    verifier = new DownloadGloabForextVerification();
    bus.subscribe(verifier);
    FileUtils.forceMkdir(new File("./data/Stooq"));
  }

  @AfterMethod
  public void tearDown() {
    bus.unsubscribe(verifier);
    session.getSession().execute("TRUNCATE marketdata");
    FileUtils.deleteQuietly(new File("./data/Stooq/5_world_txt.zip"));
    count.set(0);
  }

  @Test
  public void parseGloabForext() throws Exception {

    Files.copy(new File("./src/test/resources/data/Stooq/5_world_txt.zip"),
        new File("./data/Stooq/5_world_txt.zip"));

    Thread.sleep(15000);

    Long daoCount = dao.count();
    assertEquals(daoCount.longValue(), 62355L);
    assertEquals(count.get(), 62355L);

  }

  @Test
  public void downloadGloabForext() throws Exception {

    StooqHistoricalForexQuery query = new StooqHistoricalForexQuery().currencies().min5();
    client.query(query);

    Thread.sleep(15000);

    Long count = dao.count();
    assertTrue(count > 0);

  }

  @Listener(references = References.Strong)
  private class DownloadGloabForextVerification {

    @Handler
    public void onEvent(MarketData md) {

      assertNotNull(md.getSymbol());
      assertEquals(md.getSource(), "Stooq");
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
      assertEquals(c.interval, Candle.MIN_5_INTERVAL);
      count.incrementAndGet();

    }

  }
}
