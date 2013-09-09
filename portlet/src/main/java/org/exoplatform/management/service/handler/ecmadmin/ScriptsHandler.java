package org.exoplatform.management.service.handler.ecmadmin;

import org.exoplatform.management.service.api.AbstractResourceHandler;
import org.exoplatform.management.service.api.StagingService;

import java.io.File;
import java.util.Map;
import java.util.Set;

public class ScriptsHandler extends AbstractResourceHandler {
  @Override
  public String getParentPath() {
    return StagingService.ECM_SCRIPT_PATH;
  }

  @Override
  public boolean synchronizeData(Set<String> resources, boolean isSSL, String host, String port, String username, String password, Map<String, String> options) {
    Set<String> selectedResources = filterSubResources(resources);
    if (selectedResources == null || selectedResources.isEmpty()) {
      return false;
    }

    Map<String, String> selectedExportOptions = filterOptions(options, OPERATION_EXPORT_PREFIX, true);

    for (String resourcePath : selectedResources) {
      resourcePath = resourcePath.replace(getParentPath() + "/", "");
      selectedExportOptions.put(resourcePath, "filter");
    }
    File file = getExportedFileFromOperation(getParentPath(), selectedExportOptions);
    synhronizeData(file, isSSL, host, port, getParentPath(), username, password, filterOptions(options, OPERATION_IMPORT_PREFIX, true));
    return true;
  }
}
