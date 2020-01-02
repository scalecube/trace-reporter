package io.scalecube.trace.service.reporter;

import java.time.Duration;
import reactor.core.Disposable;

public abstract class Reporter implements AutoCloseable {

  protected int warmupTime = 1;
  protected int warmupIterations = 1;
  protected boolean warmupFinished = false;

  protected Duration reportDelay = Duration.ofMillis(warmupTime * warmupIterations);
  protected Disposable disposable;

  public Reporter() {
    super();
  }

  public abstract <T> T start();

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

  /**
   * setup owner of the repo.
   *
   * @param owner of the repo
   * @return this
   */
  public <T extends Reporter> T owner(String owner) {
    PerfromanceReporter.OWNER = owner;
    return (T) this;
  }

  /**
   * setup repo name.
   *
   * @param repo name.
   * @return this
   */
  public <T extends Reporter> T repo(String repo) {
    PerfromanceReporter.REPO = repo;
    return (T) this;
  }
}
