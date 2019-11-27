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
import io.scalecube.trace.git.GitClient;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.RefNotAdvertisedException;
import reactor.core.Disposable.Composite;
import reactor.core.Disposables;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class TraceReporter implements AutoCloseable {

  private static final PrettyPrinter PRINTER = new DefaultPrettyPrinter();

  private static String DEFAULT_TRACES_FOLDER = "./target/traces/";
  private static String DEFAULT_CHARTS_FOLDER = "./target/charts/";
  private static String template = "./src/main/resources/chart_template.json";

  private final ConcurrentMap<String, TraceData<Object, Object>> traces = new ConcurrentHashMap<>();
  private final ConcurrentMap<String, LongAdder> xadder = new ConcurrentHashMap<>();
  private final ConcurrentMap<String, LongAdder> yadder = new ConcurrentHashMap<>();
  private final boolean isActive;

  private final Composite disposables = Disposables.composite();

  private final ObjectMapper mapper;

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

    mapper = initMapper();
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
   * create a chart report and upload to git.
   *
   * @param gitClient the git client.
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
                for (int i = 0; i < tracesJson.size(); i++) {
                  String name = tracesJson.get(i).get("name").asText("");
                  if (traces.containsKey(name)) {
                    tracesJson.remove(i--);
                  }
                }
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
        .doOnError(
            th -> {
              gitClient.fetchFromOriginToBranch().hardReset().pull();
            })
        .delaySubscription(Duration.ofSeconds(1))
        .retry(1000, t -> "non-fast-forward".equals(t.getMessage()))
        .then();
  }

  @Override
  public void close() {
    disposables.dispose();
  }


  private static String getenvOrDefault(String name, String orDefault) {
    if (System.getenv(name) != null) {
      return System.getenv(name);
    } else {
      return orDefault;
    }
  }
}
