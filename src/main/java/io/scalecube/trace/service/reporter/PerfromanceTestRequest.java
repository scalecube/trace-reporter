package io.scalecube.trace.service.reporter;

import io.scalecube.trace.TraceData;

public class PerfromanceTestRequest {

  private final String sha;
  private final TraceData[] traces;
  private String owner;
  private String repo;

  /**
   * a DTO request to report a perfromance tests resutls.
   *
   * @param owner the owner repository of this request.
   * @param repo the repository of the request.
   * @param sha the sha commit context of the request.
   * @param traces data containing the report.
   */
  public PerfromanceTestRequest(String owner, String repo, String sha, TraceData[] traces) {
    this.owner = owner;
    this.repo = repo;
    this.sha = sha;
    this.traces = traces;
  }
}
