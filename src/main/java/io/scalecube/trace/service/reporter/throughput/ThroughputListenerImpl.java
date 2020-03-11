package io.scalecube.trace.service.reporter.throughput;

import io.scalecube.trace.service.reporter.AbstractPerformanceListener;
import io.scalecube.trace.service.reporter.PublisherContext;
import java.util.function.UnaryOperator;
import reactor.core.Exceptions;

public final class ThroughputListenerImpl extends AbstractPerformanceListener
    implements ThroughputListener, Cloneable {

  public ThroughputListenerImpl publisher(UnaryOperator<PublisherContext> op) {
    ThroughputListenerImpl c = clone();
    c.publisherContext = op.apply(c.publisherContext);
    return c;
  }

  @Override
  public void onReport(double messagesPerSec, double bytesPerSec) {
    reporter.addY(publisherContext.testName(), "throughput", messagesPerSec);
  }

  @Override
  public ThroughputListenerImpl clone() {
    try {
      return (ThroughputListenerImpl) super.clone();
    } catch (CloneNotSupportedException e) {
      throw Exceptions.propagate(e);
    }
  }
}
