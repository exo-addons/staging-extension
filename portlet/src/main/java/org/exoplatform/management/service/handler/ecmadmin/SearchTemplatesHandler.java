package org.exoplatform.management.service.handler.ecmadmin;

import org.exoplatform.management.service.api.AbstractResourceHandler;
import org.exoplatform.management.service.api.StagingService;
import org.gatein.management.api.controller.ManagedResponse;

import java.util.Map;
import java.util.Set;

public class SearchTemplatesHandler extends AbstractResourceHandler {
  @Override
  public String getParentPath() {
    return StagingService.ECM_TEMPLATES_APPLICATION_SEARCH_PATH;
  }

  @Override
  public boolean synchronizeData(Set<String> resources, boolean isSSL, String host, String port, String username, String password, Map<String, String> options) {
    Set<String> selectedResources = filterSubResources(resources);
    if (selectedResources == null || selectedResources.isEmpty()) {
      return false;
    }

    Map<String, String> selectedExportOptions = filterOptions(options, OPERATION_EXPORT_PREFIX);

    for (String resourcePath : selectedResources) {
      resourcePath = resourcePath.replace(getParentPath() + "/", "");
      selectedExportOptions.put("filter/" + resourcePath, null);
    }

    ManagedResponse managedResponse = getExportedResourceFromOperation(getParentPath(), selectedExportOptions);
    synhronizeData(managedResponse, isSSL, host, port, getParentPath(), username, password, filterOptions(options, OPERATION_EXPORT_PREFIX));
    return true;
  }
}
