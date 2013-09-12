package org.exoplatform.management.service.handler.content;

import org.exoplatform.management.service.api.AbstractResourceHandler;
import org.exoplatform.management.service.api.StagingService;
import org.gatein.management.api.controller.ManagedResponse;

import java.util.Map;
import java.util.Set;

public class SiteContentsHandler extends AbstractResourceHandler {
  private static final String JCR_QUERY_ID = StagingService.CONTENT_SITES_PATH + "/" + OPERATION_EXPORT_PREFIX + "/query";

  @Override
  public String getParentPath() {
    return StagingService.CONTENT_SITES_PATH;
  }

  @Override
  public boolean synchronizeData(Set<String> resources, boolean isSSL, String host, String port, String username, String password, Map<String, String> options) {
    Set<String> selectedResources = filterSubResources(resources);
    if (selectedResources == null || selectedResources.isEmpty()) {
      return false;
    }

    Map<String, String> selectedExportOptions = filterOptions(options, OPERATION_EXPORT_PREFIX);
    Map<String, String> selectedImportOptions = filterOptions(options, OPERATION_IMPORT_PREFIX);

    for (String resourcePath : selectedResources) {
      ManagedResponse managedResponse = getExportedResourceFromOperation(resourcePath, selectedExportOptions);
      synhronizeData(managedResponse, isSSL, host, port, getParentPath(), username, password, selectedImportOptions);
    }
    return true;
  }
}
