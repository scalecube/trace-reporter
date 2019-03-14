package io.scalecube.trace.jsonbin;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import org.rapidoid.concurrent.Callback;
import org.rapidoid.http.HTTP;
import org.rapidoid.http.HttpClient;
import org.rapidoid.http.HttpReq;
import org.rapidoid.http.HttpResp;

public class JsonbinClient {
  private static final String APPLICATION_JSON = "application/json";
  private final HttpClient client;
  private final ObjectMapper mapper;
  

  public JsonbinClient(ObjectMapper mapper) {
    this.mapper = mapper;
    this.client = HTTP.client().reuseConnections(true).keepAlive(true);
  }

  public <R> CompletableFuture<R> get(JsonbinRequest request) throws IOException {
    CompletableFuture<R> future = new CompletableFuture<>();

    HttpReq req = headers(request);

    client.executeRequest(
        req.get(request.url()),
        new Callback<HttpResp>() {

          @Override
          public void onDone(HttpResp result, Throwable error) throws Exception {
            future.complete((R) mapper.readValue(result.bodyBytes(), request.responseType()));
          }
        });

    return future;
  }

  public <R> CompletableFuture<R> post(JsonbinRequest request) throws IOException {
    CompletableFuture<R> future = new CompletableFuture<>();

    HttpReq req = headers(request);

    client.executeRequest(
        req.post(request.url()).contentType(APPLICATION_JSON).body(mapper.writeValueAsBytes(request.body())),
        new Callback<HttpResp>() {

          @Override
          public void onDone(HttpResp result, Throwable error) throws Exception {
            future.complete((R) mapper.readValue(result.bodyBytes(), request.responseType()));
          }
        });

    return future;
  }

  private HttpReq headers(JsonbinRequest request) {
    HttpReq http = new HttpReq(client);
    if (request.secret() != null) http = http.header("secret-key", request.secret());

    if (request.collectionId() != null) http = http.header("collection-id", request.collectionId());

    return http;
  }
}
