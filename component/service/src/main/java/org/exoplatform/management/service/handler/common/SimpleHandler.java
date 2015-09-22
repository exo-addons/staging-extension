package org.exoplatform.management.service.handler.common;

import org.exoplatform.management.service.api.AbstractResourceHandler;

public class SimpleHandler extends AbstractResourceHandler {

  private String path;

  public SimpleHandler(String path) {
    this.path = path;
  }

  @Override
  public String getPath() {
    return path;
  }

}
