package io.scalecube.trace.service.reporter;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.scalecube.trace.TraceData;
import io.scalecube.trace.TraceReporter;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collection;

public class PerfromanceReporter {

  private static final ObjectMapper mapper = initMapper();

  public static String OWNER = getenvOrDefault("OWNER", "scalecube");
  public static String REPO = getenvOrDefault("REPO", "github-gateway");
  public static String CID = getenvOrDefault("COMMIT_ID", "1");
  public static String TRACE_REPORT_URL = getenvOrDefault("TRACE_REPORT_URL", "https://scalecube-7778.exchange.om2.com/traces/add");
  
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

    TraceData[] traces = collection.toArray(new TraceData[collection.size()]);
    PerfromanceTestRequest req = new PerfromanceTestRequest(owner, repo, sha, traces);
    postResults(url, mapper.writeValueAsString(req));
  }

  /**
   * publish the current state of reporter to the trace reporter service.
   *
   * @param reporter containing a state.
   * @throws IOException error if there is exception.
   */
  public static void publish(TraceReporter reporter) throws IOException {
    publish(TRACE_REPORT_URL,OWNER,REPO, CID, reporter.traces());
  }

  /**
   * post http request to endpoint url.
   *
   * @param url target where to send the http request.
   * @param jsonData json object to be sent.
   * @throws IOException if error has happened.
   */
  public static void postResults(String url, String jsonData) throws IOException {

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
    StringBuffer response = new StringBuffer();

    while ((output = in.readLine()) != null) {
      response.append(output);
    }
    in.close();

    // printing result from response
    System.out.println(response.toString());
  }

  private static String getenvOrDefault(String name, String orDefault) {
    if (System.getenv(name) != null) {
      return System.getenv(name);
    } else {
      return orDefault;
    }
  }
}
