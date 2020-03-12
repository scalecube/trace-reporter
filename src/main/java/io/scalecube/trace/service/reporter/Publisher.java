package io.scalecube.trace.service.reporter;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.scalecube.trace.TraceData;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collection;

public final class Publisher {

  private static final ObjectMapper mapper = initMapper();

  private Publisher() {
    // Do not instantiate
  }

  private static ObjectMapper initMapper() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    mapper.configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL, true);
    mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    mapper.configure(SerializationFeature.WRITE_ENUMS_USING_TO_STRING, true);
    mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
    mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    return mapper;
  }

  /**
   * publish test results to the given url.
   *
   * @param url traget service colleciting these results.
   * @param sha the context or commit id of this publish request.
   * @param collection the traces result of the test run.
   * @throws IOException on error.
   */
  public static void publish(
      String url,
      String owner,
      String repo,
      String sha,
      Collection<TraceData<Object, Object>> collection)
      throws IOException {
    String uri = String.format("%s/%s/%s/%s", url, owner, repo, sha);
    //noinspection rawtypes
    TraceData[] traces = collection.toArray(new TraceData[0]);
    PerfromanceTestRequest req = new PerfromanceTestRequest(owner, repo, sha, traces);
    postResults(uri, mapper.writeValueAsString(req));
  }

  private static void postResults(String url, String jsonData) throws IOException {
    URL obj = new URL(url);
    HttpURLConnection con = (HttpURLConnection) obj.openConnection();

    // Setting basic post request
    con.setRequestMethod("POST");
    con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
    con.setRequestProperty("Content-Type", "application/json");

    // Send post request
    con.setDoOutput(true);
    DataOutputStream wr = new DataOutputStream(con.getOutputStream());
    wr.writeBytes(jsonData);
    wr.flush();
    wr.close();

    int responseCode = con.getResponseCode();
    System.out.println("nSending 'POST' request to URL : " + url);
    System.out.println("Post Data : " + jsonData);
    System.out.println("Response Code : " + responseCode);

    BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
    String output;
    StringBuilder response = new StringBuilder();

    while ((output = in.readLine()) != null) {
      response.append(output);
    }
    in.close();

    // printing result from response
    System.out.println(response.toString());
  }
}
