package org.exoplatform.management.service.impl;

import java.util.List;

import org.exoplatform.management.service.api.ChromatticService;
import org.exoplatform.management.service.api.ResourceCategory;
import org.exoplatform.management.service.api.ResourceHandler;
import org.exoplatform.management.service.api.SynchronizationService;
import org.exoplatform.management.service.api.TargetServer;
import org.exoplatform.management.service.handler.ResourceHandlerLocator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

public class SynchronizationServiceImpl implements SynchronizationService {

  private Log log = ExoLogger.getLogger(SynchronizationServiceImpl.class);

  ChromatticService chromatticService;

  public SynchronizationServiceImpl(ChromatticService chromatticService) {
    this.chromatticService = chromatticService;
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
  public void synchronize(List<ResourceCategory> selectedResourcesCategories, TargetServer targetServer) throws Exception {
    for (ResourceCategory selectedResourceCategory : selectedResourcesCategories) {
      // Gets the right resource handler thanks to the Service Locator
      ResourceHandler resourceHandler = ResourceHandlerLocator.getResourceHandler(selectedResourceCategory.getPath());

      if (resourceHandler != null) {
        resourceHandler.synchronize(selectedResourceCategory.getResources(), selectedResourceCategory.getExportOptions(), selectedResourceCategory.getImportOptions(), targetServer);
      } else {
        log.error("No handler for " + selectedResourceCategory.getPath());
        throw new Exception("No handler for " + selectedResourceCategory.getPath());
      }
    }
  }
}