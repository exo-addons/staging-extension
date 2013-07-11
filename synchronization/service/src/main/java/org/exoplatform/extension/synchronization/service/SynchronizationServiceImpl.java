package org.exoplatform.extension.synchronization.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Singleton;
import javax.jcr.NodeIterator;
import javax.jcr.Session;
import javax.jcr.query.Query;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.extension.synchronization.service.api.Node;
import org.exoplatform.extension.synchronization.service.api.ResourceHandler;
import org.exoplatform.extension.synchronization.service.api.SynchronizationService;
import org.exoplatform.extension.synchronization.service.handler.ActionNodeTypeHandler;
import org.exoplatform.extension.synchronization.service.handler.ApplicationRegistryHandler;
import org.exoplatform.extension.synchronization.service.handler.CLVTemplatesHandler;
import org.exoplatform.extension.synchronization.service.handler.DrivesHandler;
import org.exoplatform.extension.synchronization.service.handler.JCRQueryHandler;
import org.exoplatform.extension.synchronization.service.handler.MOPSiteHandler;
import org.exoplatform.extension.synchronization.service.handler.MetadataTemplatesHandler;
import org.exoplatform.extension.synchronization.service.handler.NodeTypeHandler;
import org.exoplatform.extension.synchronization.service.handler.NodeTypeTemplatesHandler;
import org.exoplatform.extension.synchronization.service.handler.ScriptsHandler;
import org.exoplatform.extension.synchronization.service.handler.SearchTemplatesHandler;
import org.exoplatform.extension.synchronization.service.handler.SiteContentsHandler;
import org.exoplatform.extension.synchronization.service.handler.SiteExplorerTemplatesHandler;
import org.exoplatform.extension.synchronization.service.handler.SiteExplorerViewHandler;
import org.exoplatform.extension.synchronization.service.handler.TaxonomyHandler;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.services.wcm.core.WCMConfigurationService;
import org.gatein.management.api.ContentType;
import org.gatein.management.api.PathAddress;
import org.gatein.management.api.controller.ManagedRequest;
import org.gatein.management.api.controller.ManagedResponse;
import org.gatein.management.api.controller.ManagementController;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.model.ReadResourceModel;

@Singleton
public class SynchronizationServiceImpl implements SynchronizationService {

  private Log log = ExoLogger.getLogger(this.getClass());

  private ManagementController managementController = null;
  private WCMConfigurationService wcmConfigurationService = null;
  private RepositoryService repositoryService = null;

  private List<ResourceHandler> handlers = new ArrayList<ResourceHandler>();

