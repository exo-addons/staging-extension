package org.exoplatform.management.service.handler.mop;

import org.exoplatform.management.service.api.AbstractResourceHandler;
import org.exoplatform.management.service.api.StagingService;
import org.exoplatform.portal.mop.SiteType;
import org.gatein.management.api.controller.ManagedResponse;

import java.util.Map;
import java.util.Set;

public class MOPSiteHandler extends AbstractResourceHandler {

  String parentPath;

  public MOPSiteHandler(SiteType siteType) {
    parentPath = "/site/" + siteType.getName() + "sites/";
  }

  @Override
  public String getParentPath() {
    return parentPath;
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
      synhronizeData(managedResponse, isSSL, host, port, StagingService.SITES_PARENT_PATH, username, password, selectedImportOptions);
    }
    return true;
  }
}
