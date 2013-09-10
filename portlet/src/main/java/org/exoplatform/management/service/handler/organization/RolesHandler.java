package org.exoplatform.management.service.handler.organization;

import org.exoplatform.management.service.api.AbstractResourceHandler;
import org.exoplatform.management.service.api.StagingService;
import org.gatein.management.api.controller.ManagedResponse;

import java.util.Map;
import java.util.Set;

public class RolesHandler extends AbstractResourceHandler {

  @Override
  public String getParentPath() {
    return StagingService.ROLE_PATH;
  }

  @Override
  public boolean synchronizeData(Set<String> resources, boolean isSSL, String host, String port, String username, String password, Map<String, String> options) {
    Set<String> selectedResources = filterSubResources(resources);
    if (selectedResources == null || selectedResources.isEmpty()) {
      return false;
    }

    for (String resourcePath : selectedResources) {
      ManagedResponse managedResponse = getExportedResourceFromOperation(resourcePath, filterOptions(options, OPERATION_EXPORT_PREFIX, false));
      synhronizeData(managedResponse, isSSL, host, port, getParentPath(), username, password, filterOptions(options, OPERATION_EXPORT_PREFIX, false));
    }
    return true;
  }
}