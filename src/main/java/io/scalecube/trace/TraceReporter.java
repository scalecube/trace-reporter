package io.scalecube.trace;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.LongAdder;
import org.HdrHistogram.Histogram;

public class TraceReporter {

  static ObjectMapper mapper;
  static JsonGenerator generator;
  double outputValueUnitScalingRatio = 1000;

  private final ConcurrentMap<String, TraceData<Object, Object>> traces = new ConcurrentHashMap<>();
  private final ConcurrentMap<String, LongAdder> xadder = new ConcurrentHashMap<>();
  private final ConcurrentMap<String, LongAdder> yadder = new ConcurrentHashMap<>();
  
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

  /** create reporter. */
  public TraceReporter() {
    try {
      mapper = initMapper();
    } catch (Exception ex) {
      System.err.println(ex);
    }
  }

  public <X, Y> TraceData<X, Y> trace(String name) {
    return (TraceData<X, Y>) traces.computeIfAbsent(name, c -> new TraceData<>(c));
  }

  public LongAdder xadder(String name) {
    return xadder.computeIfAbsent(name, c -> new LongAdder());
  }
  
  public LongAdder yadder(String name) {
    return yadder.computeIfAbsent(name, c -> new LongAdder());
  }
  
  public <X> void addY(String name, X value) {
    xadder(name).increment();
    trace(name).xaxis().add(xadder(name).longValue());
    trace(name).yaxis().add(value);
  }

  public <Y> void addX(String name, Y value) {
    yadder(name).increment();
    trace(name).yaxis().add(yadder(name).longValue());
    trace(name).xaxis().add(value);
  }
  
  public double mean(Histogram histogram) {
    return histogram.getMean() / outputValueUnitScalingRatio;
  }

  /**
   * Dump the trace state in to a file. the file is overwritten every time this method is called.
   *
   * @param fullName path and file name of the file.
   * @param trace data to store to file.
   * @return
   * @throws IOException on file errors.
   */
  public CompletableFuture<Void> dumpToFile(String fullName, TraceData trace) throws IOException {
    OutputStream out = new FileOutputStream(fullName);
    return dumpTo(out, trace);
  }

  /**
   * Dump the trace state in to a file. the file is overwritten every time this method is called.
   *
   * @param folder path and file name of the file.
   * @param file path and file name of the file.
   * @param trace data to store to file.
   * @return
   * @throws IOException on file errors.
   */
  public CompletableFuture<Void> dumpToFile(String folder, String file, TraceData trace)
      throws IOException {
    new File(folder).mkdir();
    return dumpToFile(folder + file, trace);
  }
  
  public CompletableFuture<Void> dumpTo(OutputStream output, TraceData trace) {
    return CompletableFuture.runAsync(
        () -> {
          try {
            generator = mapper.getFactory().createGenerator(output);
            generator.writeObject(trace);
            output.close();
          } catch (IOException e) {
            throw new IllegalStateException(e);
          }
        });
  }
  
  /**
   * Dump traces state in to a file. the file is overwritten every time this method is called. a
   * file in folder will be created for each trace in the given name of the trace when created.
   *
   * @param folder path and file name of the file .
   * @return
   */
  public CompletableFuture<Void> dumpTo(String folder) {
    CompletableFuture<Void> future = new CompletableFuture<>();
    List<CompletableFuture<Void>> list = new ArrayList<>();
    traces.forEach(
        (key, value) -> {
          try {
            list.add(dumpToFile(folder, key, value));
          } catch (IOException ex) {
            throw new IllegalStateException(ex);
          }
        });
    return future.allOf(list.toArray(new CompletableFuture[list.size()]));
  }
}