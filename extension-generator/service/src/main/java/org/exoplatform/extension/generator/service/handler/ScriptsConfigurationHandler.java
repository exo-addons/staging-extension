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
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ObjectParameter;
import org.exoplatform.extension.generator.service.api.AbstractConfigurationHandler;
import org.exoplatform.extension.generator.service.api.ExtensionGenerator;
import org.exoplatform.extension.generator.service.api.Utils;
import org.exoplatform.services.cms.impl.ResourceConfig;
import org.exoplatform.services.cms.impl.ResourceConfig.Resource;
import org.exoplatform.services.cms.scripts.ScriptService;
import org.exoplatform.services.cms.scripts.impl.ScriptPlugin;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

public class ScriptsConfigurationHandler extends AbstractConfigurationHandler {
  private static final String SCRIPT_CONFIGURATION_NAME = "scripts-configuration.xml";
  private static final List<String> configurationPaths = new ArrayList<String>();
  static {
    configurationPaths.add(DMS_CONFIGURATION_LOCATION.replace("WEB-INF", "war:") + SCRIPT_CONFIGURATION_NAME);
  }

  private Log log = ExoLogger.getLogger(this.getClass());

  /**
   * {@inheritDoc}
   */
  public boolean writeData(ZipOutputStream zos, Collection<String> selectedResources) {
    Set<String> filteredSelectedResources = filterSelectedResources(selectedResources, ExtensionGenerator.ECM_SCRIPT_PATH);
    if (filteredSelectedResources.isEmpty()) {
      return false;
    }

    List<String> filterScripts = new ArrayList<String>();
    for (String resourcePath : filteredSelectedResources) {
      String scriptName = resourcePath.replace(ExtensionGenerator.ECM_SCRIPT_PATH + "/", "");
      filterScripts.add(scriptName);
    }

    ExternalComponentPlugins externalComponentPlugins = new ExternalComponentPlugins();
    List<ResourceConfig.Resource> scripts = new ArrayList<ResourceConfig.Resource>();
    {
      InitParams params = new InitParams();
      params.addParam(getValueParam("autoCreateInNewRepository", "true"));
      String location = DMS_CONFIGURATION_LOCATION.replace("WEB-INF", "war:");
      // Delete last '/'
      location = location.substring(0, location.length() - 1);
      params.addParam(getValueParam("predefinedScriptsLocation", location));
      ObjectParameter objectParameter = new ObjectParameter();
      objectParameter.setName("predefined.scripts");
      ResourceConfig resourceConfig = new ResourceConfig();
      resourceConfig.setRessources(scripts);
      objectParameter.setObject(resourceConfig);
      params.addParam(objectParameter);

      ComponentPlugin plugin = createComponentPlugin("manage.script.plugin", ScriptPlugin.class.getName(), "addScriptPlugin", params);
      addComponentPlugin(externalComponentPlugins, ScriptService.class.getName(), plugin);
    }
    try {
      ZipFile zipFile = getExportedFileFromOperation(ExtensionGenerator.ECM_SCRIPT_PATH, filterScripts.toArray(new String[0]));
      Enumeration<? extends ZipEntry> entries = zipFile.entries();
      while (entries.hasMoreElements()) {
        ZipEntry zipEntry = (ZipEntry) entries.nextElement();
        String relatifLocation = zipEntry.getName().replaceFirst("script/", "");
        String scriptConfigurationLocation = DMS_CONFIGURATION_LOCATION + "scripts/" + relatifLocation;
        Resource scriptResource = new ResourceConfig.Resource();
        scriptResource.setName(relatifLocation);
        scriptResource.setDescription(relatifLocation);
        scripts.add(scriptResource);
        try {
          InputStream inputStream = zipFile.getInputStream(zipEntry);
          Utils.writeZipEnry(zos, scriptConfigurationLocation, inputStream);
        } catch (Exception e) {
          log.error("Error while marshalling " + zipEntry.getName(), e);
        }
      }
    } finally {
      clearTempFiles();
    }
    return Utils.writeConfiguration(zos, DMS_CONFIGURATION_LOCATION + SCRIPT_CONFIGURATION_NAME, externalComponentPlugins);
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
