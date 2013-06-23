package org.exoplatform.extension.generator.service.handler;

import java.io.InputStream;
import java.io.InputStreamReader;
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
import org.exoplatform.management.ecmadmin.operations.templates.NodeTemplate;
import org.exoplatform.management.ecmadmin.operations.templates.nodetypes.NodeTypeTemplatesMetaData;
import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.services.cms.templates.impl.TemplateConfig;
import org.exoplatform.services.cms.templates.impl.TemplateConfig.NodeType;
import org.exoplatform.services.cms.templates.impl.TemplatePlugin;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import com.thoughtworks.xstream.XStream;

public class NodeTypeTemplatesConfigurationHandler extends AbstractConfigurationHandler {
  private static final String DOCUMENT_TYPE_CONFIGURATION_LOCATION = DMS_CONFIGURATION_LOCATION + "templates/nodetypes";
  private static final String DOCUMENT_TYPE_CONFIGURATION_NAME = "nodetype-templates-configuration.xml";
  private Log log = ExoLogger.getLogger(this.getClass());
  private static final List<String> configurationPaths = new ArrayList<String>();
  static {
    configurationPaths.add(DMS_CONFIGURATION_LOCATION.replace("WEB-INF", "war:") + DOCUMENT_TYPE_CONFIGURATION_NAME);
  }

  public boolean writeData(ZipOutputStream zos, Set<String> selectedResources) {
    Set<String> filteredSelectedResources = filterSelectedResources(selectedResources, ExtensionGenerator.ECM_TEMPLATES_DOCUMENT_TYPE_PATH);
    if (filteredSelectedResources.isEmpty()) {
      return false;
    }
    List<String> filterNodeTypes = new ArrayList<String>();
    for (String resourcePath : filteredSelectedResources) {
      String nodeTypeName = resourcePath.replace(ExtensionGenerator.ECM_TEMPLATES_DOCUMENT_TYPE_PATH + "/", "");
      filterNodeTypes.add(nodeTypeName);
    }
    ZipFile zipFile = getExportedFileFromOperation(ExtensionGenerator.ECM_TEMPLATES_DOCUMENT_TYPE_PATH, filterNodeTypes.toArray(new String[0]));
    List<NodeTypeTemplatesMetaData> metaDatas = new ArrayList<NodeTypeTemplatesMetaData>();
    Enumeration<? extends ZipEntry> entries = zipFile.entries();
    while (entries.hasMoreElements()) {
      ZipEntry zipEntry = (ZipEntry) entries.nextElement();
      try {
        InputStream inputStream = zipFile.getInputStream(zipEntry);
        if (zipEntry.getName().endsWith("metadata.xml")) {
          XStream xStream = new XStream();
          xStream.alias("metadata", NodeTypeTemplatesMetaData.class);
          xStream.alias("template", NodeTemplate.class);
          NodeTypeTemplatesMetaData metadata = (NodeTypeTemplatesMetaData) xStream.fromXML(new InputStreamReader(inputStream));
          metaDatas.add(metadata);
        } else {
          Utils.writeZipEnry(zos, DMS_CONFIGURATION_LOCATION + zipEntry.getName(), inputStream);
        }
      } catch (Exception e) {
        log.error("Error while getting NamespaceConfiguration Entry", e);
        return false;
      }
    }

    ExternalComponentPlugins externalComponentPlugins = new ExternalComponentPlugins();
    List<NodeType> nodeTypes = new ArrayList<NodeType>();
    {
      InitParams params = new InitParams();
      params.addParam(getValueParam("autoCreateInNewRepository", "true"));
      params.addParam(getValueParam("storedLocation", DOCUMENT_TYPE_CONFIGURATION_LOCATION.replace("WEB-INF", "war:")));
      ObjectParameter objectParameter = new ObjectParameter();
      objectParameter.setName("template.configuration");
      TemplateConfig templateConfig = new TemplateConfig();
      templateConfig.setNodeTypes(nodeTypes);
      objectParameter.setObject(templateConfig);
      params.addParam(objectParameter);

      ComponentPlugin plugin = createComponentPlugin("addPlugins", TemplatePlugin.class.getName(), "addPlugins", params);
      addComponentPlugin(externalComponentPlugins, TemplateService.class.getName(), plugin);
    }

    for (NodeTypeTemplatesMetaData nodeTypeTemplatesMetaData : metaDatas) {
      NodeType nodeType = new NodeType();
      nodeType.setDocumentTemplate(nodeTypeTemplatesMetaData.isDocumentTemplate());
      nodeType.setLabel(nodeTypeTemplatesMetaData.getLabel());
      nodeType.setNodetypeName(nodeTypeTemplatesMetaData.getNodeTypeName());
      nodeType.setReferencedDialog(nodeTypeTemplatesMetaData.getTemplates().get("dialogs"));
      nodeType.setReferencedView(nodeTypeTemplatesMetaData.getTemplates().get("views"));
      nodeType.setReferencedSkin(nodeTypeTemplatesMetaData.getTemplates().get("skins"));
      nodeTypes.add(nodeType);
    }
    return Utils.writeConfiguration(zos, DMS_CONFIGURATION_LOCATION + DOCUMENT_TYPE_CONFIGURATION_NAME, externalComponentPlugins);
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
