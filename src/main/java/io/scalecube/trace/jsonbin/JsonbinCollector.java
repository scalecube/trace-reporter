package io.scalecube.trace.jsonbin;

import io.scalecube.trace.TraceReporter;
import java.time.Duration;

public class JsonbinCollector {

  /**
   * upload chart bin to jsonbin service.
   *
   * @param args noop
   * @throws Exception in case of error.
   */
  public static void main(String[] args) throws Exception {

    TraceReporter r = new TraceReporter();

    r.createChart(r.tracesEnv(), r.chartsEnv(), r.teplateFileEnv()).block(Duration.ofSeconds(30));
  }
}
