package io.scalecube.trace;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;

public class TraceData<X,Y> {

  String name;
  String group;
  String hovertemplate = "<b>%{y}</b>";
  LineColor line;
  
  @JsonProperty("x")
  List<X> xaxis = new ArrayList<>();
  
  @JsonProperty("y")
  List<Y> yaxis = new ArrayList<>();
  String type = "scatter";

  public TraceData() {  
  }
  
  public TraceData(String name,String group) {
    this.name = name;
    this.group = group;
  }

  /**
   * tracing data information.
   * 
   * @param name the name of the trace.
   * @param group the group this trace take part of.
   * @param color of the line.
   */
  public TraceData(String name,String group, String color) {
    this.name = name;
    this.group = group;
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
