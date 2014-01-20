package org.exoplatform.management.service.api;

import java.util.List;

public interface SynchronizationService {

  public void init(ChromatticService chromatticService);

  public List<TargetServer> getSynchonizationServers();

  public void addSynchonizationServer(TargetServer targetServer);

  public void removeSynchonizationServer(TargetServer targetServer);

  /**
   * Synchronize Managed Resources
   *
   * @param selectedResourcesCategories
   * @param targetServer
   * @throws java.io.IOException
   */
  void synchronize(List<ResourceCategory> selectedResourcesCategories, TargetServer targetServer) throws Exception;

}
