package org.exoplatform.extension.synchronization.service.handler.gadget;

import java.io.File;
import java.util.Map;
import java.util.Set;

import org.exoplatform.extension.synchronization.service.api.AbstractResourceHandler;
import org.exoplatform.extension.synchronization.service.api.SynchronizationService;

public class GadgetHandler extends AbstractResourceHandler {

  @Override
  public String getParentPath() {
    return SynchronizationService.GADGET_PATH;
  }

  @Override
  public boolean synchronizeData(Set<String> resources, boolean isSSL, String host, String port, String username, String password, Map<String, String> options) {
    filterSubResources(resources);
    if (selectedResources == null || selectedResources.isEmpty()) {
      return false;
    }
    filterOptions(options, false);
    for (String resourcePath : selectedResources) {
      File file = getExportedFileFromOperation(resourcePath, selectedExportOptions);
      synhronizeData(file, isSSL, host, port, getParentPath(), username, password, selectedImportOptions);
    }
    clearTempFiles();
    return true;
  }
}