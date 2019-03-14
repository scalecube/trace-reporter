package io.scalecube.trace.jsonbin;

public class JsonbinRequest<T> {

  private String secret;

  private String collectionId;

  private String url;

  private Object body;

  private Class<?> responseType;

  private JsonbinRequest(Builder builder) {
    this.secret = builder.secret;
    this.collectionId = builder.collectionId;
    this.body = builder.body;
    this.responseType = builder.responseType;
    this.url = builder.url;
  }
  
  public String secret() {
    return secret;
  }

  public String collectionId() {
    return collectionId;
  }

  public String url() {
    return url;
  }

  public Object body() {
    return body;
  }

  public Class<?> responseType() {
    return responseType;
  }

  public static class Builder {

    public Object body;
    public String secret;
    private String url;
    private Class<?> responseType;
    private String collectionId;

    public Builder url(String url) {
      this.url = url;
      return this;
    }

    public Builder responseType(Class<?> responseType) {
      this.responseType = responseType;
      return this;
    }

    public JsonbinRequest<?> build() {
      return new JsonbinRequest<>(this);
    }

    public Builder body(Object body) {
      this.body = body;
      return this;
    }

    public Builder secret(String secret) {
      this.secret = secret;
      return this;
    }

    public Builder collection(String collectionId) {
      this.collectionId = collectionId;
      return this;
    }
  }

  public static Builder builder() {
    return new Builder();
  }
}
