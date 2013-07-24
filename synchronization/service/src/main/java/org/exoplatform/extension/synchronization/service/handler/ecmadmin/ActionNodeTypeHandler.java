package org.exoplatform.extension.synchronization.service.handler.ecmadmin;

import java.io.File;
import java.util.Map;
import java.util.Set;

import org.exoplatform.extension.synchronization.service.api.AbstractResourceHandler;
import org.exoplatform.extension.synchronization.service.api.SynchronizationService;

public class ActionNodeTypeHandler extends AbstractResourceHandler {

  @Override
  public String getParentPath() {
    return SynchronizationService.ECM_ACTION_PATH;
  }

  @Override
  public boolean synchronizeData(Set<String> resources, boolean isSSL, String host, String port, String username, String password, Map<String, String> options) {
    filterSubResources(resources);
    if (selectedResources == null || selectedResources.isEmpty()) {
      return false;
    }
    filterOptions(options, true);

    for (String resourcePath : selectedResources) {
      resourcePath = resourcePath.replace(getParentPath() + "/", "");
      selectedExportOptions.put(resourcePath, "filter");
    }

    File file = getExportedFileFromOperation(getParentPath(), selectedExportOptions);
    synhronizeData(file, isSSL, host, port, getParentPath(), username, password, selectedImportOptions);
    return true;
  }
}
