package io.scalecube.trace.service.reporter;

import reactor.core.Exceptions;

public final class PublisherContext implements Cloneable {

  private String owner = "scalecube";
  private String repo = "github-gateway";
  private String testName = "TEST_NAME";
  private String commitId = "1";
  private String traceReportUrl = "https://scalecube-robokit-develop.exchange.om2.com/traces";

  public PublisherContext owner(String owner) {
    PublisherContext c = clone();
    c.owner = owner;
    return c;
  }

  public String owner() {
    return owner;
  }

  public PublisherContext repo(String repo) {
    PublisherContext c = clone();
    c.repo = repo;
    return c;
  }

  public String repo() {
    return repo;
  }

  public PublisherContext testName(String testName) {
    PublisherContext c = clone();
    c.testName = testName;
    return c;
  }

  public String testName() {
    return testName;
  }

  public PublisherContext commitId(String commitId) {
    PublisherContext c = clone();
    c.commitId = commitId;
    return c;
  }

  public String commitId() {
    return commitId;
  }

  public PublisherContext traceReportUrl(String traceReportUrl) {
    PublisherContext c = clone();
    c.traceReportUrl = traceReportUrl;
    return c;
  }

  public String traceReportUrl() {
    return traceReportUrl;
  }

  @Override
  public PublisherContext clone() {
    try {
      return (PublisherContext) super.clone();
    } catch (CloneNotSupportedException e) {
      throw Exceptions.propagate(e);
    }
  }
}
