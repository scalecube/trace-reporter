package io.scalecube.trace.service.reporter;

import io.scalecube.trace.EnviromentVariables;
import io.scalecube.trace.service.reporter.latency.LatencyListenerImpl;
import io.scalecube.trace.service.reporter.latency.LatencyReporter;
import io.scalecube.trace.service.reporter.throughput.ThroughputListenerImpl;
import io.scalecube.trace.service.reporter.throughput.ThroughputReporter;
import java.time.Duration;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class LatencyReporterTest {

  @Disabled
  @Test
  void testPerformance() throws Exception {
    final String testName = EnviromentVariables.testName("TEST_NAME");
    final String commitId = EnviromentVariables.sha("1");
    final String owner = EnviromentVariables.owner("scalecube");
    final String repo = EnviromentVariables.repo("github-gateway");

    // Measure latency
    LatencyReporter latency =
        LatencyReporter.create(
                new LatencyListenerImpl()
                    .publisher(r -> r.testName(testName))
                    .publisher(r -> r.commitId(commitId))
                    .publisher(r -> r.owner(owner))
                    .publisher(r -> r.repo(repo))
                    .scalingRatio(1000.0)
                    .percentiles(50d, 99d))
            .warmupIterations(3)
            .warmupTime(Duration.ofMillis(3))
            .start();

    // simulating a test that reports the diff measurement.
    for (int i = 0; i < 3000; i++) {
      latency.onDiff(new Double((Math.random() * 100)).longValue());
      Thread.sleep(1);
    }

    // Measure throughput
    ThroughputReporter throughput =
        ThroughputReporter.create(
                new ThroughputListenerImpl()
                    .publisher(r -> r.testName(testName))
                    .publisher(r -> r.commitId(commitId))
                    .publisher(r -> r.owner(owner))
                    .publisher(r -> r.repo(repo)))
            .warmupIterations(3)
            .warmupTime(Duration.ofMillis(3))
            .start();

    // simulating a test that reports the TPS measurement.
    for (int i = 0; i < 3000; i++) {
      throughput.onMessage(1, new Double((Math.random() * 100)).longValue());
      Thread.sleep(1);
    }

    // closes the resources and publish the results
    latency.close();
    throughput.close();
    Thread.sleep(3000);
  }
}
