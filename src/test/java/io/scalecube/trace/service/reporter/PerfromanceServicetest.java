package io.scalecube.trace.service.reporter;

import io.scalecube.trace.TraceReporter;
import java.io.IOException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.TransportException;
import org.junit.jupiter.api.Test;

class PerfromanceServicetest {

  @Test
  void testFlow() throws InvalidRemoteException, TransportException, IOException, GitAPIException {
    try (TraceReporter reporter = new TraceReporter()) {
      reporter.addY("ABC","A", 72);
      reporter.addY("ABC","A", 63);
      reporter.addY("ABC","A", 45);
      reporter.addY("ABC","A", 52);
      reporter.addY("XYZ","A", 161);
      reporter.addY("XYZ","A", 151);
      reporter.addY("XYZ","A", 143);

      PerfromanceReporter.publish("http://localhost:7778/traces/add","1", reporter.traces());
      
      PerfromanceReporter.publish(reporter);
    }
  }
}
