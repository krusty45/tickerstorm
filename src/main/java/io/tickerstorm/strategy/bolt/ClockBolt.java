package io.tickerstorm.strategy.bolt;

import io.tickerstorm.entity.MarketData;
import io.tickerstorm.strategy.Clock;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;

@Component
@SuppressWarnings("serial")
public class ClockBolt extends BaseRichBolt {

  @Autowired
  private Clock clock;

  private OutputCollector collector;

  @Override
  public void execute(Tuple tuple) {
    MarketData data = (MarketData) tuple.getValueByField(Fields.MARKETDATA);
    clock.update(data.getTimestamp());
    collector.emit(new Values(clock.now()));
    collector.ack(tuple);
  }

  @Override
  public void prepare(Map arg0, TopologyContext arg1, OutputCollector arg2) {
    this.collector = arg2;
  }

  @Override
  public void declareOutputFields(OutputFieldsDeclarer arg0) {
    arg0.declare(new backtype.storm.tuple.Fields(Fields.NOW));
  }

}
