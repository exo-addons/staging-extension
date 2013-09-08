package org.exoplatform.management.service.api;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

public interface SynchronizationService {

  /**
   * Synchronize Managed Resources
   * 
   * @param selectedResources
   * @param options
   * @param host
   * @param port
   * 
   * @throws java.io.IOException
   */
  void synchronize(Set<String> selectedResources, Map<String, String> options, String isSSLString, String host, String port, String username, String password) throws IOException;

}
