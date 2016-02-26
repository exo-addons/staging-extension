package org.exoplatform.management.service.api;

import java.util.List;

public interface SynchronizationService {

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

  /**
   * 
   * Test Server connection
   * 
   * @param targetServer : the server connection details (host, port, username, password).
   * @throws Exception 
   */
  void testServerConnection(TargetServer targetServer) throws Exception;

}
