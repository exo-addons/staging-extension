package org.exoplatform.extension.generator.service.handler;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.IOUtils;
import org.exoplatform.application.registry.ApplicationCategoriesPlugins;
import org.exoplatform.application.registry.ApplicationRegistryService;
import org.exoplatform.container.xml.ComponentPlugin;
import org.exoplatform.container.xml.ExternalComponentPlugins;
import org.exoplatform.container.xml.ObjectParameter;
import org.exoplatform.extension.generator.service.api.AbstractConfigurationHandler;
import org.exoplatform.extension.generator.service.api.ExtensionGenerator;
import org.exoplatform.extension.generator.service.api.Utils;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

public class ApplicationRegistryConfigurationHandler extends AbstractConfigurationHandler {
  private static final String APPLICATION_REGISTRY_CONFIGURATION_XML = "WEB-INF/conf/custom-extension/portal/application-registry-configuration.xml";
  private static final List<String> configurationPaths = new ArrayList<String>();
  static {
    configurationPaths.add(APPLICATION_REGISTRY_CONFIGURATION_XML.replace("WEB-INF", "war:"));
  }

  private Log log = ExoLogger.getLogger(this.getClass());

  @Override
  protected Log getLogger() {
    return log;
  }

  /**
   * {@inheritDoc}
   */
  public boolean writeData(ZipOutputStream zos, Collection<String> selectedResources) {
    Set<String> filteredSelectedResources = filterSelectedResources(selectedResources, ExtensionGenerator.REGISTRY_PATH);
    if (filteredSelectedResources.isEmpty()) {
      return false;
    }
    ExternalComponentPlugins externalComponentPlugins = new ExternalComponentPlugins();
    ComponentPlugin plugin = createComponentPlugin("new.registry.category", ApplicationCategoriesPlugins.class.getName(), "initListener", null);
    addComponentPlugin(externalComponentPlugins, ApplicationRegistryService.class.getName(), plugin);

    for (String resourcePath : filteredSelectedResources) {

      try {
        ZipFile zipFile = getExportedFileFromOperation(resourcePath);
        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        while (entries.hasMoreElements()) {
          ZipEntry zipEntry = (ZipEntry) entries.nextElement();
          try {
            InputStream inputStream = zipFile.getInputStream(zipEntry);
            ObjectParameter objectParameter = Utils.fromXML(IOUtils.toByteArray(inputStream), ObjectParameter.class);
            objectParameter.setName(zipEntry.getName().replace(".xml", ""));
            addParameter(plugin, objectParameter);
          } catch (Exception e) {
            log.error("Error while marshalling " + zipEntry.getName(), e);
          }
        }
      } finally {
        clearTempFiles();
      }
    }
    return Utils.writeConfiguration(zos, APPLICATION_REGISTRY_CONFIGURATION_XML, externalComponentPlugins);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<String> getConfigurationPaths() {
    return configurationPaths;
  }
}
