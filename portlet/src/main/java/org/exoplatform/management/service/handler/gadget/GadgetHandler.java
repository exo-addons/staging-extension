package org.exoplatform.management.service.handler.gadget;

import org.exoplatform.management.service.api.AbstractResourceHandler;
import org.exoplatform.management.service.api.StagingService;

import java.io.File;
import java.util.Map;
import java.util.Set;

public class GadgetHandler extends AbstractResourceHandler {

  @Override
  public String getParentPath() {
    return StagingService.GADGET_PATH;
  }

  @Override
  public boolean synchronizeData(Set<String> resources, boolean isSSL, String host, String port, String username, String password, Map<String, String> options) {
    Set<String> selectedResources = filterSubResources(resources);
    if (selectedResources == null || selectedResources.isEmpty()) {
      return false;
    }

    Map<String, String> selectedExportOptions = filterOptions(options, OPERATION_EXPORT_PREFIX, false);

    for (String resourcePath : selectedResources) {
      File file = getExportedFileFromOperation(resourcePath, selectedExportOptions);
      synhronizeData(file, isSSL, host, port, getParentPath(), username, password, filterOptions(options, OPERATION_IMPORT_PREFIX, false));
    }
    clearTempFiles();
    return true;
  }
}