package org.exoplatform.management.service.handler.content;

import org.exoplatform.management.service.api.AbstractResourceHandler;
import org.exoplatform.management.service.api.StagingService;

import java.io.File;
import java.util.Map;
import java.util.Set;

public class SiteContentsHandler extends AbstractResourceHandler {
  private static final String JCR_QUERY_ID = StagingService.CONTENT_SITES_PATH + "/" + OPERATION_EXPORT_PREFIX + "/query";

  @Override
  public String getParentPath() {
    return StagingService.CONTENT_SITES_PATH;
  }

  @Override
  public boolean synchronizeData(Set<String> resources, boolean isSSL, String host, String port, String username, String password, Map<String, String> options) {
    filterSubResources(resources);
    if (selectedResources == null || selectedResources.isEmpty()) {
      return false;
    }
    String sqlQuery = null;
    if (options.containsKey(JCR_QUERY_ID)) {
      sqlQuery = options.get(JCR_QUERY_ID);
    }
    filterOptions(options, true);
    if (selectedExportOptions.containsKey("query")) {
      selectedExportOptions.remove("query");
      selectedExportOptions.put("query:" + sqlQuery, "filter");
    }
    for (String resourcePath : selectedResources) {
      File file = getExportedFileFromOperation(resourcePath, selectedExportOptions);
      synhronizeData(file, isSSL, host, port, getParentPath(), username, password, selectedImportOptions);
    }
    clearTempFiles();
    return true;
  }
}
