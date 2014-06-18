package org.exoplatform.management.service.handler.ecmadmin;

import org.exoplatform.management.service.api.AbstractResourceHandler;
import org.exoplatform.management.service.api.StagingService;

public class SearchTemplatesHandler extends AbstractResourceHandler {
  @Override
  public String getPath() {
    return StagingService.ECM_TEMPLATES_APPLICATION_SEARCH_PATH;
  }

}
