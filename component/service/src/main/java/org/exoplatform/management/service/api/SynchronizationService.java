package org.exoplatform.management.service.api;

import java.util.List;

public interface SynchronizationService {

  public List<TargetServer> getSynchonizationServers();

  public void addSynchonizationServer(TargetServer targetServer);

  public void removeSynchonizationServer(TargetServer targetServer);

  /**
   * Synchronize Managed Resources
   *
   */
  void synchronize(List<ResourceCategory> selectedResourcesCategories, TargetServer targetServer) throws Exception;

}
