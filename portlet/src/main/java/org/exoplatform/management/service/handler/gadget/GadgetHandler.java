package org.exoplatform.management.service.handler.gadget;

import org.exoplatform.management.service.api.AbstractResourceHandler;
import org.exoplatform.management.service.api.StagingService;

public class GadgetHandler extends AbstractResourceHandler {

  @Override
  public String getPath() {
    return StagingService.GADGET_PATH;
  }
}