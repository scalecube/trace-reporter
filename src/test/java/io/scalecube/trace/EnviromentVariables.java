package io.scalecube.trace;

import io.github.cdimascio.dotenv.Dotenv;

public class EnviromentVariables {

  private static final Dotenv dotenv =
      Dotenv.configure()
          .directory("./src/main/resources/")
          .ignoreIfMalformed()
          .ignoreIfMissing()
          .load();

  public static String env(String name) {
    return dotenv.get(name);
  }

  public static String env(String name, String defaultValue) {
    return dotenv.get(name, defaultValue);
  }

  public static boolean isActive() {
    return EnviromentVariables.env("TRACE_REPORT") != null
        && EnviromentVariables.env("TRACE_REPORT").equals("true");
  }

  public static String owner(String defValue) {
    return env("OWNER", defValue);
  }

  public static String repo(String defValue) {
    return env("REPO", defValue);
  }

  public static String sha(String defValue) {
    return env("COMMIT_ID", defValue);
  }

  public static String url(String defValue) {
    return env("TRACE_REPORT_URL", defValue);
  }

  public static String testName(String defValue) {
    return env("TEST_NAME", defValue);
  }

  public static double scalingRatio(double d) { // TODO Auto-generated method stub
    return Double.valueOf(env("SCALE_RATIO", String.valueOf(d)));
  }
}
