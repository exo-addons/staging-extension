/*
 * Copyright (C) 2003-2017 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.exoplatform.management.content.operations.site.contents;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.commons.utils.ActivityTypeUtils;
import org.exoplatform.management.common.DataTransformerService;
import org.exoplatform.management.common.exportop.AbstractJCRExportOperationHandler;
import org.exoplatform.management.common.exportop.ActivitiesExportTask;
import org.exoplatform.management.common.exportop.JCRNodeExportTask;
import org.exoplatform.management.content.operations.site.SiteUtil;
import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.services.wcm.core.WCMConfigurationService;
import org.exoplatform.services.wcm.portal.PortalFolderSchemaHandler;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.manager.ActivityManager;
import org.exoplatform.social.core.manager.IdentityManager;
import org.gatein.management.api.PathAddress;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.operation.OperationAttributes;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.ExportResourceModel;
import org.gatein.management.api.operation.model.ExportTask;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;

/**
 * The Class SiteContentsExportResource.
 *
 * @author <a href="mailto:thomas.delhomenie@exoplatform.com">Thomas
 *         Delhoménie</a>
 * @author <a href="mailto:boubaker.khanfir@exoplatform.com">Boubaker
 *         Khanfir</a>
 * @version $Revision$
 */
public class SiteContentsExportResource extends AbstractJCRExportOperationHandler {
  
  /** The Constant log. */
  private static final Log log = ExoLogger.getLogger(SiteContentsExportResource.class);

  /** The Constant FILTER_SEPARATOR. */
  public static final String FILTER_SEPARATOR = ":";

  /** The wcm configuration service. */
  private WCMConfigurationService wcmConfigurationService = null;

  /**
   * {@inheritDoc}
   */
  @Override
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws OperationException {
    increaseCurrentTransactionTimeOut(operationContext);
    try {
      String operationName = operationContext.getOperationName();
      PathAddress address = operationContext.getAddress();
      OperationAttributes attributes = operationContext.getAttributes();

      String siteName = address.resolvePathTemplate("site-name");
      if (siteName == null) {
        throw new OperationException(operationName, "No site name specified.");
      }

      repositoryService = operationContext.getRuntimeContext().getRuntimeComponent(RepositoryService.class);
      templateService = operationContext.getRuntimeContext().getRuntimeComponent(TemplateService.class);
      wcmConfigurationService = operationContext.getRuntimeContext().getRuntimeComponent(WCMConfigurationService.class);
      identityManager = operationContext.getRuntimeContext().getRuntimeComponent(IdentityManager.class);
      activityManager = operationContext.getRuntimeContext().getRuntimeComponent(ActivityManager.class);

      List<ExportTask> exportTasks = new ArrayList<ExportTask>();
      List<String> excludePaths = attributes.getValues("excludePaths");
      List<String> filters = attributes.getValues("filter");

      NodeLocation sitesLocation = wcmConfigurationService.getLivePortalsLocation();

      // "taxonomy" attribute. Defaults to true.
      boolean exportSiteTaxonomy = !filters.contains("taxonomy:false");
      // "no-history" attribute. Defaults to false.
      boolean exportVersionHistory = !filters.contains("no-history:true");
      // Exports only metadata
      boolean exportOnlyMetadata = filters.contains("only-metadata:true");
      // Exports with skeleton
      boolean exportSiteWithSkeleton = !filters.contains("no-skeleton:true");
      // get JCR Query
      String jcrQuery = getParameterValue(filters, "query:", null);
      // get JCR workspace
      String workspace = getParameterValue(filters, "workspace:", sitesLocation.getWorkspace());
      // remove JCR Nodes
      String removeNodes = getParameterValue(filters, "removeNodes:", null);

      // get JCR Site path
      String sitePath = (sitesLocation.getPath().endsWith("/") ? sitesLocation.getPath() : (sitesLocation.getPath() + "/")) + siteName;

      SiteMetaData metaData = new SiteMetaData();
      metaData.getOptions().put(SiteMetaData.SITE_PATH, sitePath);
      metaData.getOptions().put(SiteMetaData.SITE_WORKSPACE, workspace);
      metaData.getOptions().put(SiteMetaData.SITE_NAME, siteName);
      if (!StringUtils.isEmpty(removeNodes)) {
        metaData.getOptions().put("removeNodes", removeNodes);
      } else {
        Set<String> activitiesId = new HashSet<String>();
        // Site contents
        if (!StringUtils.isEmpty(jcrQuery)) {
          exportTasks.addAll(exportQueryResult(workspace, sitePath, jcrQuery, excludePaths, exportVersionHistory, metaData, activitiesId, exportOnlyMetadata));
        } else if (exportSiteWithSkeleton) {
          exportTasks.addAll(exportSite(workspace, sitePath, exportVersionHistory, metaData, activitiesId, exportOnlyMetadata));
        } else {
          exportTasks.addAll(exportSiteWithoutSkeleton(workspace, sitePath, exportSiteTaxonomy, exportVersionHistory, metaData, activitiesId, exportOnlyMetadata));
        }

        if (!exportOnlyMetadata) {
          // Export activities
          exportActivities(exportTasks, activitiesId, siteName);
        }
      }

      // Export Site Metadata
      exportTasks.add(new SiteMetaDataExportTask(metaData));

      resultHandler.completed(new ExportResourceModel(exportTasks));
    } catch (Exception e) {
      throw new OperationException(OperationNames.EXPORT_RESOURCE, "Unable to export site contents, cause : " + e.getMessage(), e);
    } finally {
      restoreDefaultTransactionTimeOut(operationContext);
    }
  }

