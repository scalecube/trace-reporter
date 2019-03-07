package io.scalecube.trace;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import org.HdrHistogram.Histogram;

public class Reporter {

  static ObjectMapper mapper;
  static JsonGenerator generator;
  double outputValueUnitScalingRatio = 1000;

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
   * create reporter.
   */
  public Reporter() {
    try {
      mapper = initMapper();
    } catch (Exception ex) {
      System.err.println(ex);
    }
  }

  public void dump(OutputStream output, TraceData trace) throws IOException {
    generator = mapper.getFactory().createGenerator(output);
    generator.writeObject(trace);
  }

  public double mean(Histogram histogram) {
    return histogram.getMean() / outputValueUnitScalingRatio;
  }

  public void dumpToFile(String fullName, TraceData trace) throws IOException {
    OutputStream out = new FileOutputStream(fullName);
    dump(out, trace);
    out.close();
    
  }
}
