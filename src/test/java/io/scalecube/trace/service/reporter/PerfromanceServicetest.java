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
    	 reporter.addY("latency-1","latency", 72);
         reporter.addY("latency-1","latency", 63);
         reporter.addY("latency-1","latency", 45);
         
         reporter.addY("latency-2","latency", 52);
         reporter.addY("latency-2","latency", 161);
         reporter.addY("latency-2","latency", 151);
         reporter.addY("latency-2","latency", 143);

         reporter.addY("throughput-1","throughput", 52);
         reporter.addY("throughput-1","throughput", 53);
         reporter.addY("throughput-1","throughput", 35);
         reporter.addY("throughput-1","throughput", 62);
         
         reporter.addY("throughput-2","throughput", 361);
         reporter.addY("throughput-2","throughput", 151);
         reporter.addY("throughput-2","throughput", 343);
         
      PerfromanceReporter.publish(reporter);
    }
  }
}
