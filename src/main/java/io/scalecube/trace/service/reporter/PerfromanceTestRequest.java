package io.scalecube.trace.service.reporter;

import io.scalecube.trace.TraceData;

public class PerfromanceTestRequest {
  private final String cid;
  private final TraceData[] traces;

  public PerfromanceTestRequest(String cid, TraceData[] traces) {
    this.cid = cid;
    this.traces = traces;
  }
}
