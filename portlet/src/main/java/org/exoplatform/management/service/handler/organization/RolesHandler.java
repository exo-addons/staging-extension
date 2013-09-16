package org.exoplatform.management.service.handler.organization;

import org.exoplatform.management.service.api.AbstractResourceHandler;
import org.exoplatform.management.service.api.StagingService;

public class RolesHandler extends AbstractResourceHandler {

  @Override
  public String getPath() {
    return StagingService.ROLE_PATH;
  }
}