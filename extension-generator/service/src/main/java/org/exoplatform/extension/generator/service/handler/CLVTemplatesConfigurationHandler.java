package org.exoplatform.extension.generator.service.handler;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.exoplatform.container.xml.ComponentPlugin;
import org.exoplatform.container.xml.ExternalComponentPlugins;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ObjectParameter;
import org.exoplatform.extension.generator.service.AbstractConfigurationHandler;
import org.exoplatform.extension.generator.service.api.ExtensionGenerator;
import org.exoplatform.extension.generator.service.api.Utils;
import org.exoplatform.services.cms.views.ApplicationTemplateManagerService;
import org.exoplatform.services.cms.views.PortletTemplatePlugin;
import org.exoplatform.services.cms.views.PortletTemplatePlugin.PortletTemplateConfig;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

public class CLVTemplatesConfigurationHandler extends AbstractConfigurationHandler {
  private static final String APPLICATION_CLV_CONFIGURATION_LOCATION = DMS_CONFIGURATION_LOCATION + "templates/applications/content-list-viewer";
  private static final String APPLICATION_CLV_CONFIGURATION_NAME = "application-clv-templates-configuration.xml";
  private static final List<String> configurationPaths = new ArrayList<String>();
  static {
    configurationPaths.add(DMS_CONFIGURATION_LOCATION.replace("WEB-INF", "war:") + APPLICATION_CLV_CONFIGURATION_NAME);
  }

  private Log log = ExoLogger.getLogger(this.getClass());

  public boolean writeData(ZipOutputStream zos, Set<String> selectedResources) {
    Set<String> filteredSelectedResources = filterSelectedResources(selectedResources, ExtensionGenerator.ECM_TEMPLATES_APPLICATION_CLV_PATH);
    if (filteredSelectedResources.isEmpty()) {
      return false;
    }
    for (String resourcePath : filteredSelectedResources) {
      ZipFile zipFile = getExportedFileFromOperation(resourcePath);
      Enumeration<? extends ZipEntry> entries = zipFile.entries();
      while (entries.hasMoreElements()) {
        ZipEntry zipEntry = (ZipEntry) entries.nextElement();
        try {
          InputStream inputStream = zipFile.getInputStream(zipEntry);
          Utils.writeZipEnry(zos, DMS_CONFIGURATION_LOCATION + zipEntry.getName(), inputStream);
        } catch (Exception e) {
          log.error("Error while getting NamespaceConfiguration Entry", e);
          return false;
        }
      }
    }

    ExternalComponentPlugins externalComponentPlugins = new ExternalComponentPlugins();
    InitParams params = new InitParams();
    params.addParam(getValueParam("portletName", "content-list-viewer"));
    params.addParam(getValueParam("portlet.template.path", APPLICATION_CLV_CONFIGURATION_LOCATION.replace("WEB-INF", "war:")));

    for (String selectedResourcePath : filteredSelectedResources) {
      PortletTemplateConfig templateConfig = new PortletTemplateConfig();

      selectedResourcePath = selectedResourcePath.replace(ExtensionGenerator.ECM_TEMPLATES_APPLICATION_CLV_PATH + "/", "");
      String[] paths = selectedResourcePath.split("/");

      templateConfig.setCategory(paths[0]);
      templateConfig.setTemplateName(paths[1]);

      ObjectParameter objectParameter = new ObjectParameter();
      objectParameter.setName(paths[1].replace(".gtmpl", ""));
      objectParameter.setObject(templateConfig);
      params.addParam(objectParameter);
    }

    ComponentPlugin plugin = createComponentPlugin("clv.templates.plugin", PortletTemplatePlugin.class.getName(), "addPlugin", params);
    addComponentPlugin(externalComponentPlugins, ApplicationTemplateManagerService.class.getName(), plugin);

    return Utils.writeConfiguration(zos, DMS_CONFIGURATION_LOCATION + APPLICATION_CLV_CONFIGURATION_NAME, externalComponentPlugins);
  }

  @Override
  protected Log getLogger() {
    return log;
  }

  @Override
  public List<String> getConfigurationPaths() {
    return configurationPaths;
  }
}
