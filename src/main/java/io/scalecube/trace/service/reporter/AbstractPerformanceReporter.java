package io.scalecube.trace.service.reporter;

import java.time.Duration;
import reactor.core.Disposable;

public abstract class AbstractPerformanceReporter<T> implements AutoCloseable {

  protected int warmupTime = 1;
  protected int warmupIterations = 1;
  protected boolean warmupFinished = false;

  protected Duration reportDelay = Duration.ofMillis(warmupTime * warmupIterations);
  protected Disposable disposable;

  public abstract T start();

  /**
   * setup warm-up time of the test.
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
   * setup warmupIterations of the test.
   *
   * @param warmupIterations in before test starts.
   * @return ThroughputReporter
   */
  public T warmupIterations(int warmupIterations) {
    this.reportDelay = Duration.ofMillis(warmupTime * warmupIterations);
    //noinspection unchecked
    return (T) this;
  }
}