  public SynchronizationServiceImpl() {
    handlers.add(new ActionNodeTypeHandler());
    handlers.add(new NodeTypeHandler());
    handlers.add(new ApplicationRegistryHandler());
    handlers.add(new MOPSiteHandler(SiteType.PORTAL));
    handlers.add(new MOPSiteHandler(SiteType.GROUP));
    handlers.add(new MOPSiteHandler(SiteType.USER));
    handlers.add(new ScriptsHandler());
    handlers.add(new DrivesHandler());
    handlers.add(new JCRQueryHandler());
    handlers.add(new MetadataTemplatesHandler());
    handlers.add(new NodeTypeTemplatesHandler());
    handlers.add(new SiteContentsHandler());
    handlers.add(new SearchTemplatesHandler());
    handlers.add(new CLVTemplatesHandler());
    handlers.add(new TaxonomyHandler());
    handlers.add(new SiteExplorerTemplatesHandler());
    handlers.add(new SiteExplorerViewHandler());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<Node> getPortalSiteNodes() {
    return getNodes(SITES_PORTAL_PATH);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<Node> getGroupSiteNodes() {
    return getNodes(SITES_GROUP_PATH);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<Node> getUserSiteNodes() {
    return getNodes(SITES_USER_PATH);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<Node> getSiteContentNodes() {
    return getNodes(CONTENT_SITES_PATH);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<Node> getApplicationCLVTemplatesNodes() {
    return getNodes(ECM_TEMPLATES_APPLICATION_CLV_PATH);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<Node> getApplicationSearchTemplatesNodes() {
    return getNodes(ECM_TEMPLATES_APPLICATION_SEARCH_PATH);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<Node> getDocumentTypeTemplatesNodes() {
    return getNodes(ECM_TEMPLATES_DOCUMENT_TYPE_PATH);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<Node> getMetadataTemplatesNodes() {
    return getNodes(ECM_TEMPLATES_METADATA_PATH);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<Node> getTaxonomyNodes() {
    return getNodes(ECM_TAXONOMY_PATH);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<Node> getQueryNodes() {
    return getNodes(ECM_QUERY_PATH);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<Node> getDriveNodes() {
    return getNodes(ECM_DRIVE_PATH);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<Node> getScriptNodes() {
    return getNodes(ECM_SCRIPT_PATH);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<Node> getActionNodeTypeNodes() {
    return getNodes(ECM_ACTION_PATH);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<Node> getNodeTypeNodes() {
    return getNodes(ECM_NODETYPE_PATH);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<Node> getRegistryNodes() {
    return getNodes(REGISTRY_PATH);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<Node> getViewTemplatesNodes() {
    return getNodes(ECM_VIEW_TEMPLATES_PATH);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<Node> getViewConfigurationNodes() {
    return getNodes(ECM_VIEW_CONFIGURATION_PATH);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void synchronize(Set<String> selectedResources, Map<String, String> options, String isSSLString, String host, String port, String username, String password) throws IOException {
    boolean isSSL = false;
    if (isSSLString != null && isSSLString.equals("true")) {
      isSSL = true;
    }
    for (ResourceHandler handler : handlers) {
      handler.synchronizeData(selectedResources, isSSL, host, port, username, password, options);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<String> executeSQL(String sql, Set<String> selectedResources) throws Exception {
    NodeLocation sitesLocation = getWCMConfigurationService().getLivePortalsLocation();
    Set<String> sites = filterSelectedResources(selectedResources, CONTENT_SITES_PATH);
    Set<String> paths = new HashSet<String>();
    for (String sitePath : sites) {
      String realSQL = sql;
      sitePath = sitesLocation.getPath() + sitePath.replace(CONTENT_SITES_PATH, "") + "/";
      sitePath = sitePath.replaceAll("//", "/");
      String queryPath = "jcr:path = '" + sitePath + "%'";
      if (realSQL.contains("where")) {
        int startIndex = realSQL.indexOf("where");
        int endIndex = startIndex + "where".length();

        String condition = realSQL.substring(endIndex);
        condition = queryPath + " AND (" + condition + ")";

        realSQL = realSQL.substring(0, startIndex) + " where " + condition;
      } else {
        realSQL += " where " + queryPath;
      }

      SessionProvider provider = SessionProvider.createSystemProvider();
      ManageableRepository repository = getRepositoryService().getCurrentRepository();
      Session session = provider.getSession(sitesLocation.getWorkspace(), repository);

      Query query = session.getWorkspace().getQueryManager().createQuery(realSQL, Query.SQL);
      NodeIterator nodeIterator = query.execute().getNodes();
      while (nodeIterator.hasNext()) {
        javax.jcr.Node node = nodeIterator.nextNode();
        paths.add(node.getPath());
      }
    }
    return paths;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<String> filterSelectedResources(Collection<String> selectedResources, String parentPath) {
    Set<String> filteredSelectedResources = new HashSet<String>();
    for (String resourcePath : selectedResources) {
      if (resourcePath.contains(parentPath)) {
        filteredSelectedResources.add(resourcePath);
      }
    }
    return filteredSelectedResources;
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

  private ManagementController getManagementController() {
    if (managementController == null) {
      managementController = (ManagementController) PortalContainer.getInstance().getComponentInstanceOfType(ManagementController.class);
    }
    return managementController;
  }

  private WCMConfigurationService getWCMConfigurationService() {
    if (wcmConfigurationService == null) {
      wcmConfigurationService = (WCMConfigurationService) PortalContainer.getInstance().getComponentInstanceOfType(WCMConfigurationService.class);
    }
    return wcmConfigurationService;
  }

  private RepositoryService getRepositoryService() {
    if (repositoryService == null) {
      repositoryService = (RepositoryService) PortalContainer.getInstance().getComponentInstanceOfType(RepositoryService.class);
    }
    return repositoryService;
  }
}