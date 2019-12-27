package io.scalecube.trace.service.reporter;

import io.scalecube.trace.TraceData;

public class PerfromanceTestRequest {
  private final String sha;
  private final TraceData[] traces;
  private String owner;
  private String repo;

  public PerfromanceTestRequest(String owner, String repo, String sha, TraceData[] traces) {
    this.owner = owner;
    this.repo = repo;
    this.sha = sha;
    this.traces = traces;
  }
}
