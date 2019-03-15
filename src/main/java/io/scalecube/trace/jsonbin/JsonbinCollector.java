package io.scalecube.trace.jsonbin;

import io.scalecube.trace.TraceReporter;

public class JsonbinCollector {

  private static String DEFAULT_TRACES_FOLDER = "./target/traces/";
  private static String DEFAULT_CHARTS_FOLDER = "./target/charts/";
  private static String template = "./src/main/resources/chart_template.json";

  /**
   * upload chart bin to jsonbin service.
   *
   * @param args noop
   * @throws Exception in case of error.
   */
  public static void main(String[] args) throws Exception {

    String traces = getenvOrDefault("TRACES_FOLDER", DEFAULT_TRACES_FOLDER);
    String charts = getenvOrDefault("CHARTS_FOLDER", DEFAULT_CHARTS_FOLDER);
    String templateFile = getenvOrDefault("CHART_TEMPLATE", template);

    TraceReporter r = new TraceReporter();

    r.createChart(traces, charts, templateFile);

    Thread.currentThread().join();
  }

  private static String getenvOrDefault(String name, String orDefault) {
    if (System.getenv(name) != null) {
      return System.getenv(name);
    } else {
      return orDefault;
    }
  }
}
