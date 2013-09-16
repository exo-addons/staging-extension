package org.exoplatform.management.service.api;

import java.io.IOException;
import java.util.List;

public interface SynchronizationService {

  /**
   * Synchronize Managed Resources
   *
   * @param selectedResourcesCategories
   * @param targetServer
   * @throws java.io.IOException
   */
  void synchronize(List<ResourceCategory> selectedResourcesCategories, TargetServer targetServer) throws IOException;

}
