package org.exoplatform.management.service.impl;

import org.apache.commons.fileupload.FileItem;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.management.service.api.Resource;
import org.exoplatform.management.service.api.StagingService;
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

import javax.inject.Singleton;
import javax.jcr.NodeIterator;
import javax.jcr.Session;
import javax.jcr.query.Query;
import java.io.IOException;
import java.util.*;

/**
 * Staging service
 */
@Singleton
public class StagingServiceImpl implements StagingService {

  private Log log = ExoLogger.getLogger(StagingServiceImpl.class);

  private ManagementController managementController = null;
  private WCMConfigurationService wcmConfigurationService = null;
  private RepositoryService repositoryService = null;

  public StagingServiceImpl() {
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void importResource(String selectedResourcePath, FileItem file) throws IOException {
    Map<String, List<String>> attributes = new HashMap<String, List<String>>(0);
    ManagedRequest request = ManagedRequest.Factory.create(OperationNames.IMPORT_RESOURCE, PathAddress.pathAddress(selectedResourcePath), attributes, file.getInputStream(), ContentType.ZIP);

    ManagedResponse response = managementController.execute(request);
    if (!response.getOutcome().isSuccess()) {
      throw new RuntimeException(response.getOutcome().getFailureDescription());
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

  public Set<Resource> getResources(String path) {
    ManagedRequest request = ManagedRequest.Factory.create(OperationNames.READ_RESOURCE, PathAddress.pathAddress(path), ContentType.JSON);
    ManagedResponse response = getManagementController().execute(request);
    if (!response.getOutcome().isSuccess()) {
      log.error(response.getOutcome().getFailureDescription());
      throw new RuntimeException(response.getOutcome().getFailureDescription());
    }
    ReadResourceModel result = (ReadResourceModel) response.getResult();
    Set<Resource> children = new HashSet<Resource>(result.getChildren().size());
    if (result.getChildren() != null && !result.getChildren().isEmpty()) {
      for (String childName : result.getChildren()) {
        String description = result.getChildDescription(childName).getDescription();
        String childPath = path + "/" + childName;
        Resource child = new Resource(childName, description, childPath);
        children.add(child);
      }
    } else {
      Resource parent = new Resource(path, result.getDescription(), path);
      children.add(parent);
    }
    return children;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<Resource> getPortalSiteResources() {
    return getResources(SITES_PORTAL_PATH);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<Resource> getGroupSiteResources() {
    return getResources(SITES_GROUP_PATH);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<Resource> getUserSiteResources() {
    return getResources(SITES_USER_PATH);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<Resource> getSiteContentResources() {
    return getResources(CONTENT_SITES_PATH);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<Resource> getApplicationCLVTemplatesResources() {
    return getResources(ECM_TEMPLATES_APPLICATION_CLV_PATH);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<Resource> getApplicationSearchTemplatesResources() {
    return getResources(ECM_TEMPLATES_APPLICATION_SEARCH_PATH);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<Resource> getDocumentTypeTemplatesResources() {
    return getResources(ECM_TEMPLATES_DOCUMENT_TYPE_PATH);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<Resource> getMetadataTemplatesResources() {
    return getResources(ECM_TEMPLATES_METADATA_PATH);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<Resource> getTaxonomyResources() {
    return getResources(ECM_TAXONOMY_PATH);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<Resource> getQueryResources() {
    return getResources(ECM_QUERY_PATH);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<Resource> getDriveResources() {
    return getResources(ECM_DRIVE_PATH);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<Resource> getScriptResources() {
    return getResources(ECM_SCRIPT_PATH);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<Resource> getActionNodeTypeResources() {
    return getResources(ECM_ACTION_PATH);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<Resource> getNodeTypeResources() {
    return getResources(ECM_NODETYPE_PATH);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<Resource> getRegistryResources() {
    return getResources(REGISTRY_PATH);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<Resource> getViewTemplatesResources() {
    return getResources(ECM_VIEW_TEMPLATES_PATH);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<Resource> getViewConfigurationResources() {
    return getResources(ECM_VIEW_CONFIGURATION_PATH);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<Resource> getGadgetResources() {
    return getResources(GADGET_PATH);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<Resource> getUserResources() {
    return getResources(USERS_PATH);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<Resource> getGroupResources() {
    return getResources(GROUPS_PATH);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<Resource> getRoleResources() {
    return getResources(ROLE_PATH);
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