  /**
   * Export activities.
   *
   * @param exportTasks the export tasks
   * @param activitiesId the activities id
   * @param siteName the site name
   */
  private void exportActivities(List<ExportTask> exportTasks, Set<String> activitiesId, String siteName) {
    // In case of minimal profile
    if (activityManager != null) {
      try {
        List<ExoSocialActivity> activities = new ArrayList<ExoSocialActivity>();
        for (String activityId : activitiesId) {
          addActivityWithComments(activities, activityId);
        }
        if (!activities.isEmpty()) {
          exportTasks.add(new ActivitiesExportTask(identityManager, activities, SiteUtil.getSiteContentsBasePath(siteName)));
        }
      } catch (Exception e) {
        log.warn("Can't export activities", e);
      }
    }
  }

  /**
   * Export site.
   *
   * @param workspace the workspace
   * @param siteRootNodePath the site root node path
   * @param exportVersionHistory the export version history
   * @param metaData the meta data
   * @param activitiesId the activities id
   * @param exportOnlyMetadata the export only metadata
   * @return the list
   * @throws Exception the exception
   */
  private List<ExportTask> exportSite(String workspace, String siteRootNodePath, boolean exportVersionHistory, SiteMetaData metaData, Set<String> activitiesId, boolean exportOnlyMetadata)
      throws Exception {
    List<ExportTask> exportTasks = new ArrayList<ExportTask>();

    Session session = getSession(workspace);

    Node siteNode = (Node) session.getItem(siteRootNodePath);
    Node parentNode = siteNode.getParent();

    exportNode(workspace, parentNode, null, exportVersionHistory, exportTasks, siteNode, metaData, activitiesId, exportOnlyMetadata);

    return exportTasks;
  }

  /**
   * Export query result.
   *
   * @param workspace the workspace
   * @param siteRootNodePath the site root node path
   * @param jcrQuery the jcr query
   * @param excludePaths the exclude paths
   * @param exportVersionHistory the export version history
   * @param metaData the meta data
   * @param activitiesId the activities id
   * @param exportOnlyMetadata the export only metadata
   * @return the list
   * @throws Exception the exception
   */
  private List<ExportTask> exportQueryResult(String workspace, String siteRootNodePath, String jcrQuery, List<String> excludePaths, boolean exportVersionHistory, SiteMetaData metaData,
      Set<String> activitiesId, boolean exportOnlyMetadata) throws Exception {
    List<ExportTask> exportTasks = new ArrayList<ExportTask>();

    if (!jcrQuery.contains("jcr:path")) {
      String queryPath = "jcr:path = '" + siteRootNodePath + "/%'";
      queryPath.replace("//", "/");
      if (jcrQuery.contains("where")) {
        int startIndex = jcrQuery.indexOf("where");
        int endIndex = startIndex + "where".length();

        String condition = jcrQuery.substring(endIndex);
        condition = queryPath + " AND (" + condition + ")";

        jcrQuery = jcrQuery.substring(0, startIndex) + " where " + condition;
      } else {
        jcrQuery += " where " + queryPath;
      }
    }

    Session session = getSession(workspace);

    Query query = session.getWorkspace().getQueryManager().createQuery(jcrQuery, Query.SQL);
    NodeIterator nodeIterator = query.execute().getNodes();
    while (nodeIterator.hasNext()) {
      Node node = nodeIterator.nextNode();
      exportNode(workspace, node.getParent(), excludePaths, exportVersionHistory, exportTasks, node, metaData, activitiesId, exportOnlyMetadata);
    }
    return exportTasks;
  }

