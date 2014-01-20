package org.exoplatform.management.service.handler.registry;

import org.exoplatform.management.service.api.AbstractResourceHandler;
import org.exoplatform.management.service.api.StagingService;

public class ApplicationRegistryHandler extends AbstractResourceHandler {
  @Override
  public String getPath() {
    return StagingService.REGISTRY_PATH;
  }
}
