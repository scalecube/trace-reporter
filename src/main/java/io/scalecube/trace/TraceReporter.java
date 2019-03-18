package io.scalecube.trace;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.scalecube.trace.jsonbin.JsonbinClient;
import io.scalecube.trace.jsonbin.JsonbinRequest;
import io.scalecube.trace.jsonbin.JsonbinRequest.Builder;
import io.scalecube.trace.jsonbin.JsonbinResponse;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class TraceReporter {

  static ObjectMapper mapper;
  static JsonGenerator generator;
  private static final String URL_API_JSONBIN_IO = "https://api.jsonbin.io/b/";

  private static String DEFAULT_TRACES_FOLDER = "./target/traces/";
  private static String DEFAULT_CHARTS_FOLDER = "./target/charts/";
  private static String template = "./src/main/resources/chart_template.json";

  private final ScheduledExecutorService scheduler;
  private final ConcurrentMap<String, TraceData<Object, Object>> traces = new ConcurrentHashMap<>();
  private final ConcurrentMap<String, LongAdder> xadder = new ConcurrentHashMap<>();
  private final ConcurrentMap<String, LongAdder> yadder = new ConcurrentHashMap<>();
  private final boolean isActive;

  private JsonbinClient client;

  private static ObjectMapper initMapper() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    mapper.configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL, true);
    mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    mapper.configure(SerializationFeature.WRITE_ENUMS_USING_TO_STRING, true);
    mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
    mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    return mapper;
  }

  /** to set to active create environment variable named TRACE_REPORT=true. */
  public boolean isActive() {
    return isActive;
  }

  public static String tracesLocation() {
    return getenvOrDefault("TRACES_FOLDER", DEFAULT_TRACES_FOLDER);
  }

  public static String chartsLocation() {
    return getenvOrDefault("CHARTS_FOLDER", DEFAULT_CHARTS_FOLDER);
  }

  public static String teplateLocation() {
    return getenvOrDefault("CHART_TEMPLATE", template);
  }

  /** create reporter. */
  public TraceReporter() {
    isActive =
        System.getenv("TRACE_REPORT") != null && System.getenv("TRACE_REPORT").equals("true");
    scheduler = Executors.newScheduledThreadPool(1);
    try {
      mapper = initMapper();
      client = new JsonbinClient(mapper);
    } catch (Exception ex) {
      System.err.println(ex);
    }
  }

  /**
   * get or create a trace line with a given name.
   *
   * @param name of the trace.
   * @return TraceData with a given name.
   */
  public <X, Y> TraceData<X, Y> trace(String name) {
    return (TraceData<X, Y>) traces.computeIfAbsent(name, c -> new TraceData<>(c));
  }

  /**
   * add sample value on X axis y axis s auto incremented by 1.
   *
   * @param name of trace
   * @param value to add.
   */
  public <X> void addY(String name, X value) {
    xadder(name).increment();
    trace(name).xaxis().add(xadder(name).longValue());
    trace(name).yaxis().add(value);
  }

  /**
   * add sample value on Y axis y axis s auto incremented by 1.
   *
   * @param name of trace
   * @param value to add.
   */
  public <Y> void addX(String name, Y value) {
    yadder(name).increment();
    trace(name).yaxis().add(yadder(name).longValue());
    trace(name).xaxis().add(value);
  }

  /**
   * Dump the trace state in to a file. the file is overwritten every time this method is called.
   *
   * @param fullName path and file name of the file.
   * @param json data to store to file.
   * @return CompletableFuture of future result.
   * @throws IOException on file errors.
   */
  public Mono<Void> dumpToFile(String fullName, Object json) {
    try {
      return dumpTo(new FileOutputStream(fullName), json);
    } catch (FileNotFoundException e) {
      return Mono.error(e);
    }
  }

  /**
   * Dump the trace state in to a file. the file is overwritten every time this method is called.
   *
   * @param folder path and file name of the file.
   * @param file path and file name of the file.
   * @param json data to store to file.
   * @return CompletableFuture of future result.
   * @throws IOException on file errors.
   */
  public Mono<Void> dumpToFile(String folder, String file, Object json) {
    File dir = new File(folder);
    if (!dir.exists()) {
      dir.mkdirs();
    }
    return dumpToFile(folder + file, json);
  }

  /**
   * dump trace data to output stream.
   *
   * @param output stream to dump to.
   * @param json data to dump.
   * @return Mono when done.
   */
  public Mono<Void> dumpTo(OutputStream output, Object json) {
    return Mono.create(
        sink -> {
          try {
            generator = mapper.getFactory().createGenerator(output);
            generator.writeObject(json);
            output.close();
            sink.success();
          } catch (IOException e) {
            sink.error(e);
          }
        });
  }

  /**
   * Dump traces state in to a file. the file is overwritten every time this method is called. a
   * file in folder will be created for each trace in the given name of the trace when created.
   *
   * @param folder path and file name of the file .
   * @return Mono of future result.
   */
  public Mono<Void> dumpTo(String folder) {
    return Flux.fromStream(traces.entrySet().stream())
        .flatMap(m -> dumpToFile(folder, m.getKey(), m.getValue()))
        .then();
  }

  public Flux<JsonbinResponse> sendToJsonbin() {
    return sendToJsonbin(null, null);
  }

  public Mono<JsonbinResponse> sendToJsonbin(Object data) {
    return sendToJsonbin(null, null, data);
  }

  /**
   * send all collected data to json bin service.
   *
   * @param secret optional nullable.
   * @param collectionId optional nullable.
   * @return json bin response.
   */
  public Flux<JsonbinResponse> sendToJsonbin(String secret, String collectionId) {
    Builder b =
        JsonbinRequest.builder()
            .url("https://api.jsonbin.io/b")
            .responseType(JsonbinResponse.class);

    if (secret != null) {
      b.secret(secret);
    }

    if (collectionId != null) {
      b.collection(collectionId);
    }

    return Flux.fromStream(traces.entrySet().stream())
        .flatMap(
            (entry) ->
                client
                    .post(b.body(entry.getValue()).build())
                    .map(
                        resp -> {
                          ((JsonbinResponse) resp).name(entry.getKey());
                          return resp;
                        }));
  }

  /**
   * send to jsonbin a given data.
   *
   * @param secret optional nullable.
   * @param collectionId optional nullable.
   * @param body mandatory.
   * @return json response bin.
   */
  public Mono<JsonbinResponse> sendToJsonbin(String secret, String collectionId, Object body) {

    Builder b =
        JsonbinRequest.builder()
            .url("https://api.jsonbin.io/b")
            .responseType(JsonbinResponse.class);

    if (secret != null) {
      b.secret(secret);
    }

    if (collectionId != null) {
      b.collection(collectionId);
    }

    return client.post(b.body(body).build());
  }

  /**
   * Schedule periodic dump to folder.
   *
   * @param duration periodic dump duration.
   * @param folder target where .json files are created.
   * @return ScheduledFuture of the scheduler
   */
  public ScheduledFuture<?> scheduleDumpTo(Duration duration, String folder) {

    return scheduler.scheduleAtFixedRate(
        () ->
            sendToJsonbin()
                .subscribe(
                    res -> {
                      if (res.success()) {
                        dumpToFile(folder, res.name(), res).subscribe();
                      }
                    }),
        duration.toMillis(),
        duration.toMillis(),
        TimeUnit.MILLISECONDS);
  }

  /**
   * get or create x axis long adder.
   *
   * @param name of the trace.
   * @return LongAdder of the trace.
   */
  private LongAdder xadder(String name) {
    return xadder.computeIfAbsent(name, c -> new LongAdder());
  }

  /**
   * get or create y axis long adder.
   *
   * @param name of the trace.
   * @return LongAdder of the trace.
   */
  private LongAdder yadder(String name) {
    return yadder.computeIfAbsent(name, c -> new LongAdder());
  }

  /**
   * create a chart report and upload to jsonbin.
   *
   * @param tracesFolder where is the data.
   * @param chartsFolder where chart is created.
   * @param chartTemplate location file or url as the basis template of chart.
   * @return mono void when operation completed.
   * @throws Exception in error case.
   */
  public Mono<Void> createChart(String tracesFolder, String chartsFolder, String chartTemplate)
      throws Exception {

    return Mono.create(
        sink -> {
          File inDir = new File(tracesFolder);

          if (inDir.exists() && containsFiles(inDir)) {
            JsonNode root;
            try {
              root = fetchFromFileOrUrl(chartTemplate);
              String[] tracesIds = readTracesIds(inDir);
              setTraces(root, tracesIds);
              sendToJsonbin(root)
                  .subscribe(
                      consumer -> {
                        dumpToFile(chartsFolder, consumer.id(), consumer.data()).subscribe();
                        System.out.println(URL_API_JSONBIN_IO + consumer.id());
                        sink.success();
                      });
            } catch (Exception e) {
              sink.error(e);
            }
          } else {
            System.out.println("folder not found and or files are found in" + tracesFolder);
            sink.error(new Exception("folder not found and or files are found in" + tracesFolder));
          }
        });
  }

  private String[] readTracesIds(File inDir) {
    List<String> result = new ArrayList();
    for (String file : inDir.list()) {
      File src = new File(inDir, file);
      try {
        JsonbinResponse value = mapper.readValue(src, JsonbinResponse.class);
        result.add(value.id());
      } catch (IOException e) {
        System.out.println(file + " file is not json or a Jsonbin result... skipping");
      }
    }
    return result.toArray(new String[result.size()]);
  }

  private JsonNode fetchFromFileOrUrl(String location) throws Exception {
    if (isUrl(location)) {
      JsonbinRequest<JsonNode> req =
          JsonbinRequest.builder().responseType(JsonNode.class).url(location).build();

      return client.get(req).block();
    } else {
      File file = new File(location);
      if (file.exists() && file.isFile()) {
        return mapper.readTree(file);
      } else {
        throw new Exception("provided location: [" + location + "] does not exists or not a file.");
      }
    }
  }

  private boolean isUrl(String location) {
    return location.startsWith("http://") || location.startsWith("https://");
  }

  private boolean containsFiles(File inDir) {
    return inDir.list() != null && inDir.list().length > 0;
  }

  private void setTraces(JsonNode root, String[] listTraces) {
    ArrayNode traces = mapper.createArrayNode();

    for (String file : listTraces) {
      traces.add(URL_API_JSONBIN_IO + file);
    }
    ((ObjectNode) root).put("traces", traces);

    root.path("traces");
  }

  private static String getenvOrDefault(String name, String orDefault) {
    if (System.getenv(name) != null) {
      return System.getenv(name);
    } else {
      return orDefault;
    }
  }
}
