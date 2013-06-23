package org.exoplatform.extension.generator.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipOutputStream;

import javax.inject.Singleton;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.xml.Configuration;
import org.exoplatform.extension.generator.service.api.ConfigurationHandler;
import org.exoplatform.extension.generator.service.api.ExtensionGenerator;
import org.exoplatform.extension.generator.service.api.Node;
import org.exoplatform.extension.generator.service.api.Utils;
import org.exoplatform.extension.generator.service.handler.ActionNodeTypeConfigurationHandler;
import org.exoplatform.extension.generator.service.handler.ApplicationRegistryConfigurationHandler;
import org.exoplatform.extension.generator.service.handler.CLVTemplatesConfigurationHandler;
import org.exoplatform.extension.generator.service.handler.DrivesConfigurationHandler;
import org.exoplatform.extension.generator.service.handler.JCRQueryConfigurationHandler;
import org.exoplatform.extension.generator.service.handler.MOPSiteConfigurationHandler;
import org.exoplatform.extension.generator.service.handler.MetadataTemplatesConfigurationHandler;
import org.exoplatform.extension.generator.service.handler.NodeTypeConfigurationHandler;
import org.exoplatform.extension.generator.service.handler.NodeTypeTemplatesConfigurationHandler;
import org.exoplatform.extension.generator.service.handler.ScriptsConfigurationHandler;
import org.exoplatform.extension.generator.service.handler.SearchTemplatesRegistryConfigurationHandler;
import org.exoplatform.extension.generator.service.handler.SiteContentsConfigurationHandler;
import org.exoplatform.extension.generator.service.handler.TaxonomyConfigurationHandler;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.gatein.management.api.ContentType;
import org.gatein.management.api.PathAddress;
import org.gatein.management.api.controller.ManagedRequest;
import org.gatein.management.api.controller.ManagedResponse;
import org.gatein.management.api.controller.ManagementController;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.model.ReadResourceModel;

@Singleton
public class ExtensionGeneratorImpl implements ExtensionGenerator {
  private static final String CONFIGURATION_XML_LOCATION = "WEB-INF/conf/configuration.xml";

  private Log log = ExoLogger.getLogger(this.getClass());

  private ManagementController managementController = null;

  private List<ConfigurationHandler> handlers = new ArrayList<ConfigurationHandler>();

  public ExtensionGeneratorImpl() {
    handlers.add(new ActionNodeTypeConfigurationHandler());
    handlers.add(new ApplicationRegistryConfigurationHandler());
    handlers.add(new CLVTemplatesConfigurationHandler());
    handlers.add(new DrivesConfigurationHandler());
    handlers.add(new JCRQueryConfigurationHandler());
    handlers.add(new MetadataTemplatesConfigurationHandler());
    handlers.add(new MOPSiteConfigurationHandler(SiteType.PORTAL));
    handlers.add(new MOPSiteConfigurationHandler(SiteType.GROUP));
    handlers.add(new MOPSiteConfigurationHandler(SiteType.USER));
    handlers.add(new NodeTypeConfigurationHandler());
    handlers.add(new NodeTypeTemplatesConfigurationHandler());
    handlers.add(new ScriptsConfigurationHandler());
    handlers.add(new SearchTemplatesRegistryConfigurationHandler());
    handlers.add(new SiteContentsConfigurationHandler());
    handlers.add(new TaxonomyConfigurationHandler());
  }

  @Override
  public Set<Node> getPortalSiteNodes() {
    return getNodes(SITES_PORTAL_PATH);
  }

  @Override
  public Set<Node> getGroupSiteNodes() {
    return getNodes(SITES_GROUP_PATH);
  }

  @Override
  public Set<Node> getUserSiteNodes() {
    return getNodes(SITES_USER_PATH);
  }

  @Override
  public Set<Node> getSiteContentNodes() {
    return getNodes(CONTENT_SITES_PATH);
  }

  @Override
  public Set<Node> getApplicationCLVTemplatesNodes() {
    return getNodes(ECM_TEMPLATES_APPLICATION_CLV_PATH);
  }

  @Override
  public Set<Node> getApplicationSearchTemplatesNodes() {
    return getNodes(ECM_TEMPLATES_APPLICATION_SEARCH_PATH);
  }

  @Override
  public Set<Node> getDocumentTypeTemplatesNodes() {
    return getNodes(ECM_TEMPLATES_DOCUMENT_TYPE_PATH);
  }

  @Override
  public Set<Node> getMetadataTemplatesNodes() {
    return getNodes(ECM_TEMPLATES_METADATA_PATH);
  }

  @Override
  public Set<Node> getTaxonomyNodes() {
    return getNodes(ECM_TAXONOMY_PATH);
  }

  @Override
  public Set<Node> getQueryNodes() {
    return getNodes(ECM_QUERY_PATH);
  }

  @Override
  public Set<Node> getDriveNodes() {
    return getNodes(ECM_DRIVE_PATH);
  }

  @Override
  public Set<Node> getScriptNodes() {
    return getNodes(ECM_SCRIPT_PATH);
  }

  @Override
  public Set<Node> getActionNodeTypeNodes() {
    return getNodes(ECM_ACTION_PATH);
  }

  @Override
  public Set<Node> getNodeTypeNodes() {
    return getNodes(ECM_NODETYPE_PATH);
  }

  @Override
  public Set<Node> getRegistryNodes() {
    return getNodes(REGISTRY_PATH);
  }

  private Set<Node> getNodes(String path) {
    ManagedRequest request = ManagedRequest.Factory.create(OperationNames.READ_RESOURCE, PathAddress.pathAddress(path), ContentType.JSON);
    ManagedResponse response = getManagementController().execute(request);
    if (!response.getOutcome().isSuccess()) {
      log.error(response.getOutcome().getFailureDescription());
      throw new RuntimeException(response.getOutcome().getFailureDescription());
    }
    ReadResourceModel result = (ReadResourceModel) response.getResult();
    Set<Node> children = new HashSet<Node>(result.getChildren().size());
    if (result.getChildren() != null && !result.getChildren().isEmpty()) {
      for (String childName : result.getChildren()) {
        String description = result.getChildDescription(childName).getDescription();
        String childPath = path + "/" + childName;
        Node child = new Node(childName, description, childPath);
        children.add(child);
      }
    } else {
      Node parent = new Node(path, result.getDescription(), path);
      children.add(parent);
    }
    return children;
  }

  @Override
  public InputStream generateWARExtension(Set<String> selectedResources) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ZipOutputStream zos = new ZipOutputStream(out);

    Configuration configuration = new Configuration();
    for (ConfigurationHandler configurationHandler : handlers) {
      boolean extracted = configurationHandler.writeData(zos, selectedResources);
      if (extracted) {
        List<String> configurationPaths = configurationHandler.getConfigurationPaths();
        if (configurationPaths != null) {
          for (String path : configurationPaths) {
            configuration.addImport(path);
          }
        }
      }
    }
    Utils.writeConfiguration(zos, CONFIGURATION_XML_LOCATION, configuration);

    try {
      zos.flush();
      zos.close();
    } catch (IOException e) {
      log.error("Error while closing ZipOutputStream.");
    }
    return new ByteArrayInputStream(out.toByteArray());
  }

  private ManagementController getManagementController() {
    if (managementController == null) {
      managementController = (ManagementController) PortalContainer.getInstance().getComponentInstanceOfType(ManagementController.class);
    }
    return managementController;
  }

}