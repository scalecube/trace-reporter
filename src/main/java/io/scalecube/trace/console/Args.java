package io.scalecube.trace.console;

import com.beust.jcommander.Parameter;

public class Args {
  @Parameter(
      names = {"--input", "-i"},
      description = "input folder")
  String input;

  @Parameter(
      names = {"--output", "-o"},
      description = "output folder")
  String output;

  @Parameter(
      names = {"--template", "-t"},
      description = "url or file name")
  String template;
}
