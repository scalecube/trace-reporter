package io.scalecube.trace;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.LongAdder;
import reactor.core.Disposable.Composite;
import reactor.core.Disposables;

public class TraceReporter implements AutoCloseable {

  private final ConcurrentMap<String, TraceData<Object, Object>> traces = new ConcurrentHashMap<>();
  private final ConcurrentMap<String, LongAdder> xadder = new ConcurrentHashMap<>();
  private final ConcurrentMap<String, LongAdder> yadder = new ConcurrentHashMap<>();
  private final boolean isActive = EnviromentVariables.isActive();

  private final Composite disposables = Disposables.composite();

  /** to set to active create environment variable named TRACE_REPORT=true. */
  public boolean isActive() {
    return isActive;
  }

  /** create reporter. */
  public TraceReporter() {}

  /**
   * get or create a trace line with a given name.
   *
   * @param name of the trace.
   * @return TraceData with a given name.
   */
  public <X, Y> TraceData<X, Y> trace(String name, String group) {
    return (TraceData<X, Y>) traces.computeIfAbsent((name), c -> new TraceData<>(c, group));
  }

  /**
   * add sample value on X axis y axis s auto incremented by 1.
   *
   * @param name of trace
   * @param value to add.
   */
  public <X> void addY(String name, String group, X value) {
    xadder(name).increment();
    trace(name, group).xaxis().add(xadder(name).longValue());
    trace(name, group).yaxis().add(value);
  }

  /**
   * add sample value on Y axis y axis s auto incremented by 1.
   *
   * @param name of trace
   * @param value to add.
   */
  public <Y> void addX(String name, String group, Y value) {
    yadder(name).increment();
    trace(name, group).yaxis().add(yadder(name).longValue());
    trace(name, group).xaxis().add(value);
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

  @Override
  public void close() {
    disposables.dispose();
  }

  public Collection<TraceData<Object, Object>> traces() {
    return this.traces.values();
  }
}
