package io.scalecube.trace.jsonbin;

public class JsonbinResponse<T> {

  private String id;
  private boolean success;
  private String message;
  private T data;
  private String name;

  public String id() {
    return id;
  }

  public String name() {
    return name;
  }
  
  public void name(String name) {
    this.name = name;
  }
  
  public boolean success() {
    return success;
  }

  public String message() {
    return message;
  }
  
  public T data() {
    return data;
  }
  
  @Override
  public String toString() {
    return "JsonbinResponse [id=" + id + ", success=" + success + ", data=" + data + "]";
  }

  
}
