package org.exoplatform.management.service.impl;

import org.exoplatform.management.service.api.ResourceCategory;
import org.exoplatform.management.service.api.ResourceHandler;
import org.exoplatform.management.service.api.SynchronizationService;
import org.exoplatform.management.service.api.TargetServer;
import org.exoplatform.management.service.handler.ResourceHandlerLocator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import javax.inject.Singleton;
import java.io.IOException;
import java.util.*;

@Singleton
public class SynchronizationServiceImpl implements SynchronizationService {

  private Log log = ExoLogger.getLogger(SynchronizationServiceImpl.class);

  public SynchronizationServiceImpl() {

  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void synchronize(List<ResourceCategory> selectedResourcesCategories, TargetServer targetServer) throws IOException {
    for(ResourceCategory selectedResourceCategory : selectedResourcesCategories) {
      // Gets the right resource handler thanks to the Service Locator
      ResourceHandler resourceHandler = ResourceHandlerLocator.getResourceHandler(selectedResourceCategory.getPath());

      if(resourceHandler != null) {
        resourceHandler.synchronize(selectedResourceCategory.getResources(), selectedResourceCategory.getExportOptions(), selectedResourceCategory.getImportOptions(), targetServer);
      } else {
        log.error("No handler for " + selectedResourceCategory.getPath());
      }
    }
  }

}