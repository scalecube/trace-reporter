package io.scalecube.trace.service.reporter;

import java.time.Duration;

public abstract class AbstractPerformanceReporter<T extends AbstractPerformanceReporter<?>>
    implements AutoCloseable {

  protected int warmupTime = 1;
  protected int warmupIterations = 1;
  protected boolean warmupFinished = false;

  protected Duration reportDelay = Duration.ofMillis(warmupTime * warmupIterations);
  protected Duration reportInterval =
      Duration.ofSeconds(Long.getLong("benchmark.report.interval", 1));

  public abstract T start();

  /**
   * Setup warm-up time of the test (recalculates {@code reportDelay}).
   *
   * @param warmupTime in millis.
   * @return ThroughputReporter
   */
  public T warmupTime(Duration warmupTime) {
    this.reportDelay = Duration.ofMillis(warmupTime.toMillis() * warmupIterations);
    //noinspection unchecked
    return (T) this;
  }

  /**
   * Setup warmupIterations of the test (recalculates {@code reportDelay}).
   *
   * @param warmupIterations number before test starts.
   * @return ThroughputReporter
   */
  public T warmupIterations(int warmupIterations) {
    this.reportDelay = Duration.ofMillis(warmupTime * warmupIterations);
    //noinspection unchecked
    return (T) this;
  }
}
