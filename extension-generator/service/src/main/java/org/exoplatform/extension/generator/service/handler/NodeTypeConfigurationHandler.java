package org.exoplatform.extension.generator.service.handler;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.IOUtils;
import org.exoplatform.container.xml.ComponentPlugin;
import org.exoplatform.container.xml.Configuration;
import org.exoplatform.container.xml.ExternalComponentPlugins;
import org.exoplatform.container.xml.ValuesParam;
import org.exoplatform.extension.generator.service.api.AbstractConfigurationHandler;
import org.exoplatform.extension.generator.service.api.ExtensionGenerator;
import org.exoplatform.extension.generator.service.api.Utils;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.impl.AddNodeTypePlugin;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

public class NodeTypeConfigurationHandler extends AbstractConfigurationHandler {
  private static final String JCR_CONFIGURATION_NAME = "jcr-component-plugins-configuration.xml";
  private static final String JCR_CONFIGURATION_LOCATION = "WEB-INF/conf/custom-extension/jcr/";
  private static final String JCR_NAMESPACES_CONFIGURATION_XML = "nodetype/jcr-namespaces-configuration.xml";
  private static final List<String> configurationPaths = new ArrayList<String>();
  static {
    configurationPaths.add(JCR_CONFIGURATION_LOCATION.replace("WEB-INF", "war:") + JCR_CONFIGURATION_NAME);
  }

  private Log log = ExoLogger.getLogger(this.getClass());

  /**
   * {@inheritDoc}
   */
  @SuppressWarnings("unchecked")
  public boolean writeData(ZipOutputStream zos, Set<String> selectedResources) {
    Set<String> filteredSelectedResources = filterSelectedResources(selectedResources, ExtensionGenerator.ECM_NODETYPE_PATH);
    if (filteredSelectedResources.isEmpty()) {
      return false;
    }
    // jcr-namespaces-configuration.xml
    ExternalComponentPlugins externalComponentPlugins = new ExternalComponentPlugins();

    List<String> filterNodeTypes = new ArrayList<String>();
    for (String resourcePath : filteredSelectedResources) {
      String nodeTypeName = resourcePath.replace(ExtensionGenerator.ECM_NODETYPE_PATH + "/", "");
      filterNodeTypes.add(nodeTypeName);
    }
    try {
      ZipFile zipFile = getExportedFileFromOperation(ExtensionGenerator.ECM_NODETYPE_PATH, filterNodeTypes.toArray(new String[0]));
      ZipEntry namespaceConfigurationEntry = zipFile.getEntry(JCR_NAMESPACES_CONFIGURATION_XML);
      try {
        InputStream inputStream = zipFile.getInputStream(namespaceConfigurationEntry);
        Configuration configuration = Utils.fromXML(IOUtils.toByteArray(inputStream), Configuration.class);
        externalComponentPlugins = configuration.getExternalComponentPlugins(RepositoryService.class.getName());
      } catch (Exception e) {
        log.error("Error while getting NamespaceConfiguration Entry", e);
      }
      ValuesParam valuesParam = new ValuesParam();
      valuesParam.setName("autoCreatedInNewRepository");
      valuesParam.setValues(new ArrayList<String>());
      ComponentPlugin plugin = createComponentPlugin("add.nodetype", AddNodeTypePlugin.class.getName(), "addPlugin", null, valuesParam);
      addComponentPlugin(externalComponentPlugins, RepositoryService.class.getName(), plugin);

      //
      Enumeration<? extends ZipEntry> entries = zipFile.entries();
      while (entries.hasMoreElements()) {
        ZipEntry zipEntry = (ZipEntry) entries.nextElement();
        String nodeTypeConfigurationLocation = JCR_CONFIGURATION_LOCATION + zipEntry.getName();
        valuesParam.getValues().add(nodeTypeConfigurationLocation.replace("WEB-INF", "war:"));
        try {
          InputStream inputStream = zipFile.getInputStream(zipEntry);
          Utils.writeZipEnry(zos, nodeTypeConfigurationLocation, inputStream);
        } catch (Exception e) {
          log.error("Error while marshalling " + zipEntry.getName(), e);
        }
      }
    } finally {
      clearTempFiles();
    }
    return Utils.writeConfiguration(zos, JCR_CONFIGURATION_LOCATION + JCR_CONFIGURATION_NAME, externalComponentPlugins);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<String> getConfigurationPaths() {
    return configurationPaths;
  }

  @Override
  protected Log getLogger() {
    return log;
  }
}
