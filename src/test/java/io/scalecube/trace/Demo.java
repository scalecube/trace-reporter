package io.scalecube.trace;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.ExecutionException;

public class Demo {

  private static String folder = "./target/traces/";
  
  public static void main(String[] args) throws Exception{

    new File(folder).mkdir();

    TraceReporter r = new TraceReporter();
    
    r.trace("reactor-netty");
    r.addY("reactor-netty",11);
    r.addY("reactor-netty",22);
    r.addY("reactor-netty",33);
    
    r.trace("reactor-aeron");
    r.addX("reactor-aeron",10);
    r.addX("reactor-aeron",20);
    r.addX("reactor-aeron",30);
    
    r.dumpTo(folder).get();
  }
}
