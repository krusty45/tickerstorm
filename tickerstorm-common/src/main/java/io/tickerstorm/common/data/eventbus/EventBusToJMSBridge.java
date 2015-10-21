package io.tickerstorm.common.data.eventbus;

import java.io.Serializable;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;

import net.engio.mbassy.bus.MBassador;
import net.engio.mbassy.listener.Handler;
import net.engio.mbassy.listener.Listener;
import net.engio.mbassy.listener.References;

@Listener(references = References.Strong)
public class EventBusToJMSBridge {

  private static final Logger logger = LoggerFactory.getLogger(EventBusToJMSBridge.class);

  public EventBusToJMSBridge(MBassador<?> eventBus, String destination, JmsTemplate template) {
    this.bus = eventBus;
    this.destination = destination;
    this.template = template;
  }

  private MBassador<?> bus;
  private JmsTemplate template;
  private String destination;

  @PostConstruct
  public void init() {
    bus.subscribe(this);
  }

  @PreDestroy
  public void destroy() {
    bus.unsubscribe(this);
  }

  @Handler
  public void onEvent(Serializable data) {

    template.send(destination, new MessageCreator() {
      @Override
      public Message createMessage(Session session) throws JMSException {
        logger.debug("Dispatching " + data.toString());
        Message m = session.createObjectMessage(data);
        return m;
      }
    });
  }



}