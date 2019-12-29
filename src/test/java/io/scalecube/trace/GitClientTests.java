package io.scalecube.trace;

import io.scalecube.trace.git.GitClient;
import java.io.IOException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.TransportException;
import org.junit.jupiter.api.Test;
import org.opentest4j.TestAbortedException;

class GitClientTests {

  @Test
  void testFlow() throws InvalidRemoteException, TransportException, IOException, GitAPIException {
    String slug = System.getProperty("slug", "");
    if (slug.trim().isEmpty()) {
      throw new TestAbortedException("missing slug configuration in the test");
    }
    GitClient git = GitClient.cloneRepo("git@github.com:" + slug + ".git");
    try (TraceReporter reporter = new TraceReporter()) {
      reporter.addY("ABC","latency", 72);
      reporter.addY("ABC","latency", 63);
      reporter.addY("ABC","latency", 45);
      reporter.addY("ABC","latency", 52);
      reporter.addY("XYZ","latency", 161);
      reporter.addY("XYZ","latency", 151);
      reporter.addY("XYZ","latency", 143);

      reporter.addY("ABC","throughput", 52);
      reporter.addY("ABC","throughput", 53);
      reporter.addY("ABC","throughput", 35);
      reporter.addY("ABC","throughput", 62);
      reporter.addY("XYZ","throughput", 361);
      reporter.addY("XYZ","throughput", 151);
      reporter.addY("XYZ","throughput", 343);

      reporter.createChart(git, "template.json", "test-with-data").block();
    }
  }
}
