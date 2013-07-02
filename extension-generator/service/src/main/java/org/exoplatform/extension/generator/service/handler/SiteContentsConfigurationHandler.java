package org.exoplatform.extension.generator.service.handler;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.exoplatform.container.xml.ComponentPlugin;
import org.exoplatform.container.xml.ExternalComponentPlugins;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ObjectParameter;
import org.exoplatform.container.xml.ValuesParam;
import org.exoplatform.extension.generator.service.api.AbstractConfigurationHandler;
import org.exoplatform.extension.generator.service.api.ExtensionGenerator;
import org.exoplatform.extension.generator.service.api.Utils;
import org.exoplatform.management.content.operations.site.SiteConstants;
import org.exoplatform.management.content.operations.site.contents.SiteData;
import org.exoplatform.management.content.operations.site.contents.SiteMetaData;
import org.exoplatform.services.deployment.DeploymentDescriptor;
import org.exoplatform.services.deployment.DeploymentDescriptor.Target;
import org.exoplatform.services.deployment.WCMContentInitializerService;
import org.exoplatform.services.deployment.plugins.XMLDeploymentPlugin;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.portal.artifacts.CreatePortalArtifactsService;
import org.exoplatform.services.wcm.portal.artifacts.IgnorePortalPlugin;

import com.thoughtworks.xstream.XStream;

public class SiteContentsConfigurationHandler extends AbstractConfigurationHandler {
  private static final String WCM_CONTENT_CONFIGURATION_LOCATION = "WEB-INF/conf/custom-extension/wcm/content/";
  private static final String WCM_CONTENT_CONFIGURATION_NAME = "content-artifacts-deployment-configuration.xml";
  private static final List<String> configurationPaths = new ArrayList<String>();
  static {
    configurationPaths.add(WCM_CONTENT_CONFIGURATION_LOCATION.replace("WEB-INF", "war:") + WCM_CONTENT_CONFIGURATION_NAME);
  }

  private Log log = ExoLogger.getLogger(this.getClass());

  /**
   * {@inheritDoc}
   */
  public boolean writeData(ZipOutputStream zos, Collection<String> selectedResources) {
    Set<String> filteredSelectedResources = filterSelectedResources(selectedResources, ExtensionGenerator.CONTENT_SITES_PATH);
    if (filteredSelectedResources.isEmpty()) {
      return false;
    }

    Map<String, SiteData> sitesData = new HashMap<String, SiteData>();
    try {
      for (String filteredResource : filteredSelectedResources) {
        String[] filters = new String[3];
        filters[0] = "no-skeleton:true";
        filters[1] = "taxonomy:false";
        filters[2] = "version-hitory:true";
        ZipFile zipFile = getExportedFileFromOperation(filteredResource, filters);

        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        while (entries.hasMoreElements()) {
          ZipEntry zipEntry = (ZipEntry) entries.nextElement();
          try {
            InputStream inputStream = zipFile.getInputStream(zipEntry);
            String siteName = extractSiteNameFromPath(zipEntry.getName());
            if (zipEntry.getName().endsWith("metadata.xml")) {
              // Unmarshall metadata xml file
              XStream xstream = new XStream();
              xstream.alias("metadata", SiteMetaData.class);
              InputStreamReader isr = new InputStreamReader(inputStream, "UTF-8");
              SiteMetaData siteMetadata = (SiteMetaData) xstream.fromXML(isr);

              // Save unmarshalled metadata
              SiteData siteData = sitesData.get(siteName);
              if (siteData == null) {
                siteData = new SiteData();
              }
              siteData.setSiteMetadata(siteMetadata);
              sitesData.put(siteName, siteData);
            } else if (zipEntry.getName().endsWith("seo.xml")) {
              continue;
            } else {
              String location = zipEntry.getName();
              location = location.substring(location.lastIndexOf("/" + siteName + "/") + 1);
              Utils.writeZipEnry(zos, WCM_CONTENT_CONFIGURATION_LOCATION + location, inputStream);
            }
          } catch (Exception e) {
            log.error("Exception while writing Data", e);
            return false;
          }
        }
      }
    } finally {
      clearTempFiles();
    }

    ExternalComponentPlugins ignoreContentComponentPlugin = new ExternalComponentPlugins();
    {
      InitParams params = new InitParams();
      ValuesParam valuesParam = new ValuesParam();
      valuesParam.setName("autoCreatedInNewRepository");
      ArrayList<String> ignoredSitesList = new ArrayList<String>(sitesData.keySet());
      ignoredSitesList.remove("shared");
      valuesParam.setValues(ignoredSitesList);
      params.addParam(valuesParam);
      ComponentPlugin plugin = createComponentPlugin("Add as ignored portal", IgnorePortalPlugin.class.getName(), "addIgnorePortalPlugin", params);
      addComponentPlugin(ignoreContentComponentPlugin, CreatePortalArtifactsService.class.getName(), plugin);
    }

    ExternalComponentPlugins contentExternalComponentPlugins = new ExternalComponentPlugins();
    Set<Entry<String, SiteData>> sitesDataSet = sitesData.entrySet();
    for (Entry<String, SiteData> siteDataEntry : sitesDataSet) {
      InitParams params = new InitParams();
      ComponentPlugin plugin = createComponentPlugin(siteDataEntry.getKey() + " Content Initializer Service", XMLDeploymentPlugin.class.getName(), "addPlugin", params);
      addComponentPlugin(contentExternalComponentPlugins, WCMContentInitializerService.class.getName(), plugin);

      SiteData siteData = siteDataEntry.getValue();
      String siteName = siteData.getSiteMetadata().getOptions().get("site-name");

      Set<Map.Entry<String, String>> exportedFilesEntrySet = siteData.getSiteMetadata().getExportedFiles().entrySet();
      for (Entry<String, String> exportedFileEntry : exportedFilesEntrySet) {
        DeploymentDescriptor deploymentDescriptor = new DeploymentDescriptor();
        deploymentDescriptor.setCleanupPublication(false);
        String location = exportedFileEntry.getKey();
        location = location.substring(location.lastIndexOf("/" + siteName + "/") + 1);
        String xmlLocation = WCM_CONTENT_CONFIGURATION_LOCATION.replace("WEB-INF", "war:") + location;

        // Replace ".xml" by "_VersionHistory.zip"
        String versionHistoryLocation = xmlLocation.substring(0, xmlLocation.length() - 4) + "_VersionHistory.zip";

        deploymentDescriptor.setSourcePath(xmlLocation);
        deploymentDescriptor.setVersionHistoryPath(versionHistoryLocation);

        Target target = new Target();
        target.setWorkspace(siteData.getSiteMetadata().getOptions().get("site-workspace"));
        target.setNodePath(exportedFileEntry.getValue());
        deploymentDescriptor.setTarget(target);

        ObjectParameter objectParameter = new ObjectParameter();
        objectParameter.setName(location);
        objectParameter.setObject(deploymentDescriptor);
        params.addParam(objectParameter);
      }
    }
    return Utils.writeConfiguration(zos, WCM_CONTENT_CONFIGURATION_LOCATION + WCM_CONTENT_CONFIGURATION_NAME, contentExternalComponentPlugins, ignoreContentComponentPlugin);
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

  protected String extractSiteNameFromPath(String path) {
    String siteName = null;

    int beginIndex = SiteConstants.SITE_CONTENTS_ROOT_PATH.length() + 1;
    siteName = path.substring(beginIndex, path.indexOf("/", beginIndex));

    return siteName;
  }
}
