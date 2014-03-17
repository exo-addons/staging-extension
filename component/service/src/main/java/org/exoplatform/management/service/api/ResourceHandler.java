package org.exoplatform.management.service.api;

import java.util.List;
import java.util.Map;
import java.util.zip.ZipOutputStream;

public interface ResourceHandler {

  /**
   * @return paren path resource
   */
  public String getPath();

  /**
   * Synchronise selected resources with host identified by host and port, by
   * using the selected options.
   * 
   * @param resourcesPaths
   * @param exportOptions
   * @param importOptions
   * @param targetServer
   */
  public abstract void synchronize(List<Resource> resourcesPaths, Map<String, String> exportOptions, Map<String, String> importOptions, TargetServer targetServer) throws Exception;

  /**
   * Export selected resources with selected options.
   * 
   * @param list : List of resources to export
   * @param exportFileOS : Write in this zip file the entries
   * @param exportOptions export options
   * 
   * @throws Exception if an error occurs
   */
  public void export(List<Resource> list, ZipOutputStream exportFileOS, Map<String, String> exportOptions) throws Exception;
}