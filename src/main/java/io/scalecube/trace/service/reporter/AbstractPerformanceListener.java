package io.scalecube.trace.service.reporter;

import io.scalecube.trace.TraceReporter;

public abstract class AbstractPerformanceListener implements AutoCloseable {

  protected final TraceReporter reporter = new TraceReporter();

  protected PublisherContext publisherContext = new PublisherContext();

  @Override
  public final void close() {
    try {
      Publisher.publish(
          publisherContext.traceReportUrl(),
          publisherContext.owner(),
          publisherContext.repo(),
          publisherContext.commitId(),
          reporter.traces());
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }
}
