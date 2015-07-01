package io.tickerstorm.feed;

import io.tickerstorm.dao.MarketDataDto;
import io.tickerstorm.entity.Candle;

import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.cassandra.core.CassandraOperations;
import org.springframework.stereotype.Repository;

import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

@Repository
public class HistoricalDataFeed {

  private static final DateTimeFormatter dateFormat = DateTimeFormat.forPattern("yyyyMMdd");

  private static final Logger logger = LoggerFactory.getLogger(HistoricalDataFeed.class);

  @Qualifier("query")
  @Autowired
  private EventBus bus;

  @Autowired
  private CassandraOperations cassandra;

  @Value("${cassandra.keyspace}")
  private String keyspace;

  @PostConstruct
  public void init() {
    bus.register(this);
  }

  @PreDestroy
  public void destroy() {
    bus.unregister(this);
  }

  @Subscribe
  public void onQuery(HistoricalFeedQuery query) {

    DateTime start = query.interval.getStart();
    DateTime end = query.interval.getEnd();
    DateTime date = start;
    Set<String> dates = new java.util.HashSet<>();
    dates.add(date.toString(dateFormat));

    for (String s : query.symbols) {

      while (!date.isEqual(end)) {

        if (date.isBefore(end))
          date = date.plusDays(1);

        dates.add(date.toString(dateFormat));
      }

      Select select = QueryBuilder.select().from("marketdata");
      select.where(QueryBuilder.eq("symbol", s.toLowerCase())).and(QueryBuilder.in("date", dates.toArray(new String[] {})))
          .and(QueryBuilder.eq("type", Candle.TYPE.toLowerCase())).and(QueryBuilder.eq("source", query.source.toLowerCase()))
          .and(QueryBuilder.eq("interval", query.periods.iterator().next()));

      logger.info(select.toString());

      List<MarketDataDto> dtos = cassandra.select(select, MarketDataDto.class);

      logger.info("Fetched " + dtos.size() + " results.");

      for (MarketDataDto dto : dtos) {
        bus.post(dto.toMarketData());
      }
    }
  }
}