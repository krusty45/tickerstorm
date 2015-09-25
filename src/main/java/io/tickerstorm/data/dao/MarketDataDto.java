package io.tickerstorm.data.dao;

import io.tickerstorm.entity.Candle;
import io.tickerstorm.entity.MarketData;
import io.tickerstorm.entity.Quote;
import io.tickerstorm.entity.Tick;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.util.HashMap;
import java.util.Map;

import org.dozer.DozerBeanMapper;
import org.springframework.data.cassandra.mapping.Table;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;

@Table("marketdata")
@SuppressWarnings("serial")
public class MarketDataDto implements Serializable {

  public static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("uuuuMMdd");
  public static final DozerBeanMapper mapper = new DozerBeanMapper();

  static {
    mapper.setMappingFiles(Lists.newArrayList("dozer.xml"));
  }

  public static MarketDataDto convert(MarketData data) {

    MarketDataDto dto = new MarketDataDto();
    PrimaryKey key = new PrimaryKey();
    try {

      mapper.map(data, dto);
      mapper.map(data, key);
      key.date = dateFormatter.format(LocalDateTime.ofInstant(data.getTimestamp(), ZoneOffset.UTC));
      key.hour = LocalDateTime.ofInstant(data.getTimestamp(), ZoneOffset.UTC).get(ChronoField.HOUR_OF_DAY);
      key.min = LocalDateTime.ofInstant(data.getTimestamp(), ZoneOffset.UTC).get(ChronoField.MINUTE_OF_HOUR);
      dto.primarykey = key;
      dto.primarykey.symbol = dto.primarykey.symbol.toLowerCase();
      dto.primarykey.source = dto.primarykey.source.toLowerCase();

      if (dto.primarykey.interval != null)
        dto.primarykey.interval = dto.primarykey.interval.toLowerCase();

    } catch (Exception e) {
      Throwables.propagate(e);
    }

    return dto;

  }

  @org.springframework.data.cassandra.mapping.PrimaryKey
  public PrimaryKey primarykey;

  public BigDecimal ask;
  public BigDecimal bid;
  public BigDecimal price;
  public BigDecimal askSize;
  public BigDecimal bidSize;
  public BigDecimal volume;
  public BigDecimal close;
  public BigDecimal open;
  public BigDecimal high;
  public BigDecimal low;
  public BigDecimal quantity;
  public String exchange;
  public Map<String, Object> properties = new HashMap<String, Object>();

  public BigDecimal getAsk() {
    return ask;
  }

  public BigDecimal getAskSize() {
    return askSize;
  }

  public BigDecimal getBid() {
    return bid;
  }

  public BigDecimal getBidSize() {
    return bidSize;
  }

  public BigDecimal getClose() {
    return close;
  }

  public String getExchange() {
    return exchange;
  }

  public BigDecimal getHigh() {
    return high;
  }

  public BigDecimal getLow() {
    return low;
  }

  public BigDecimal getOpen() {
    return open;
  }

  public BigDecimal getPrice() {
    return price;
  }

  public PrimaryKey getPrimarykey() {
    return primarykey;
  }

  public Map<String, Object> getProperties() {
    return properties;
  }

  public BigDecimal getQuantity() {
    return quantity;
  }

  public BigDecimal getVolume() {
    return volume;
  }

  public void setAsk(BigDecimal ask) {
    this.ask = ask;
  }

  public void setAskSize(BigDecimal askSize) {
    this.askSize = askSize;
  }

  public void setBid(BigDecimal bid) {
    this.bid = bid;
  }

  public void setBidSize(BigDecimal bidSize) {
    this.bidSize = bidSize;
  }

  public void setClose(BigDecimal close) {
    this.close = close;
  }

  public void setExchange(String exchange) {
    this.exchange = exchange;
  }

  public void setHigh(BigDecimal high) {
    this.high = high;
  }

  public void setLow(BigDecimal low) {
    this.low = low;
  }

  public void setOpen(BigDecimal open) {
    this.open = open;
  }

  public void setPrice(BigDecimal price) {
    this.price = price;
  }

  public void setPrimarykey(PrimaryKey primarykey) {
    this.primarykey = primarykey;
  }

  public void setProperties(Map<String, Object> properties) {
    this.properties = properties;
  }

  public void setQuantity(BigDecimal quantity) {
    this.quantity = quantity;
  }

  public void setVolume(BigDecimal volume) {
    this.volume = volume;
  }

  public MarketData toMarketData() {

    MarketData c = null;

    try {
      if (Candle.TYPE.equals(primarykey.type)) {
        c = mapper.map(this, Candle.class);
      } else if (Quote.TYPE.equals(primarykey.type)) {
        c = mapper.map(this, Quote.class);
      } else if (Tick.TYPE.equals(primarykey.type)) {
        c = mapper.map(this, Tick.class);
      }

    } catch (Exception e) {
      Throwables.propagate(e);
    }

    return c;

  }

}