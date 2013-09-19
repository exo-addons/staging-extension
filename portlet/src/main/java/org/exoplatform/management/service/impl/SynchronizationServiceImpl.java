package org.exoplatform.management.service.impl;

import org.exoplatform.management.service.api.*;
import org.exoplatform.management.service.handler.ResourceHandlerLocator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.IOException;
import java.util.List;

public class SynchronizationServiceImpl implements SynchronizationService {

  private Log log = ExoLogger.getLogger(SynchronizationServiceImpl.class);

  ChromatticService chromatticService;

  public SynchronizationServiceImpl() {
  }

  @Override
  public void init(ChromatticService chromatticService) {
    this.chromatticService = chromatticService;
    chromatticService.init();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<TargetServer> getSynchonizationServers() {
    return chromatticService.getSynchonizationServers();
  }

  @Override
  public void addSynchonizationServer(TargetServer targetServer) {
    chromatticService.addSynchonizationServer(targetServer);
  }

  @Override
  public void removeSynchonizationServer(TargetServer targetServer) {
    chromatticService.removeSynchonizationServer(targetServer);
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