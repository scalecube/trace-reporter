package io.scalecube.trace.jsonbin;

import io.scalecube.trace.TraceData;

public class JsonbinResponse {

  private String id;
  private boolean success;
  private String message;
  private TraceData data;

  public String id() {
    return id;
  }

  public boolean success() {
    return success;
  }

  public TraceData data() {
    return data;
  }
  
  @Override
  public String toString() {
    return "JsonbinResponse [id=" + id + ", success=" + success + ", data=" + data + "]";
  }
}
