package io.scalecube.trace.service.reporter;

import io.scalecube.trace.service.reporter.latency.LatencyReporter;
import io.scalecube.trace.service.reporter.latency.PerfServiceLatencyListener;
import io.scalecube.trace.service.reporter.throughput.PerfServiceThroughputListener;
import io.scalecube.trace.service.reporter.throughput.ThroughputReporter;
import java.time.Duration;
import org.junit.jupiter.api.Test;

public class PerfromanceTestExample {

  @Test
  void testPerformance() throws InterruptedException {

    // Measure latency
    LatencyReporter latency =
        LatencyReporter.create(new PerfServiceLatencyListener(50d, 99d))
            .warmupIterations(3)
            .warmupTime(Duration.ofMillis(3))
            .owner("scalecube")
            .repo("github-gateway")
            .start();

    // simulating a test that reports the diff measurement.
    for (int i = 0; i < 3000; i++) {
      latency.onDiff(new Double((Math.random() * 100)).longValue());
      Thread.sleep(10);
    }

    // Measure throughput
    ThroughputReporter throughput =
        ThroughputReporter.create(new PerfServiceThroughputListener())
            .warmupIterations(3)
            .warmupTime(Duration.ofMillis(3))
            .owner("scalecube")
            .repo("github-gateway")
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
