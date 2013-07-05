package org.exoplatform.extension.generator.service.handler;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
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
import org.exoplatform.extension.generator.service.api.Utils;
import org.exoplatform.portal.config.NewPortalConfig;
import org.exoplatform.portal.config.NewPortalConfigListener;
import org.exoplatform.portal.config.UserPortalConfigService;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

public class MOPSiteConfigurationHandler extends AbstractConfigurationHandler {
  private static final String SITES_CONFIGURATION_LOCATION = "WEB-INF/conf/custom-extension/portal/";
  private static final String SITES_CONFIGURATION_NAME = "-sites-configuration.xml";
  private final List<String> configurationPaths = new ArrayList<String>();

  private Log log = ExoLogger.getLogger(this.getClass());
  String siteType;
  String siteResourcePath;

  public MOPSiteConfigurationHandler(SiteType portal) {
    this.siteType = portal.getName();
    siteResourcePath = "/site/" + this.siteType + "sites/";
    configurationPaths.add(SITES_CONFIGURATION_LOCATION.replace("WEB-INF", "war:") + this.siteType + SITES_CONFIGURATION_NAME);
  }

  /**
   * {@inheritDoc}
   */
  public boolean writeData(ZipOutputStream zos, Collection<String> selectedResources) {
    Set<String> filteredSelectedResources = filterSelectedResources(selectedResources, siteResourcePath);
    if (filteredSelectedResources.isEmpty()) {
      return false;
    }
    HashSet<String> siteNames = new HashSet<String>();
    try {
      for (String resourcePath : filteredSelectedResources) {
        siteNames.add(resourcePath.replace(siteResourcePath, ""));
        ZipFile zipFile = getExportedFileFromOperation(resourcePath);
        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        while (entries.hasMoreElements()) {
          ZipEntry zipEntry = (ZipEntry) entries.nextElement();
          try {
            InputStream inputStream = zipFile.getInputStream(zipEntry);
            Utils.writeZipEnry(zos, SITES_CONFIGURATION_LOCATION + zipEntry.getName(), inputStream);
          } catch (Exception e) {
            log.error("Error while serializing MOP data", e);
            return false;
          }
        }
      }
    } finally {
      clearTempFiles();
    }

    ExternalComponentPlugins externalComponentPlugins = new ExternalComponentPlugins();
    InitParams params = new InitParams();
    ObjectParameter objectParameter = new ObjectParameter();
    objectParameter.setName(siteType + ".configuration");
    NewPortalConfig portalConfig = new NewPortalConfig();
    portalConfig.setOwnerType(siteType);
    portalConfig.setTemplateLocation(SITES_CONFIGURATION_LOCATION.replace("WEB-INF", "war:"));
    portalConfig.setPredefinedOwner(siteNames);
    objectParameter.setObject(portalConfig);
    params.addParam(objectParameter);
    ComponentPlugin plugin = createComponentPlugin(siteType + ".config.user.listener", NewPortalConfigListener.class.getName(), "initListener", params);
    addComponentPlugin(externalComponentPlugins, UserPortalConfigService.class.getName(), plugin);
    return Utils.writeConfiguration(zos, SITES_CONFIGURATION_LOCATION + siteType + SITES_CONFIGURATION_NAME, externalComponentPlugins);
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
