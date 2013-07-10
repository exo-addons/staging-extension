package org.exoplatform.extension.synchronization.service.handler;

import java.io.File;
import java.util.Map;
import java.util.Set;

import org.exoplatform.extension.synchronization.service.api.AbstractResourceHandler;
import org.exoplatform.extension.synchronization.service.api.SynchronizationService;

public class TaxonomyConfigurationHandler extends AbstractResourceHandler {
  @Override
  public String getParentPath() {
    return SynchronizationService.ECM_VIEW_CONFIGURATION_PATH;
  }

  @Override
  public boolean synchronizeData(Set<String> resources, boolean isSSL, String host, String port, String username, String password, Map<String, String> options) {
    filterSubResources(resources);
    if (selectedResources == null || selectedResources.isEmpty()) {
      return false;
    }
    filterOptions(options);
    for (String resourcePath : selectedResources) {
      File file = getExportedFileFromOperation(resourcePath, selectedOptions.keySet().toArray(new String[0]));
      synhronizeData(file, isSSL, host, port, getParentPath(), username, password, selectedOptions);
    }
    clearTempFiles();
    return true;
  }
}
