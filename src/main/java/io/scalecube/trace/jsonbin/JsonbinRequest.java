package io.scalecube.trace.jsonbin;

public class JsonbinRequest<T> {

  private String secret;

  private String collectionId;

  private String url;

  private Object body;

  private Class<T> responseType;

  private boolean versioning;

  private boolean isPrivate;

  private JsonbinRequest(Builder<T> builder) {
    this.secret = builder.secret;
    this.collectionId = builder.collectionId;
    this.body = builder.body;
    this.responseType = builder.responseType;
    this.url = builder.url;
    this.versioning = builder.versioning;
    this.isPrivate = builder.isPrivate;
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

  public boolean versioning() {
    return versioning;
  }

  public boolean isPrivate() {
    return isPrivate;
  }

  public static class Builder<T> {

    public boolean isPrivate = false;
    public boolean versioning = true;
    public Object body;
    public String secret;
    private String url;
    private Class<T> responseType;
    private String collectionId;

    public Builder<T> url(String url) {
      this.url = url;
      return this;
    }

    public Builder<T> responseType(Class<T> responseType) {
      this.responseType = responseType;
      return this;
    }

    public JsonbinRequest<T> build() {
      return new JsonbinRequest<T>(this);
    }

    public Builder<T> versioning(boolean versioning) {
      this.versioning = versioning;
      return this;
    }

    public Builder<T> isPrivate(boolean isPrivate) {
      this.isPrivate = isPrivate;
      return this;
    }

    public Builder<T> body(T body) {
      this.body = body;
      return this;
    }

    public Builder<T> secret(String secret) {
      this.secret = secret;
      return this;
    }

    public Builder<T> collection(String collectionId) {
      this.collectionId = collectionId;
      return this;
    }
  }

  public static Builder builder() {
    return new Builder();
  }
}
