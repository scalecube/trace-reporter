package io.scalecube.trace.console;

import com.beust.jcommander.JCommander;
import io.scalecube.trace.TraceReporter;
import java.time.Duration;

class ReportBuilder {

  /**
   * example -t ./target/charts/ -i ./target/traces/ -o ./target/output.
   *
   * @param argv command line argument
   */
  public static void main(String... argv) {
    Args args = new Args();
    JCommander cmd = JCommander.newBuilder().addObject(args).build();
    cmd.parse(argv);
    run(args);
  }

  private static void run(Args args) {
    TraceReporter r = new TraceReporter();

    if (args.input == null) {
      args.input = r.tracesLocation();
    }

    if (args.output == null) {
      args.output = r.chartsLocation();
    }

    if (args.template == null) {
      args.template = r.teplateLocation();
    }

    try {
      System.out.printf(
          "--input=%s  --output=%s  --template=%s \n", args.input, args.output, args.template);

      r.createChart(args.input, args.output, args.template).block();
    } catch (Exception e) {
      System.out.println("failed with reason:" + e.getMessage());
    }
  }
}