  /**
   * Export site without skeleton.
   *
   * @param workspace the workspace
   * @param path the path
   * @param exportSiteTaxonomy the export site taxonomy
   * @param exportVersionHistory the export version history
   * @param metaData the meta data
   * @param activitiesId the activities id
   * @param exportOnlyMetadata the export only metadata
   * @return the list
   * @throws Exception the exception
   * @throws RepositoryException the repository exception
   */
  private List<ExportTask> exportSiteWithoutSkeleton(String workspace, String path, boolean exportSiteTaxonomy, boolean exportVersionHistory, SiteMetaData metaData, Set<String> activitiesId,
      boolean exportOnlyMetadata) throws Exception, RepositoryException {

    List<ExportTask> exportTasks = new ArrayList<ExportTask>();

    NodeLocation nodeLocation = new NodeLocation("repository", workspace, path, null, true);
    Node portalNode = NodeLocation.getNodeByLocation(nodeLocation);

    PortalFolderSchemaHandler portalFolderSchemaHandler = new PortalFolderSchemaHandler();

    // CSS Folder
    exportTasks.addAll(exportSubNodes(workspace, portalFolderSchemaHandler.getCSSFolder(portalNode), null, exportVersionHistory, metaData, activitiesId, exportOnlyMetadata));

    // JS Folder
    exportTasks.addAll(exportSubNodes(workspace, portalFolderSchemaHandler.getJSFolder(portalNode), null, exportVersionHistory, metaData, activitiesId, exportOnlyMetadata));

    // Document Folder
    exportTasks.addAll(exportSubNodes(workspace, portalFolderSchemaHandler.getDocumentStorage(portalNode), null, exportVersionHistory, metaData, activitiesId, exportOnlyMetadata));

    // Images Folder
    exportTasks.addAll(exportSubNodes(workspace, portalFolderSchemaHandler.getImagesFolder(portalNode), null, exportVersionHistory, metaData, activitiesId, exportOnlyMetadata));

    // Audio Folder
    exportTasks.addAll(exportSubNodes(workspace, portalFolderSchemaHandler.getAudioFolder(portalNode), null, exportVersionHistory, metaData, activitiesId, exportOnlyMetadata));

    // Video Folder
    exportTasks.addAll(exportSubNodes(workspace, portalFolderSchemaHandler.getVideoFolder(portalNode), null, exportVersionHistory, metaData, activitiesId, exportOnlyMetadata));

    // Multimedia Folder
    exportTasks.addAll(exportSubNodes(workspace, portalFolderSchemaHandler.getMultimediaFolder(portalNode), Arrays.asList("images", "audio", "videos"), exportVersionHistory, metaData, activitiesId, exportOnlyMetadata));

    // Link Folder
    exportTasks.addAll(exportSubNodes(workspace, portalFolderSchemaHandler.getLinkFolder(portalNode), null, exportVersionHistory, metaData, activitiesId, exportOnlyMetadata));

    // WebContent Folder
    exportTasks.addAll(exportSubNodes(workspace, portalFolderSchemaHandler.getWebContentStorage(portalNode), Arrays.asList("site artifacts"), exportVersionHistory, metaData, activitiesId, exportOnlyMetadata));

    // Site Artifacts Folder
    Node webContentNode = portalFolderSchemaHandler.getWebContentStorage(portalNode);
    exportTasks.addAll(exportSubNodes(workspace, webContentNode.getNode("site artifacts"), null, exportVersionHistory, metaData, activitiesId, exportOnlyMetadata));

    if (exportSiteTaxonomy) {
      // Categories Folder
      Node categoriesNode = portalNode.getNode("categories");
      exportTasks.addAll(exportSubNodes(workspace, categoriesNode, null, exportVersionHistory, metaData, activitiesId, exportOnlyMetadata));
    }
    return exportTasks;
  }

  /**
   * Export all sub-nodes of the given node.
   *
   * @param workspace the workspace
   * @param parentNode the parent node
   * @param excludedNodes the excluded nodes
   * @param exportVersionHistory the export version history
   * @param metaData the meta data
   * @param activitiesId the activities id
   * @param exportOnlyMetadata the export only metadata
   * @return the list
   * @throws Exception the exception
   */
  protected List<ExportTask> exportSubNodes(String workspace, Node parentNode, List<String> excludedNodes, boolean exportVersionHistory, SiteMetaData metaData, Set<String> activitiesId,
      boolean exportOnlyMetadata) throws Exception {
    List<ExportTask> subNodesExportTask = new ArrayList<ExportTask>();
    NodeIterator childrenNodes = parentNode.getNodes();
    while (childrenNodes.hasNext()) {
      Node childNode = (Node) childrenNodes.next();
      exportNode(workspace, parentNode, excludedNodes, exportVersionHistory, subNodesExportTask, childNode, metaData, activitiesId, exportOnlyMetadata);
    }
    return subNodesExportTask;
  }

