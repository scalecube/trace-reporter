package io.scalecube.trace.service.reporter;

import java.time.Duration;
import reactor.core.Disposable;

public abstract class Reporter implements AutoCloseable {

  protected int warmupTime = 1000;
  protected int warmupIterations = 1000;
  protected boolean warmupFinished = false;

  protected Duration reportDelay = Duration.ofMillis(warmupTime * warmupIterations);
  protected Disposable disposable;

  public Reporter() {
    super();
  }

  /**
   * setup warm-up time of the test.
   *
   * @param warmupTime in millis.
   * @return ThroughputReporter
   */
  public <T extends Reporter> T warmupTime(Duration warmupTime) {
    this.reportDelay = Duration.ofMillis(warmupTime.toMillis() * warmupIterations);
    return (T) this;
  }

  /**
   * setup warmupIterations of the test.
   *
   * @param warmupIterations in before test starts.
   * @return ThroughputReporter
   */
  public <T extends Reporter> T warmupIterations(int warmupIterations) {
    this.reportDelay = Duration.ofMillis(warmupTime * warmupIterations);
    return (T) this;
  }
}
