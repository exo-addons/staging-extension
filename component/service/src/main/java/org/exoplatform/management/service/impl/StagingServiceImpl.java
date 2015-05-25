package org.exoplatform.management.service.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipOutputStream;

import javax.inject.Singleton;
import javax.jcr.NodeIterator;
import javax.jcr.Session;
import javax.jcr.query.Query;

import org.apache.commons.fileupload.FileItem;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.management.service.api.Resource;
import org.exoplatform.management.service.api.ResourceCategory;
import org.exoplatform.management.service.api.ResourceHandler;
import org.exoplatform.management.service.api.StagingService;
import org.exoplatform.management.service.handler.ResourceHandlerLocator;
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
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.model.ReadResourceModel;

/**
 * Staging service
 */
@Singleton
public class StagingServiceImpl implements StagingService {

  private Log log = ExoLogger.getLogger(StagingServiceImpl.class);

  private ManagementController managementController = null;
  private WCMConfigurationService wcmConfigurationService = null;
  private RepositoryService repositoryService = null;

  public StagingServiceImpl(ManagementController managementController, WCMConfigurationService wcmConfigurationService, RepositoryService repositoryService) {
    this.managementController = managementController;
    this.wcmConfigurationService = wcmConfigurationService;
    this.repositoryService = repositoryService;
  }

  /**
   * Export selected resources with selected options.
   * 
   * @param resourcesPaths
   * @param exportOptions
   * @return
   * @throws Exception
   */
  public File export(List<ResourceCategory> selectedResourceCategories) throws Exception {

    File file = null;
    ZipOutputStream exportFileOS = null;
    try {
      file = File.createTempFile("staging", "-export.zip");
      file.deleteOnExit();
      // FileOutputStream will be closed by ZipOutputStream.close
      exportFileOS = new ZipOutputStream(new FileOutputStream(file));
    } catch (Exception ex) {
      throw new OperationException(OperationNames.EXPORT_RESOURCE, "Error while creating a zip temp file to export resources", ex);
    }

    for (ResourceCategory selectedResourceCategory : selectedResourceCategories) {
      // Gets the right resource handler thanks to the Service Locator
      ResourceHandler resourceHandler = ResourceHandlerLocator.getResourceHandler(selectedResourceCategory.getPath());
      resourceHandler.export(selectedResourceCategory.getResources(), exportFileOS, selectedResourceCategory.getExportOptions());
    }

    try {
      exportFileOS.flush();
      exportFileOS.close();
    } catch (Exception ex) {
      throw new OperationException(OperationNames.EXPORT_RESOURCE, "Error while closing exported zip temp file." + file.getPath(), ex);
    }
    return file;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void importResource(String selectedResourcePath, FileItem file, Map<String, List<String>> attributes) throws IOException {
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
  public Set<String> executeSQL(String sql, Set<String> sites) throws Exception {
    NodeLocation sitesLocation = getWCMConfigurationService().getLivePortalsLocation();
    Set<String> paths = new HashSet<String>();
    for (String sitePath : sites) {
      String realSQL = sql;
      if (!sql.contains("jcr:path")) {
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
      if (sql.contains("jcr:path")) {
        break;
      }
    }
    return paths;
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
    if (result.getChildren() != null) {
      for (String childName : result.getChildren()) {
        String description = result.getChildDescription(childName).getDescription();
        String childPath = path + "/" + childName;
        Resource child = new Resource(childPath, childName, description);
        children.add(child);
      }
    }
    return children;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<Resource> getWikiPortalResources() {
    return getResources(PORTAL_WIKIS_PATH);
  }

  @Override
  public Set<Resource> getWikiGroupResources() {
    return getResources(GROUP_WIKIS_PATH);
  }

  @Override
  public Set<Resource> getWikiUserResources() {
    return getResources(USER_WIKIS_PATH);
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