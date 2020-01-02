package io.scalecube.trace.service.reporter.latency;

import io.scalecube.trace.EnviromentVariables;
import io.scalecube.trace.TraceReporter;
import io.scalecube.trace.service.reporter.PerfromanceReporter;
import org.HdrHistogram.Histogram;

public class PerfServiceLatencyListener implements LatencyListener {

  private final TraceReporter reporter;
  private final String testName = EnviromentVariables.testName("TEST_NAME");
  private final String commitId = EnviromentVariables.sha("1");
  private final double scalingRatio = EnviromentVariables.scalingRatio(1000.0); // microseconds
  private double[] percentiles = new double[] {50d, 75d, 90d, 99d};

  public PerfServiceLatencyListener(double... percentiles) {
    this.reporter = new TraceReporter();
    this.percentiles = percentiles;
  }

  @Override
  public void onReport(Histogram histogram) {
    for (double d : percentiles) {
      reporter.addY(
          "[p" + d + "] " + this.testName,
          "latency",
          histogram.getValueAtPercentile(d) / scalingRatio);
    }
  }

  @Override
  public void close() {
    try {
      if (commitId != null && !commitId.trim().isEmpty()) {
        PerfromanceReporter.publish(reporter);
      }
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  @Override
  public void onTerminate(Histogram accumulatedHistogram) {
    // nothing to do here
  }
}
