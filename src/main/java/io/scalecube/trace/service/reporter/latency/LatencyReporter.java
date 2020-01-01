package io.scalecube.trace.service.reporter.latency;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.HdrHistogram.Histogram;
import org.HdrHistogram.Recorder;
import org.agrona.CloseHelper;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

public class LatencyReporter implements AutoCloseable {

  private final Recorder histogram;

  private Histogram accumulatedHistogram;

  private boolean warmupFinished = false;
  private final LatencyListener listener;
  private int warmupTime = 1000;
  private int warmupIterations = 1000;
  private Duration reportDelay = Duration.ofMillis(warmupTime * warmupIterations);

  private Disposable disposable;

  /**
   * Launch this test reporter.
   *
   * @param listeners throughput listeners
   * @return a reporter
   */
  public static LatencyReporter create(LatencyListener... listeners) {
    return new LatencyReporter(new CompositeReportingLatencyListener(listeners));
  }

  /**
   * setup warm-up time of the test.
   *
   * @param warmupTime in millis.
   * @return ThroughputReporter
   */
  public LatencyReporter warmupTime(int warmupTime) {
    this.reportDelay = Duration.ofMillis(warmupTime * warmupIterations);
    return this;
  }

  /**
   * setup warmupIterations of the test.
   *
   * @param warmupIterations in before test starts.
   * @return ThroughputReporter
   */
  public LatencyReporter warmupIterations(int warmupIterations) {
    this.reportDelay = Duration.ofMillis(warmupTime * warmupIterations);
    return this;
  }

  /** start latency reporter. */
  public void start() {
    reportDelay = Duration.ofMillis(warmupTime * warmupIterations);
    Duration reportInterval = Duration.ofSeconds(Long.getLong("benchmark.report.interval", 1));
    this.disposable =
        Flux.interval(reportDelay, reportInterval, Schedulers.single())
            .doOnCancel(this::onTerminate)
            .subscribe(i -> this.run(), Throwable::printStackTrace);
  }

  private LatencyReporter(LatencyListener listener) {
    this.listener = listener;
    this.histogram = new Recorder(TimeUnit.SECONDS.toNanos(10), 3);
  }

  private void run() {
    if (warmupFinished) {
      Histogram intervalHistogram = histogram.getIntervalHistogram();
      if (accumulatedHistogram != null) {
        accumulatedHistogram.add(intervalHistogram);
      } else {
        accumulatedHistogram = intervalHistogram;
      }

      listener.onReport(intervalHistogram);
    } else {
      warmupFinished = true;
      histogram.reset();
    }
  }

  private void onTerminate() {
    listener.onTerminate(accumulatedHistogram);
  }

  public void onDiff(long diff) {
    histogram.recordValue(diff);
  }

  @Override
  public void close() {
    disposable.dispose();
    histogram.reset();
    CloseHelper.quietClose(listener);
  }
}
