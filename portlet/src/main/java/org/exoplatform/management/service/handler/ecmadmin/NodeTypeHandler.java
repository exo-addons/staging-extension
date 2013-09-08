package org.exoplatform.management.service.handler.ecmadmin;

import org.exoplatform.management.service.api.AbstractResourceHandler;
import org.exoplatform.management.service.api.StagingService;

import java.io.File;
import java.util.Map;
import java.util.Set;

public class NodeTypeHandler extends AbstractResourceHandler {
  @Override
  public String getParentPath() {
    return StagingService.ECM_NODETYPE_PATH;
  }

  @Override
  public boolean synchronizeData(Set<String> resources, boolean isSSL, String host, String port, String username, String password, Map<String, String> options) {
    filterSubResources(resources);
    if (selectedResources == null || selectedResources.isEmpty()) {
      return false;
    }
    filterOptions(options, true);

    for (String resourcePath : selectedResources) {
      String actionTypeName = resourcePath.replace(getParentPath() + "/", "");
      selectedExportOptions.put(actionTypeName, "filter");
    }

    File file = getExportedFileFromOperation(getParentPath(), selectedExportOptions);
    synhronizeData(file, isSSL, host, port, getParentPath(), username, password, selectedImportOptions);
    return true;
  }
}
