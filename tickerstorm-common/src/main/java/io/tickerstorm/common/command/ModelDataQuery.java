package io.tickerstorm.common.command;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.function.Predicate;

@SuppressWarnings("serial")
public class ModelDataQuery extends Command implements DataFeedQuery {

  public enum OrderBy {
    ASC, DESC;
  }

  public Instant from = Instant.now().minus(365, ChronoUnit.DAYS);
  public Instant until = Instant.now();
  public OrderBy sort = OrderBy.DESC;

  public ModelDataQuery(String stream) {
    super(stream, "query.modeldata");
    this.markers.add(Markers.MODEL_DATA.toString());
    this.markers.add(Markers.QUERY.toString());
  }

  public Predicate<Notification> isDone() {
    return (n -> this.getStream().equalsIgnoreCase(n.getStream()) && n.markers.contains(Markers.MODEL_DATA.toString())
        && n.markers.contains(Markers.QUERY.toString()) && n.markers.contains(Markers.END.toString()) && n.id.equals(this.id));
  }

  @Override
  public boolean isValid() {
    return (super.validate() & from != null && until != null && from.isBefore(until) && sort != null);
  }
}