  /**
   * Export node.
   *
   * @param workspace the workspace
   * @param parentNode the parent node
   * @param excludedNodes the excluded nodes
   * @param exportVersionHistory the export version history
   * @param subNodesExportTask the sub nodes export task
   * @param childNode the child node
   * @param metaData the meta data
   * @param activitiesId the activities id
   * @param exportOnlyMetadata the export only metadata
   * @throws Exception the exception
   */
  private void exportNode(String workspace, Node parentNode, List<String> excludedNodes, boolean exportVersionHistory, List<ExportTask> subNodesExportTask, Node childNode, SiteMetaData metaData,
      Set<String> activitiesId, boolean exportOnlyMetadata) throws Exception {
    if (excludedNodes == null || (!excludedNodes.contains(childNode.getName()) && !excludedNodes.contains(childNode.getPath()))) {
      String path = childNode.getPath();
      boolean recursive = isRecursiveExport(childNode);
      if (!exportOnlyMetadata) {
        if (childNode.isNodeType("exo:activityInfo")) {
          String activityId = ActivityTypeUtils.getActivityId(childNode);
          if (activityId != null && !activityId.isEmpty()) {
            activitiesId.add(activityId);
          }
        }
        String prefix = SiteUtil.getSiteContentsBasePath(metaData.getOptions().get(SiteMetaData.SITE_NAME));
        JCRNodeExportTask siteContentExportTask = new JCRNodeExportTask(repositoryService, workspace, path, prefix, recursive, true);
        subNodesExportTask.add(siteContentExportTask);
        String parentPathName = siteContentExportTask.getEntry().substring(0, siteContentExportTask.getEntry().lastIndexOf('/'));
        int initialSize = subNodesExportTask.size();
        DataTransformerService.exportData("Content", subNodesExportTask, childNode, parentPathName);
        if (initialSize < subNodesExportTask.size()) {
          siteContentExportTask.setExportBinary(false);
        }
        if (exportVersionHistory && childNode.isNodeType(org.exoplatform.ecm.webui.utils.Utils.MIX_VERSIONABLE) && childNode.getVersionHistory().hasNodes()) {
          SiteContentsVersionHistoryExportTask versionHistoryExportTask = new SiteContentsVersionHistoryExportTask(repositoryService, workspace, metaData.getOptions().get(SiteMetaData.SITE_NAME), path, recursive);
          subNodesExportTask.add(versionHistoryExportTask);
        }
      } else {
        NodeMetadata nodeMetadata = new NodeMetadata();
        metaData.getNodesMetadata().put(path, nodeMetadata);
        nodeMetadata.setPath(path);
        if (childNode.hasProperty("exo:title")) {
          nodeMetadata.setTitle(childNode.getProperty("exo:title").getString());
        } else if (childNode.hasProperty("exo:name")) {
          nodeMetadata.setTitle(childNode.getProperty("exo:name").getString());
        } else {
          nodeMetadata.setTitle(childNode.getName());
        }
        if (childNode.hasProperty("exo:lastModifier")) {
          nodeMetadata.setLastModifier(childNode.getProperty("exo:lastModifier").getString());
        }
        if (childNode.hasProperty("publication:currentState")) {
          nodeMetadata.setPublished(childNode.getProperty("publication:currentState").getString().equals("published"));
        }
        if (childNode.hasProperty("publication:liveDate")) {
          nodeMetadata.setLiveDate(childNode.getProperty("publication:liveDate").getDate());
        }
        if (childNode.hasProperty("exo:lastModifiedDate")) {
          nodeMetadata.setLastModificationDate(childNode.getProperty("exo:lastModifiedDate").getDate());
        }
      }
      // If not export the whole node
      if (!recursive) {
        NodeIterator nodeIterator = childNode.getNodes();
        while (nodeIterator.hasNext()) {
          Node node = nodeIterator.nextNode();
          exportNode(workspace, childNode, excludedNodes, exportVersionHistory, subNodesExportTask, node, metaData, activitiesId, exportOnlyMetadata);
        }
      }
    }
  }

  /**
   * Gets the parameter value.
   *
   * @param filters the filters
   * @param prefix the prefix
   * @param defaultValue the default value
   * @return the parameter value
   */
  private String getParameterValue(List<String> filters, String prefix, String defaultValue) {
    String value = null;
    for (String filterValue : filters) {
      if (filterValue.startsWith(prefix)) {
        value = filterValue.replace(prefix, "");
        break;
      }
    }
    if (defaultValue != null && (value == null || value.isEmpty())) {
      value = defaultValue;
    }
    return value;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void addJCRNodeExportTask(Node childNode, List<ExportTask> subNodesExportTask, boolean recursive, String... params) throws Exception {}
}
