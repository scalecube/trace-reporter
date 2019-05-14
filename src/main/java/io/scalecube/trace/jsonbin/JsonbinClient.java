package io.scalecube.trace.jsonbin;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.rapidoid.concurrent.Callback;
import org.rapidoid.http.HTTP;
import org.rapidoid.http.HttpClient;
import org.rapidoid.http.HttpReq;
import org.rapidoid.http.HttpResp;
import reactor.core.publisher.Mono;

public class JsonbinClient implements AutoCloseable {

  private static final String APPLICATION_JSON = "application/json";
  private final HttpClient client;
  private final ObjectMapper mapper;

  public JsonbinClient(ObjectMapper mapper) {
    this.mapper = mapper;
    this.client = HTTP.client().reuseConnections(true).keepAlive(true);
  }

  /**
   * make a read request to jsonbin service.
   *
   * @param request paramters.
   * @return JsonbinResponse containing content.
   */
  public <R> Mono<R> get(JsonbinRequest<R> request) {
    return Mono.create(
        sink -> {
          client.executeRequest(
              options(request).get(request.url()).contentType(APPLICATION_JSON),
              new Callback<HttpResp>() {
                @Override
                public void onDone(HttpResp result, Throwable error) throws Exception {
                  if (error == null) {
                    Object resp = mapper.readValue(result.bodyBytes(), request.responseType());

                    sink.success((R) resp);
                  } else {
                    sink.error(error);
                  }
                }
              });
        });
  }

  /**
   * update a json bin document.
   *
   * @param request with params.
   * @return JsonbinResponse object
   */
  public <R> Mono<JsonbinResponse> put(JsonbinRequest<R> request) {
    return Mono.create(
        sink -> {
          try {
            client.executeRequest(
                options(request)
                    .put(request.url())
                    .contentType(APPLICATION_JSON)
                    .body(mapper.writeValueAsBytes(request.body())),
                new Callback<HttpResp>() {
                  @Override
                  public void onDone(HttpResp result, Throwable error) throws Exception {
                    JsonbinResponse response =
                        mapper.readValue(result.bodyBytes(), JsonbinResponse.class);
                    if (response.success()) {
                      sink.success(response);
                    } else {
                      sink.error(new IllegalStateException(response.message()));
                    }
                  }
                });
          } catch (JsonProcessingException e) {
            sink.error(e);
          }
        });
  }

  /**
   * create a new json bin item.
   *
   * @param request with paramters
   * @return jsonbin response.
   */
  public <R> Mono<JsonbinResponse> post(JsonbinRequest<R> request) {
    return Mono.create(
        sink -> {
          try {
            client.executeRequest(
                options(request)
                    .post(request.url())
                    .contentType(APPLICATION_JSON)
                    .body(mapper.writeValueAsBytes(request.body())),
                new Callback<HttpResp>() {
                  @Override
                  public void onDone(HttpResp result, Throwable error) throws Exception {
                    if (error == null) {
                      JsonbinResponse response =
                          mapper.readValue(result.bodyBytes(), JsonbinResponse.class);
                      if (response.success()) {
                        sink.success(response);
                      } else {
                        sink.error(new IllegalStateException(response.message()));
                      }
                    } else {
                      sink.error(error);
                    }
                  }
                });
          } catch (JsonProcessingException e) {
            sink.error(e);
          }
        });
  }

  @Override
  public void close() {
    client.close();
  }

  private HttpReq options(JsonbinRequest request) {
    HttpReq http = new HttpReq(client);
    if (request.secret() != null) {
      http = http.header("secret-key", request.secret());
    }

    if (request.collectionId() != null) {
      http = http.header("collection-id", request.collectionId());
    }

    if (!request.versioning()) {
      http = http.header("versioning", "false");
    }

    if (request.isPrivate()) {
      http = http.header("private", "true");
    }

    return http;
  }
}
