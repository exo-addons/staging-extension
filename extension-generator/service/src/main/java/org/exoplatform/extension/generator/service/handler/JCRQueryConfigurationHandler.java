package org.exoplatform.extension.generator.service.handler;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.exoplatform.extension.generator.service.AbstractConfigurationHandler;
import org.exoplatform.extension.generator.service.api.ExtensionGenerator;
import org.exoplatform.extension.generator.service.api.Utils;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

public class JCRQueryConfigurationHandler extends AbstractConfigurationHandler {
  private final List<String> configurationPaths = new ArrayList<String>();

  private Log log = ExoLogger.getLogger(this.getClass());

  public boolean writeData(ZipOutputStream zos, Set<String> selectedResources) {
    Set<String> filteredSelectedResources = filterSelectedResources(selectedResources, ExtensionGenerator.ECM_QUERY_PATH);
    if (filteredSelectedResources.isEmpty()) {
      return false;
    }
    configurationPaths.clear();
    ZipFile zipFile = getExportedFileFromOperation(ExtensionGenerator.ECM_QUERY_PATH);
    Enumeration<? extends ZipEntry> entries = zipFile.entries();
    while (entries.hasMoreElements()) {
      ZipEntry zipEntry = (ZipEntry) entries.nextElement();
      try {
        InputStream inputStream = zipFile.getInputStream(zipEntry);
        Utils.writeZipEnry(zos, DMS_CONFIGURATION_LOCATION + zipEntry.getName(), inputStream);
        configurationPaths.add(DMS_CONFIGURATION_LOCATION.replace("WEB-IN", "war:") + zipEntry.getName());
      } catch (Exception e) {
        log.error("Error while getting NamespaceConfiguration Entry", e);
        return false;
      }
    }
    return true;
  }

  @Override
  public List<String> getConfigurationPaths() {
    return configurationPaths;
  }

  @Override
  protected Log getLogger() {
    return log;
  }
}
