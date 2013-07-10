package org.exoplatform.extension.synchronization.service.api;

import java.util.Map;
import java.util.Set;

public interface ResourceHandler {

  /**
   * @return paren path resource
   */
  public abstract String getParentPath();

  /**
   * Filters subresources of parentPath. This operation retains only paths that
   * contains parentPath.
   * 
   * @param resources
   *          Set of managed resources paths
   * @return Set of sub resources path of type String
   */
  public abstract Set<String> filterSubResources(Set<String> resources);

  /**
   * Filters options of current resources.
   * 
   * @param resources
   * @return Map of options compatible with curent operation
   */
  public abstract Map<String, String> filterOptions(Map<String, String> resources);

  /**
   * Synchronise selected resources with host identified by host and port, by using the selected options.
   * 
   * @param selectedSubResources
   * @param options
   * @param path
   * @param port
   * @return
   */
  public boolean synchronizeData(Set<String> resources, boolean isSSL, String host, String port, String username, String password, Map<String, String> options);
}
