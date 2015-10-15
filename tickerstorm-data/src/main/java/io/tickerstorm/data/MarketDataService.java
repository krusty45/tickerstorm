package io.tickerstorm.data;

import javax.jms.ConnectionFactory;
import javax.jms.Session;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportResource;
import org.springframework.data.cassandra.repository.config.EnableCassandraRepositories;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.core.JmsTemplate;

import io.tickerstorm.common.data.CommonContext;
import io.tickerstorm.common.data.eventbus.ByDestinationNameJmsResolver;
import io.tickerstorm.common.data.eventbus.Destinations;
import io.tickerstorm.common.data.eventbus.EventBusToJMSBridge;
import io.tickerstorm.common.data.eventbus.JMSToEventBusBridge;
import io.tickerstorm.common.data.feed.HistoricalFeedQuery;
import io.tickerstorm.common.entity.MarketData;
import io.tickerstorm.data.dao.MarketDataDao;
import net.engio.mbassy.bus.MBassador;
import net.engio.mbassy.bus.common.Properties;
import net.engio.mbassy.bus.config.BusConfiguration;
import net.engio.mbassy.bus.config.Feature;
import net.engio.mbassy.bus.error.IPublicationErrorHandler;

@EnableJms
@SpringBootApplication
@EnableCassandraRepositories(basePackageClasses = MarketDataDao.class)
@ImportResource(value = {"classpath:/META-INF/spring/cassandra-beans.xml"})
@ComponentScan(basePackages = {"io.tickerstorm.data"})
@Import({CommonContext.class})
public class MarketDataService {

  public static void main(String[] args) throws Exception {
    SpringApplication.run(MarketDataService.class, args);
  }

  // @Bean(initMethod = "start", destroyMethod = "stop")
  // public BrokerService startActiveMQ() throws Exception {
  // BrokerService broker = new BrokerService();
  // broker.setBrokerName("tickerstorm");
  // TransportConnector connector = new TransportConnector();
  // connector.setUri(new URI(transport));
  // broker.addConnector(connector);
  // broker.setPersistent(false);;
  // return broker;
  // }

  @Bean
  public JmsTemplate buildJmsTemplate(ConnectionFactory factory) {
    JmsTemplate template = new JmsTemplate(factory);
    template.setDestinationResolver(new ByDestinationNameJmsResolver());
    template.setSessionAcknowledgeMode(Session.CLIENT_ACKNOWLEDGE);
    template.setTimeToLive(2000);
    return template;
  }

  @Qualifier("historical")
  @Bean(destroyMethod = "shutdown")
  public MBassador<MarketData> buildEventBus(IPublicationErrorHandler handler) {
    return new MBassador<MarketData>(new BusConfiguration().addFeature(Feature.SyncPubSub.Default())
        .addFeature(Feature.AsynchronousHandlerInvocation.Default(1, 4)).addFeature(Feature.AsynchronousMessageDispatch.Default())
        .addPublicationErrorHandler(handler).setProperty(Properties.Common.Id, "historical bus"));
  }

  @Bean
  public EventBusToJMSBridge buildRealtimeJmsBridge(@Qualifier("realtime") MBassador<MarketData> eventbus, JmsTemplate template) {
    return new EventBusToJMSBridge(eventbus, Destinations.TOPIC_REALTIME_MARKETDATA, template);
  }

  @Bean
  public JMSToEventBusBridge buildQueryEventBridge(@Qualifier("query") MBassador<HistoricalFeedQuery> queryBus) {
    JMSToEventBusBridge bridge = new JMSToEventBusBridge();
    bridge.setQueryBus(queryBus);
    return bridge;
  }

}
