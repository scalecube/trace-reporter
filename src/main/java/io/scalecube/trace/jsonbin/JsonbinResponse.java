package io.scalecube.trace.jsonbin;

public class JsonbinResponse<T> {

  private String id;
  private boolean success;
  private String message;
  private T data;

  public String id() {
    return id;
  }

  public boolean success() {
    return success;
  }

  public T data() {
    return data;
  }
  
  @Override
  public String toString() {
    return "JsonbinResponse [id=" + id + ", success=" + success + ", data=" + data + "]";
  }
}
