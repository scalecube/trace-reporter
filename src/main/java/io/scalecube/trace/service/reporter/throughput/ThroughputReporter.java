package io.scalecube.trace.service.reporter.throughput;

import java.time.Duration;
import java.util.concurrent.atomic.LongAdder;
import org.agrona.CloseHelper;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

/** Tracker and listener of throughput rates. */
public class ThroughputReporter implements AutoCloseable {

  private final ThroughputListener listener;

  private final LongAdder totalBytes = new LongAdder();
  private final LongAdder totalMessages = new LongAdder();

  private long lastTotalBytes;
  private long lastTotalMessages;
  private long lastTimestamp;
  private int warmupTime = 1000;
  private int warmupIterations = 1000;
  private Duration reportDelay = Duration.ofMillis(warmupTime * warmupIterations);
  private boolean warmupFinished = false;

  private long reportIntervalNs;
  private Disposable disposable;
  /**
   * Launch this test reporter.
   *
   * @param listeners throughput listeners
   * @return a reporter
   */
  public static ThroughputReporter create(ThroughputListener... listeners) {
    return new ThroughputReporter(new CompositeThroughputListener(listeners));
  }

  /**
   * setup warm-up time of the test.
   *
   * @param warmupTime in millis.
   * @return ThroughputReporter
   */
  public ThroughputReporter warmupTime(int warmupTime) {
    this.reportDelay = Duration.ofMillis(warmupTime * warmupIterations);
    return this;
  }

  /**
   * setup warmupIterations of the test.
   *
   * @param warmupIterations in before test starts.
   * @return ThroughputReporter
   */
  public ThroughputReporter warmupIterations(int warmupIterations) {
    this.reportDelay = Duration.ofMillis(warmupTime * warmupIterations);
    return this;
  }

  /**
   * Create rate reporter.
   *
   * @param listener throughput listener
   */
  private ThroughputReporter(ThroughputListener listener) {
    this.disposable = null;
    this.listener = listener;
  }

  /** Start the reporter collector. */
  public void start() {
    reportDelay = Duration.ofMillis(warmupTime * warmupIterations);
    Duration reportInterval = Duration.ofSeconds(Long.getLong("benchmark.report.interval", 1));
    this.reportIntervalNs = reportInterval.toNanos();
    this.disposable =
        Flux.interval(reportDelay, reportInterval, Schedulers.single())
            .subscribe(i -> this.run(), Throwable::printStackTrace);
  }

  private void run() {
    long currentTotalMessages = totalMessages.longValue();
    long currentTotalBytes = totalBytes.longValue();
    long currentTimestamp = System.nanoTime();

    long timeSpanNs = currentTimestamp - lastTimestamp;
    double messagesPerSec =
        ((currentTotalMessages - lastTotalMessages) * (double) reportIntervalNs)
            / (double) timeSpanNs;
    double bytesPerSec =
        ((currentTotalBytes - lastTotalBytes) * (double) reportIntervalNs) / (double) timeSpanNs;

    lastTotalBytes = currentTotalBytes;
    lastTotalMessages = currentTotalMessages;
    lastTimestamp = currentTimestamp;

    if (warmupFinished) {
      listener.onReport(messagesPerSec, bytesPerSec);
    } else {
      warmupFinished = true;
    }
  }

  /**
   * Notify rate reporter of number of messages and bytes received, sent, etc.
   *
   * @param messages received, sent, etc.
   * @param bytes received, sent, etc.
   */
  public void onMessage(final long messages, final long bytes) {
    totalBytes.add(bytes);
    totalMessages.add(messages);
  }

  @Override
  public void close() {
    disposable.dispose();
    CloseHelper.quietClose(listener);
  }
}
