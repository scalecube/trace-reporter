package io.scalecube.trace;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;

public class TraceData<X,Y> {

  String name;
  LineColor line;
  
  @JsonProperty("x")
  List<X> xaxis = new ArrayList<>();
  
  @JsonProperty("y")
  List<Y> yaxis = new ArrayList<>();
  String type = "scatter";

  public TraceData() {  
  }
  
  public TraceData(String name) {
    this.name = name;
  }

  public TraceData(String name, String color) {
    this.name = name;
    this.line = new LineColor(color);
  }

  public List<Y> yaxis() {
    return yaxis;
  }

  public List<X> xaxis() {
    return xaxis;
  }
  
  @Override
  public String toString() {
    return "TraceData [name="
        + name
        + ", line="
        + line
        + ", xaxis="
        + xaxis
        + ", yaxis="
        + yaxis
        + ", type="
        + type
        + "]";
  }
}
