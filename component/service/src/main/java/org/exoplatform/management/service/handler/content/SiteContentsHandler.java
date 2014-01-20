package org.exoplatform.management.service.handler.content;

import org.exoplatform.management.service.api.AbstractResourceHandler;
import org.exoplatform.management.service.api.StagingService;

public class SiteContentsHandler extends AbstractResourceHandler {

  @Override
  public String getPath() {
    return StagingService.CONTENT_SITES_PATH;
  }
}
