package org.exoplatform.management.service.api;

import java.util.List;
import java.util.Map;

public interface ResourceHandler {

  /**
   * @return paren path resource
   */
  public String getPath();

  /**
   * Synchronise selected resources with host identified by host and port, by
   * using the selected options.
   *
   *
   *
   * @param resourcesPaths
   * @param exportOptions
   * @param importOptions
   * @param targetServer
   * @return
   */
  public abstract void synchronize(List<Resource> resourcesPaths, Map<String, String> exportOptions, Map<String, String> importOptions, TargetServer targetServer);
}
