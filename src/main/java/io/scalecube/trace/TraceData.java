package io.scalecube.trace;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;

public class TraceData {

  String name;
  LineColor line;
  
  @JsonProperty("x")
  List<Double> xaxis = new ArrayList<>();
  
  @JsonProperty("y")
  List<Double> yaxis = new ArrayList<>();
  String type = "scatter";

  public TraceData(String name) {
    this.name = name;
  }

  public TraceData(String name, String color) {
    this.name = name;
    this.line = new LineColor(color);
  }

  public List<Double> yaxis() {
    return yaxis;
  }

  public List<Double> xaxis() {
    return xaxis;
  }
}
