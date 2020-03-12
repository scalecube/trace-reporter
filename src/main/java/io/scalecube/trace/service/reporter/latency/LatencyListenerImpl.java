package io.scalecube.trace.service.reporter.latency;

import io.scalecube.trace.service.reporter.AbstractPerformanceListener;
import io.scalecube.trace.service.reporter.PublisherContext;
import java.util.Collection;
import java.util.function.UnaryOperator;
import org.HdrHistogram.Histogram;
import reactor.core.Exceptions;

public final class LatencyListenerImpl extends AbstractPerformanceListener
    implements LatencyListener, Cloneable {

  private double scalingRatio = 1000.0; // microseconds;
  private double[] percentiles = {50d, 75d, 90d, 99d};

  /**
   * {@link PublisherContext} settings function.
   *
   * @param op operator
   * @return new {@code LatencyListenerImpl} instance
   */
  public LatencyListenerImpl publisher(UnaryOperator<PublisherContext> op) {
    LatencyListenerImpl c = clone();
    c.publisherContext = op.apply(c.publisherContext);
    return c;
  }

  /**
   * Setter for {@code scalingRatio}.
   *
   * @param scalingRatio scaling ratio
   * @return new {@code LatencyListenerImpl} instance
   */
  public LatencyListenerImpl scalingRatio(double scalingRatio) {
    LatencyListenerImpl c = clone();
    c.scalingRatio = scalingRatio;
    return c;
  }

  /**
   * Setter for {@code percentiles} function.
   *
   * @param percentiles percentiles
   * @return new {@code LatencyListenerImpl} instance
   */
  public LatencyListenerImpl percentiles(Collection<Double> percentiles) {
    LatencyListenerImpl c = clone();
    c.percentiles = percentiles.stream().mapToDouble(Double::doubleValue).toArray();
    return c;
  }

  /**
   * Setter for {@code percentiles}.
   *
   * @param percentiles percentiles
   * @return new {@code LatencyListenerImpl} instance
   */
  public LatencyListenerImpl percentiles(double... percentiles) {
    LatencyListenerImpl c = clone();
    c.percentiles = percentiles;
    return c;
  }

  @Override
  public void onReport(Histogram histogram) {
    for (double d : percentiles) {
      reporter.addY(
          "[p" + d + "] " + publisherContext.testName(),
          "latency",
          histogram.getValueAtPercentile(d) / scalingRatio);
    }
  }

  @Override
  public void onTerminate(Histogram accumulatedHistogram) {
    // nothing to do here
  }

  @Override
  public LatencyListenerImpl clone() {
    try {
      return (LatencyListenerImpl) super.clone();
    } catch (CloneNotSupportedException e) {
      throw Exceptions.propagate(e);
    }
  }
}
