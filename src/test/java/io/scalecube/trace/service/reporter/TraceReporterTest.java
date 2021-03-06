package io.scalecube.trace.service.reporter;

import io.scalecube.trace.EnviromentVariables;
import io.scalecube.trace.TraceReporter;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class TraceReporterTest {

  @Disabled
  @Test
  void testFlow() throws Exception {
    final String owner = EnviromentVariables.owner("scalecube");
    final String repo = EnviromentVariables.repo("github-gateway");
    final String commitId = EnviromentVariables.sha("1");
    final String traceReportUrl =
        EnviromentVariables.url("https://scalecube-robokit.exchange.om2.com/traces");

    try (TraceReporter reporter = new TraceReporter()) {
      reporter.addY("latency-1", "latency", 72);
      reporter.addY("latency-1", "latency", 63);
      reporter.addY("latency-1", "latency", 45);

      reporter.addY("latency-2", "latency", 52);
      reporter.addY("latency-2", "latency", 161);
      reporter.addY("latency-2", "latency", 151);
      reporter.addY("latency-2", "latency", 143);

      reporter.addY("throughput-1", "throughput", 52);
      reporter.addY("throughput-1", "throughput", 53);
      reporter.addY("throughput-1", "throughput", 35);
      reporter.addY("throughput-1", "throughput", 62);

      reporter.addY("throughput-2", "throughput", 361);
      reporter.addY("throughput-2", "throughput", 151);
      reporter.addY("throughput-2", "throughput", 343);

      Publisher.publish(traceReportUrl, owner, repo, commitId, reporter.traces());
    }
  }
}
