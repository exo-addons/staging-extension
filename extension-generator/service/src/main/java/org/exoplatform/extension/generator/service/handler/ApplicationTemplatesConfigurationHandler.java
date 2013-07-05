package org.exoplatform.extension.generator.service.handler;

import java.io.InputStream;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.exoplatform.container.xml.ComponentPlugin;
import org.exoplatform.container.xml.ExternalComponentPlugins;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ObjectParameter;
import org.exoplatform.extension.generator.service.api.AbstractConfigurationHandler;
import org.exoplatform.extension.generator.service.api.Utils;
import org.exoplatform.management.ecmadmin.operations.templates.applications.ApplicationTemplatesMetadata;
import org.exoplatform.services.cms.views.ApplicationTemplateManagerService;
import org.exoplatform.services.cms.views.PortletTemplatePlugin;
import org.exoplatform.services.cms.views.PortletTemplatePlugin.PortletTemplateConfig;

public abstract class ApplicationTemplatesConfigurationHandler extends AbstractConfigurationHandler {
  private String applicationTemplatesHomePath;
  private String applicationConfigurationFileName;
  private String stagingExtensionPath;
  private String portletName;

  public ApplicationTemplatesConfigurationHandler(String applicationTemplatesHomePath, String applicationConfigurationFileName, String stagingExtensionPath, String portletName) {
    this.applicationTemplatesHomePath = applicationTemplatesHomePath;
    this.applicationConfigurationFileName = applicationConfigurationFileName;
    this.stagingExtensionPath = stagingExtensionPath;
    this.portletName = portletName;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean writeData(ZipOutputStream zos, Collection<String> selectedResources) {
    Set<String> filteredSelectedResources = filterSelectedResources(selectedResources, stagingExtensionPath);
    if (filteredSelectedResources.isEmpty()) {
      return false;
    }
    ApplicationTemplatesMetadata metadata = new ApplicationTemplatesMetadata();
    for (String resourcePath : filteredSelectedResources) {
      try {
        ZipFile zipFile = getExportedFileFromOperation(resourcePath);
        // Compute Metadata
        ApplicationTemplatesMetadata tmpMetadata = getApplicationTemplatesMetadata(zipFile);
        if (tmpMetadata != null) {
          metadata.getTitleMap().putAll(tmpMetadata.getTitleMap());
        }
        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        while (entries.hasMoreElements()) {
          ZipEntry zipEntry = (ZipEntry) entries.nextElement();
          if (zipEntry.isDirectory() || zipEntry.getName().equals("") || !zipEntry.getName().endsWith(".gtmpl")) {
            continue;
          }
          try {
            InputStream inputStream = zipFile.getInputStream(zipEntry);
            Utils.writeZipEnry(zos, DMS_CONFIGURATION_LOCATION + zipEntry.getName(), inputStream);
          } catch (Exception e) {
            getLogger().error(e);
            return false;
          }
        }
      } finally {
        clearTempFiles();
      }
    }

    ExternalComponentPlugins externalComponentPlugins = new ExternalComponentPlugins();
    InitParams params = new InitParams();
    params.addParam(getValueParam("portletName", portletName));
    params.addParam(getValueParam("portlet.template.path", applicationTemplatesHomePath.replace("WEB-INF", "war:")));

    for (String selectedResourcePath : filteredSelectedResources) {
      PortletTemplateConfig templateConfig = new PortletTemplateConfig();

      String tmpPath = selectedResourcePath.replace(stagingExtensionPath + "/", "");
      String[] paths = tmpPath.split("/");

      templateConfig.setCategory(paths[0]);
      templateConfig.setTemplateName(paths[1]);

      String relativePath = selectedResourcePath.replace("/ecmadmin/", "");

      String templateTitle = templateConfig.getTemplateName();
      if (metadata != null && metadata.getTitle(relativePath) != null) {
        templateTitle = metadata.getTitle(relativePath);
      }
      templateConfig.setTitle(templateTitle);

      ObjectParameter objectParameter = new ObjectParameter();
      objectParameter.setName(paths[1].replace(".gtmpl", ""));
      objectParameter.setObject(templateConfig);
      params.addParam(objectParameter);
    }

    ComponentPlugin plugin = createComponentPlugin("templates.plugin", PortletTemplatePlugin.class.getName(), "addPlugin", params);
    addComponentPlugin(externalComponentPlugins, ApplicationTemplateManagerService.class.getName(), plugin);

    return Utils.writeConfiguration(zos, DMS_CONFIGURATION_LOCATION + applicationConfigurationFileName, externalComponentPlugins);
  }

}
