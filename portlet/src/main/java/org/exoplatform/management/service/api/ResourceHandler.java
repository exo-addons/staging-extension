package org.exoplatform.management.service.api;

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
   * Filters options of current resource. Turn on "filter" value for all options
   * if parameter allFilter=true
   * 
   * @param options
   * @param allFilter
   */
  public abstract void filterOptions(Map<String, String> options, boolean allFilter);

  /**
   * Synchronise selected resources with host identified by host and port, by
   * using the selected options.
   * 
   * @param resources
   * @param options
   * @param path
   * @param port
   * @return
   */
  public boolean synchronizeData(Set<String> resources, boolean isSSL, String host, String port, String username, String password, Map<String, String> options);
}
