package io.scalecube.trace.jsonbin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.scalecube.trace.TraceReporter;
import java.io.File;

public class JsonbinCollector {

  private static String in_folder = "./target/traces/";
  private static String out_folder = "./target/charts/report.json";
  private static ObjectMapper mapper = new ObjectMapper();

  public static void main(String[] args) throws Exception {
    TraceReporter r = new TraceReporter();
    File in_dir = new File(in_folder);

    JsonNode root = mapper.readTree(new File(out_folder));

    ArrayNode traces = mapper.createArrayNode();
    for (String file : in_dir.list()) {
      traces.add("https://api.jsonbin.io/b/" + file);
    }

    ((ObjectNode) root).put("traces", traces);

    root.path("traces");

    r.sendToJsonbin(root)
        .subscribe(
            consumer -> {
              System.out.println(consumer);
              System.out.println("https://api.jsonbin.io/b/" + consumer.id());
            });
    Thread.currentThread().join();
  }
}
