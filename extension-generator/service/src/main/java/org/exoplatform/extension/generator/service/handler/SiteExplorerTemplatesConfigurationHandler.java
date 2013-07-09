package org.exoplatform.extension.generator.service.handler;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.apache.tika.io.IOUtils;
import org.exoplatform.container.xml.ComponentPlugin;
import org.exoplatform.container.xml.ExternalComponentPlugins;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ObjectParameter;
import org.exoplatform.extension.generator.service.api.AbstractConfigurationHandler;
import org.exoplatform.extension.generator.service.api.ExtensionGenerator;
import org.exoplatform.extension.generator.service.api.Utils;
import org.exoplatform.services.cms.views.ManageViewService;
import org.exoplatform.services.cms.views.impl.ManageViewPlugin;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

public class SiteExplorerTemplatesConfigurationHandler extends AbstractConfigurationHandler {
  private static final String VIEW_CONFIGURATION_LOCATION = DMS_CONFIGURATION_LOCATION + "view";
  private static final String VIEW_TEMPLATES_LOCATION = DMS_CONFIGURATION_LOCATION + "view/ecm-explorer";
  private static final String VIEW_CONFIGURATION_NAME = "view-templates-configuration.xml";
  private static final String VIEW_CONFIGURATION_FULL_PATH = VIEW_CONFIGURATION_LOCATION + "/" + VIEW_CONFIGURATION_NAME;
  private static final List<String> configurationPaths = new ArrayList<String>();
  static {
    configurationPaths.add(VIEW_CONFIGURATION_FULL_PATH.replace("WEB-INF", "war:"));
  }

  private Log log = ExoLogger.getLogger(this.getClass());

  /**
   * {@inheritDoc}
   */
  public boolean writeData(ZipOutputStream zos, Collection<String> selectedResources) {
    Set<String> filteredSelectedResources = filterSelectedResources(selectedResources, ExtensionGenerator.ECM_VIEW_TEMPLATES_PATH);
    if (filteredSelectedResources.isEmpty()) {
      return false;
    }
    InitParams allParams = null;
    // Copy gtmpl in WAR and get all initParams in a single one
    for (String selectedResource : filteredSelectedResources) {
      ZipFile zipFile = getExportedFileFromOperation(selectedResource);
      try {
        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        while (entries.hasMoreElements()) {
          ZipEntry entry = (ZipEntry) entries.nextElement();
          String filePath = entry.getName();
          if (!filePath.startsWith("view/")) {
            continue;
          }
          // Skip directories
          // & Skip empty entries
          // & Skip entries not in sites/zip
          if (entry.isDirectory() || filePath.trim().isEmpty() || !(filePath.endsWith(".gtmpl") || filePath.endsWith(".xml"))) {
            continue;
          }
          InputStream inputStream = zipFile.getInputStream(entry);
          if (filePath.endsWith(".gtmpl")) {
            String location = VIEW_TEMPLATES_LOCATION + "/" + extractTemplateName(filePath);
            Utils.writeZipEnry(zos, location, inputStream);
          } else if (filePath.endsWith(".xml")) {
            log.debug("Parsing : " + filePath);

            InitParams initParams = Utils.fromXML(IOUtils.toByteArray(inputStream), InitParams.class);
            if (allParams == null) {
              allParams = initParams;
            } else {
              @SuppressWarnings("unchecked")
              Iterator<ObjectParameter> iterator = initParams.getObjectParamIterator();
              while (iterator.hasNext()) {
                ObjectParameter objectParameter = (ObjectParameter) iterator.next();
                allParams.addParameter(objectParameter);
              }
            }
          }
        }
      } catch (Exception e) {
        log.error("Error iccured while handling view templates", e);
        throw new RuntimeException(e);
      } finally {
        clearTempFiles();
      }
    }

    ExternalComponentPlugins externalComponentPlugins = new ExternalComponentPlugins();
    // Add constant init params
    allParams.addParam(getValueParam("autoCreateInNewRepository", "true"));
    allParams.addParam(getValueParam("predefinedViewsLocation", VIEW_CONFIGURATION_LOCATION.replace("WEB-INF", "war:")));

    ComponentPlugin plugin = createComponentPlugin("manage.view.plugin", ManageViewPlugin.class.getName(), "setManageViewPlugin", allParams);
    addComponentPlugin(externalComponentPlugins, ManageViewService.class.getName(), plugin);

    return Utils.writeConfiguration(zos, VIEW_CONFIGURATION_FULL_PATH, externalComponentPlugins);
  }

  @Override
  public List<String> getConfigurationPaths() {
    return configurationPaths;
  }

  @Override
  protected Log getLogger() {
    return log;
  }

  private String extractTemplateName(String filePath) {
    return filePath.replace("view/templates/", "");
  }

}
