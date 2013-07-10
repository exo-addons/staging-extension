package org.exoplatform.extension.synchronization.service.handler;

import java.io.File;
import java.util.Map;
import java.util.Set;

import org.exoplatform.extension.synchronization.service.api.AbstractResourceHandler;
import org.exoplatform.extension.synchronization.service.api.SynchronizationService;

public class ScriptsHandler extends AbstractResourceHandler {
  @Override
  public String getParentPath() {
    return SynchronizationService.ECM_SCRIPT_PATH;
  }

  @Override
  public boolean synchronizeData(Set<String> resources, boolean isSSL, String host, String port, String username, String password, Map<String, String> options) {
    filterSubResources(resources);
    if (selectedResources == null || selectedResources.isEmpty()) {
      return false;
    }
    filterOptions(options);
    for (String resourcePath : selectedResources) {
      resourcePath = resourcePath.replace(getParentPath() + "/", "");
      selectedOptions.put(resourcePath, "true");
    }
    File file = getExportedFileFromOperation(getParentPath(), selectedOptions.keySet().toArray(new String[0]));
    synhronizeData(file, isSSL, host, port, getParentPath(), username, password, selectedOptions);
    return true;
  }
}
