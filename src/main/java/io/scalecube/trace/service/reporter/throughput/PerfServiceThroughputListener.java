package io.scalecube.trace.service.reporter.throughput;

import io.scalecube.trace.EnviromentVariables;
import io.scalecube.trace.TraceReporter;
import io.scalecube.trace.service.reporter.PerfromanceReporter;

public class PerfServiceThroughputListener implements ThroughputListener {

  private final TraceReporter reporter;
  private final String testName = EnviromentVariables.testName("TEST_NAME");
  private final String commitId = EnviromentVariables.sha("1");

  /** Ctor. */
  public PerfServiceThroughputListener() {
    this.reporter = new TraceReporter();
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
  public void onReport(double messagesPerSec, double bytesPerSec) {
    reporter.addY(this.testName, "throughput", messagesPerSec);
  }
}
