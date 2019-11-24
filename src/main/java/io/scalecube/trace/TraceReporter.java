package io.scalecube.trace;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.PrettyPrinter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.scalecube.trace.git.GitClient;
import io.scalecube.trace.jsonbin.JsonbinClient;
import io.scalecube.trace.jsonbin.JsonbinRequest;
import io.scalecube.trace.jsonbin.JsonbinRequest.Builder;
import io.scalecube.trace.jsonbin.JsonbinResponse;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;
import org.eclipse.jgit.api.errors.CanceledException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.RefNotAdvertisedException;
import reactor.core.Disposable;
import reactor.core.Disposable.Composite;
import reactor.core.Disposables;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

public class TraceReporter implements AutoCloseable {

  private static final String URL_API_JSONBIN_IO = "https://api.jsonbin.io/b";
  private static final PrettyPrinter PRINTER = new DefaultPrettyPrinter();

  private static String DEFAULT_TRACES_FOLDER = "./target/traces/";
  private static String DEFAULT_CHARTS_FOLDER = "./target/charts/";
  private static String template = "./src/main/resources/chart_template.json";

  private final ConcurrentMap<String, TraceData<Object, Object>> traces = new ConcurrentHashMap<>();
  private final ConcurrentMap<String, LongAdder> xadder = new ConcurrentHashMap<>();
  private final ConcurrentMap<String, LongAdder> yadder = new ConcurrentHashMap<>();
  private final boolean isActive;
  private final String jsonBinSecret;

  private final Composite disposables = Disposables.composite();

  private final ObjectMapper mapper;
  private final JsonbinClient client;

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

    jsonBinSecret = System.getenv("JSON_BIN_SECRET");

    mapper = initMapper();
    client = new JsonbinClient(mapper);
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
            JsonGenerator generator = mapper.getFactory().createGenerator(output);
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
    return sendToJsonbin(jsonBinSecret, null);
  }

  /**
   * send all collected data to json bin service.
   *
   * @param data data sent to jsonbin.
   * @return json bin response.
   */
  public Mono<JsonbinResponse> sendToJsonbin(Object data) {
    return sendToJsonbin(jsonBinSecret, null, data);
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
        JsonbinRequest.builder().url(URL_API_JSONBIN_IO).responseType(JsonbinResponse.class);

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
        JsonbinRequest.builder().url(URL_API_JSONBIN_IO).responseType(JsonbinResponse.class);

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
  public Disposable scheduleDumpTo(Duration duration, String folder) {
    Disposable disposable =
        Flux.interval(duration, duration)
            .flatMap(i -> sendToJsonbin())
            .flatMap(res -> dumpToFile(folder, res.name(), res))
            .subscribe(null, Throwable::printStackTrace);
    disposables.add(disposable);
    return disposable;
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
   */
  public Mono<Void> createChart(String tracesFolder, String chartsFolder, String chartTemplate) {

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
                        String titile = getTitle(consumer);
                        System.out.printf(
                            "result:-:%s:-:%s",
                            titile, URL_API_JSONBIN_IO + "/" + consumer.id() + "\n");
                        sink.success();
                      },
                      sink::error);

            } catch (Exception e) {
              sink.error(e);
            }
          } else {
            System.out.println("folder not found and or files are found in" + tracesFolder);
            sink.error(new Exception("folder not found and or files are found in" + tracesFolder));
          }
        });
  }

  /**
   * create a chart report and upload to git.
   *
   * @param git the git client.
   * @param chartTemplate location file or url as the basis template of chart.
   * @return mono void when operation completed.
   */
  public Mono<Void> createChart(GitClient gitClient, String chartTemplate, String branch) {
    return Mono.just(gitClient)
        .map(git -> git.checkout(branch))
        .map(git -> git.pull().hardReset())
        .onErrorResume(
            ex -> ex.getCause() instanceof RefNotAdvertisedException, ex -> Mono.just(gitClient))
        .map(
            git -> {
              try {
                JsonNode root = mapper.reader().readTree(git.getFile(chartTemplate, false));
                ArrayNode tracesJson = ((ArrayNode) root.get("traces"));
                traces.values().forEach(tracesJson::addPOJO);
                mapper.writer(PRINTER).writeValue(git.writeToFile(chartTemplate), root);
                return git;
              } catch (IOException ignoredException) {
                throw new UncheckedIOException(ignoredException);
              }
            })
        .map(
            git -> {
              try {
                return git.add(chartTemplate)
                    .commit(
                        "traces for tests:\n"
                            + traces.keySet().stream()
                                .collect(Collectors.joining("\n+", "\n+", "")));
              } catch (GitAPIException ignoredException) {
                throw new RuntimeException(ignoredException);
              }
            })
        .flatMap(git -> git.push())
        .doOnError(th -> {
          gitClient.fetchFromOriginToBranch().hardReset().pull();
        })
        .delaySubscription(Duration.ofSeconds(1))
        .retry(
            1000,
            t -> "non-fast-forward".equals(t.getMessage()))
        .then();
  }

  private void updateChartOnGitRepo(
      GitClient git, String chartTemplate, String branch, MonoSink<Object> sink) {
    try {
      git.checkout(branch);
      //      try {
      //        git.pull().hardReset();
      //      } catch (RefNotAdvertisedException ignoredException) {
      //        // it's legitimate to have no remote at this point.
      //      } catch (GitAPIException ignoredException) {
      //        sink.error(ignoredException);
      //        return;
      //      }
      InputStream chart;
      chart = git.getFile(chartTemplate, false);
      JsonNode root = mapper.reader().readTree(chart);
      ArrayNode tracesJson = ((ArrayNode) root.get("traces"));
      traces.values().forEach(tracesJson::addPOJO);
      mapper.writer().writeValue(git.writeToFile(chartTemplate), root);
      git.add(chartTemplate)
          .commit(
              "traces for tests:\n\n"
                  + traces.keySet().stream().collect(Collectors.joining("\n", "+", "\n")))
          .push();
      sink.success();
    } catch (GitAPIException | IOException ignoredException) {
      sink.error(ignoredException);
    }
  }

  @Override
  public void close() {
    disposables.dispose();
    client.close();
  }

  private String getTitle(JsonbinResponse consumer) {
    try {
      if (consumer.data() != null
          && consumer.data().getClass().isAssignableFrom(LinkedHashMap.class)) {
        HashMap result = (HashMap) consumer.data();

        String titile = (String) ((HashMap) result.get("layout")).get("title");
        return titile.replaceAll(":-:", "_");
      } else {
        return "no title found.";
      }
    } catch (Throwable t) {
      return "no title found.";
    }
  }

  private String[] readTracesIds(File inDir) {
    List<String> result = new ArrayList<>();
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
      traces.add(URL_API_JSONBIN_IO + "/" + file);
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
