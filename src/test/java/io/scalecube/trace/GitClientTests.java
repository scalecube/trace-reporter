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
      reporter.addY("ABC", 72);
      reporter.addY("ABC", 63);
      reporter.addY("ABC", 45);
      reporter.addY("ABC", 52);
      reporter.addY("XYZ", 161);
      reporter.addY("XYZ", 151);
      reporter.addY("XYZ", 143);

      //      reporter.addY("DEF", 73);
      //      reporter.addY("DEF", 64);
      //      reporter.addY("DEF", 46);
      //      reporter.addY("DEF", 53);
      //      reporter.addY("ZXY", 162);
      //      reporter.addY("ZXY", 152);
      //      reporter.addY("ZXY", 144);
      reporter.createChart(git, "template.json", "test-with-data").block();
    }
  }
}
