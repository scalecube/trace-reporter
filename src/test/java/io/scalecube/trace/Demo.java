package io.scalecube.trace;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class Demo {

  private static String folder = "./target/traces/";
  
  public static void main(String[] args) throws IOException {

    new File(folder).mkdir();
    
    Reporter r = new Reporter();

    TraceData trace = new TraceData("trace-1");
    
    trace.xaxis().add(1d);
    trace.xaxis().add(2d);
    trace.xaxis().add(3d);

    trace.yaxis().add(31d);
    trace.yaxis().add(13d);
    trace.yaxis().add(12d);

    OutputStream out = new FileOutputStream(folder + "out-1.json");
    r.dump(out, trace);
    out.close();

    trace.xaxis().add(1d);
    trace.xaxis().add(2d);
    trace.xaxis().add(3d);

    trace.yaxis().add(31d);
    trace.yaxis().add(13d);
    trace.yaxis().add(12d);
    
    r.dumpToFile(folder + "out-2.json",trace);
    
    r.dumpToFile(folder , "out-2.json", trace);
    
  }
}
