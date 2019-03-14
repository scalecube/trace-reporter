package io.scalecube.trace;

import io.scalecube.trace.jsonbin.JsonbinClient;
import io.scalecube.trace.jsonbin.JsonbinRequest;
import io.scalecube.trace.jsonbin.JsonbinResponse;
import java.io.File;
import java.time.Duration;

public class Demo {

  private static String folder = "./target/traces/";
  private static String secret = "$2a$10$TwUCv3gNTtJ.wCQOFr.LAuedqa3y296q8dSODzSP26jPn/I.U4olO";
  private static String collectionId = "5c87f177bb08b22a7568ee7a";

  public static void main(String[] args) throws Exception {

    new File(folder).mkdir();

    TraceReporter r = new TraceReporter();

    r.trace("reactor-netty");
    r.addY("reactor-netty", 11);
    r.addY("reactor-netty", 22);
    r.addY("reactor-netty", 33);

    r.trace("reactor-aeron");
    r.addX("reactor-aeron", 10);
    r.addX("reactor-aeron", 20);
    r.addX("reactor-aeron", 30);

    r.scheduleDumpTo(Duration.ofMinutes(1), folder);

    JsonbinClient p = new JsonbinClient(r.initMapper());

    JsonbinRequest req = JsonbinRequest.builder()
      .secret(secret)
      .collection(collectionId)
      .url("https://api.jsonbin.io/b/5c8a73965ce1aa6d9f9c4bca/latest")
      .responseType(TraceData.class)
      .build();
    
    // get bin
    p.get(req )
        .thenAccept(
            c -> {
              System.out.println(c);
            });

    TraceData<Integer, Integer> td = new TraceData<>("aeron");

    JsonbinRequest req1 = JsonbinRequest.builder()
        .url("https://api.jsonbin.io/b")
        .body(td)
        .responseType(JsonbinResponse.class)
        .build();
      
    
    // create bin.
    p.post(req1)
        .thenAccept(
            c -> {
              System.out.println(c);
            });

    Thread.currentThread().join();
  }
}
