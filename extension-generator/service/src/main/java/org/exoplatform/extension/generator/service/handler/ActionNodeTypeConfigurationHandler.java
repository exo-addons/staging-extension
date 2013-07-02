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

import org.exoplatform.container.xml.ComponentPlugin;
import org.exoplatform.container.xml.ExternalComponentPlugins;
import org.exoplatform.container.xml.ValuesParam;
import org.exoplatform.extension.generator.service.api.AbstractConfigurationHandler;
import org.exoplatform.extension.generator.service.api.ExtensionGenerator;
import org.exoplatform.extension.generator.service.api.Utils;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.impl.AddNodeTypePlugin;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

public class ActionNodeTypeConfigurationHandler extends AbstractConfigurationHandler {
  private static final String ACTION_CONFIGURATION_NAME = "jcr-actions-component-plugins-configuration.xml";
  private static final String JCR_CONFIGURATION_LOCATION = "WEB-INF/conf/custom-extension/jcr/";
  private static final List<String> configurationPaths = new ArrayList<String>();
  static {
    configurationPaths.add(JCR_CONFIGURATION_LOCATION.replace("WEB-INF", "war:") + ACTION_CONFIGURATION_NAME);
  }

  private Log log = ExoLogger.getLogger(this.getClass());

  /**
   * {@inheritDoc}
   */
  @SuppressWarnings("unchecked")
  public boolean writeData(ZipOutputStream zos, Collection<String> selectedResources) {
    Set<String> filteredSelectedResources = filterSelectedResources(selectedResources, ExtensionGenerator.ECM_ACTION_PATH);
    if (filteredSelectedResources.isEmpty()) {
      return false;
    }

    ExternalComponentPlugins externalComponentPlugins = new ExternalComponentPlugins();

    List<String> filterActionTypes = new ArrayList<String>();
    for (String resourcePath : filteredSelectedResources) {
      String actionTypeName = resourcePath.replace(ExtensionGenerator.ECM_ACTION_PATH + "/", "");
      filterActionTypes.add(actionTypeName);
    }
    try {
      ZipFile zipFile = getExportedFileFromOperation(ExtensionGenerator.ECM_ACTION_PATH, filterActionTypes.toArray(new String[0]));
      ValuesParam valuesParam = new ValuesParam();
      valuesParam.setName("autoCreatedInNewRepository");
      valuesParam.setValues(new ArrayList<String>());
      ComponentPlugin plugin = createComponentPlugin("add.nodetype", AddNodeTypePlugin.class.getName(), "addPlugin", null, valuesParam);
      addComponentPlugin(externalComponentPlugins, RepositoryService.class.getName(), plugin);

      //
      Enumeration<? extends ZipEntry> entries = zipFile.entries();
      while (entries.hasMoreElements()) {
        ZipEntry zipEntry = (ZipEntry) entries.nextElement();
        String actionTypeConfigurationLocation = JCR_CONFIGURATION_LOCATION + zipEntry.getName();
        valuesParam.getValues().add(actionTypeConfigurationLocation.replace("WEB-INF", "war:"));
        try {
          InputStream inputStream = zipFile.getInputStream(zipEntry);
          Utils.writeZipEnry(zos, actionTypeConfigurationLocation, inputStream);
        } catch (Exception e) {
          log.error("Error while marshalling " + zipEntry.getName(), e);
        }
      }
    } finally {
      clearTempFiles();
    }
    return Utils.writeConfiguration(zos, JCR_CONFIGURATION_LOCATION + ACTION_CONFIGURATION_NAME, externalComponentPlugins);
  }

  @Override
  protected Log getLogger() {
    return log;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<String> getConfigurationPaths() {
    return configurationPaths;
  }
}
