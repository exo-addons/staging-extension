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
package org.exoplatform.management.uiextension;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.exoplatform.management.common.FileEntry;
import org.exoplatform.management.content.operations.site.contents.NodeMetadata;
import org.exoplatform.management.content.operations.site.contents.SiteContentsImportResource;
import org.exoplatform.management.content.operations.site.contents.SiteMetaData;
import org.exoplatform.management.content.operations.site.contents.SiteMetaDataExportTask;
import org.exoplatform.management.service.api.AbstractResourceHandler;
import org.exoplatform.management.service.api.StagingService;
import org.exoplatform.management.service.api.TargetServer;
import org.exoplatform.management.service.handler.ResourceHandlerLocator;
import org.exoplatform.management.uiextension.comparison.NodeComparison;
import org.exoplatform.management.uiextension.comparison.NodeComparisonState;
import org.exoplatform.services.security.ConversationState;
import org.gatein.management.api.controller.ManagedResponse;
import org.gatein.management.api.operation.model.ExportResourceModel;
import org.gatein.management.api.operation.model.ExportTask;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The Class Utils.
 */
public class Utils {
  
  /** The Constant SITE_CONTENTS_IMPORT_RESOURCE. */
  private static final SiteContentsImportResource SITE_CONTENTS_IMPORT_RESOURCE = new SiteContentsImportResource();

  /** The contents handler. */
  private static AbstractResourceHandler CONTENTS_HANDLER = (AbstractResourceHandler) ResourceHandlerLocator.getResourceHandler(StagingService.CONTENT_SITES_PATH);

  /**
   * Checks for push button permission.
   *
   * @param varName the var name
   * @return true, if successful
   */
  public static boolean hasPushButtonPermission(String varName) {
    ConversationState state = ConversationState.getCurrent();
    if (state == null || state.getIdentity() == null) {
      return false;
    }
    String pushButtonPermmission = System.getProperty(varName, null);
    if (pushButtonPermmission != null && pushButtonPermmission.trim().length() > 0) {
      String[] permissions = pushButtonPermmission.split(";");
      for (String permission : permissions) {
        permission = permission.trim();
        if (permission.isEmpty()) {
          continue;
        }
        String[] permissionParts = permission.split(":");
        String group = permissionParts.length > 1 ? permissionParts[1] : permissionParts[0];
        String membershipType = permissionParts.length > 1 ? permissionParts[0] : "*";
        if (state.getIdentity().isMemberOf(group, membershipType)) {
          return true;
        }
      }
      return false;
    } else {
      return true;
    }
  }

  /**
   * Compare local nodes with target server.
   *
   * @param workspace the workspace
   * @param nodePathToCompare the node path to compare
   * @param targetServer the target server
   * @return the list
   * @throws Exception the exception
   */
  public static List<NodeComparison> compareLocalNodesWithTargetServer(String workspace, String nodePathToCompare, TargetServer targetServer) throws Exception {
    Map<String, String> exportOptions = new HashMap<String, String>();
    // Select all descendants
    String sqlQueryFilter = "query:select * from nt:base where publication:currentState IS NOT NULL and jcr:path like '" + nodePathToCompare + "/%' ";
    exportOptions.put("filter/workspace", workspace);
    exportOptions.put("filter/query", sqlQueryFilter);
    exportOptions.put("filter/only-metadata", "true");

    List<NodeComparison> comparisons = compareNodesUsingOptions(targetServer, exportOptions);

    exportOptions = new HashMap<String, String>();
    // Select the current node
    sqlQueryFilter = "query:select * from nt:base where publication:currentState IS NOT NULL and jcr:path = '" + nodePathToCompare + "'";
    exportOptions.put("filter/workspace", workspace);
    exportOptions.put("filter/query", sqlQueryFilter);
    exportOptions.put("filter/only-metadata", "true");

    comparisons.addAll(compareNodesUsingOptions(targetServer, exportOptions));

    return comparisons;
  }

  /**
   * Compare nodes using options.
   *
   * @param targetServer the target server
   * @param exportOptions the export options
   * @return the list
   * @throws Exception the exception
   */
  private static List<NodeComparison> compareNodesUsingOptions(TargetServer targetServer, Map<String, String> exportOptions) throws Exception {
    Map<String, NodeMetadata> sourceServerMetadata = getSourceServerMetadata(exportOptions);
    Map<String, NodeMetadata> targetServerMetadata = getTargetServerMetadata(targetServer, exportOptions);

    List<NodeComparison> nodesComparison = new ArrayList<NodeComparison>();
    Collection<NodeMetadata> sourceNodeMetadatas = sourceServerMetadata.values();
    for (NodeMetadata sourceNodeMetadata : sourceNodeMetadatas) {
      String path = sourceNodeMetadata.getPath();
      NodeMetadata targetNodeMetadata = targetServerMetadata.get(path);

      NodeComparison comparison = new NodeComparison();
      comparison.setTitle(sourceNodeMetadata.getTitle());
      comparison.setPath(path);
      comparison.setPublished(sourceNodeMetadata.isPublished());

      comparison.setTargetModificationDateCalendar(targetNodeMetadata != null && targetNodeMetadata.getLastModificationDate() != null ? targetNodeMetadata.getLastModificationDate() : null);
      comparison.setTargetPublicationDateCalendar(targetNodeMetadata != null && targetNodeMetadata.getLiveDate() != null ? targetNodeMetadata.getLiveDate() : null);
      comparison.setSourceModificationDateCalendar(sourceNodeMetadata != null && sourceNodeMetadata.getLastModificationDate() != null ? sourceNodeMetadata.getLastModificationDate() : null);
      comparison.setSourcePublicationDateCalendar(sourceNodeMetadata != null && sourceNodeMetadata.getLiveDate() != null ? sourceNodeMetadata.getLiveDate() : null);

      if (targetNodeMetadata == null) {
        comparison.setState(NodeComparisonState.NOT_FOUND_ON_TARGET);
        comparison.setLastModifierUserName(sourceNodeMetadata.getLastModifier());
      } else {
        boolean sameContent = false;
        if (targetNodeMetadata.getLastModificationDate() != null && sourceNodeMetadata.getLastModificationDate() != null) {
          int comp = targetNodeMetadata.getLastModificationDate().compareTo(sourceNodeMetadata.getLastModificationDate());
          sameContent = comp == 0;
          if (!sameContent) {
            comparison.setState(comp > 0 ? NodeComparisonState.MODIFIED_ON_TARGET : NodeComparisonState.MODIFIED_ON_SOURCE);
            comparison.setLastModifierUserName(comp > 0 ? targetNodeMetadata.getLastModifier() : sourceNodeMetadata.getLastModifier());
          } else if (targetNodeMetadata.isPublished() && !sourceNodeMetadata.isPublished()) {
            comparison.setState(NodeComparisonState.MODIFIED_ON_SOURCE);
            comparison.setLastModifierUserName(sourceNodeMetadata.getLastModifier());
          }
        } else if (targetNodeMetadata.getLastModificationDate() == null && sourceNodeMetadata.getLastModificationDate() == null) {
          sameContent = true;
        } else if (targetNodeMetadata.getLastModificationDate() == null) {
          comparison.setState(NodeComparisonState.MODIFIED_ON_SOURCE);
          comparison.setLastModifierUserName(sourceNodeMetadata.getLastModifier());
        } else if (sourceNodeMetadata.getLastModificationDate() == null) {
          comparison.setState(NodeComparisonState.MODIFIED_ON_TARGET);
          comparison.setLastModifierUserName(targetNodeMetadata.getLastModifier());
        } else {
          comparison.setState(NodeComparisonState.UNKNOWN);
          comparison.setLastModifierUserName(sourceNodeMetadata.getLastModifier());
        }
        if (sameContent) {
          comparison.setState(NodeComparisonState.SAME);
          comparison.setLastModifierUserName(sourceNodeMetadata.getLastModifier());
        }
      }
      nodesComparison.add(comparison);
    }
    Collection<NodeMetadata> targetNodeMetadatas = targetServerMetadata.values();

    // Check only nodes added on target and not present in source
    for (NodeMetadata targetNodeMetadata : targetNodeMetadatas) {
      String path = targetNodeMetadata.getPath();
      NodeMetadata sourceNodeMetadata = sourceServerMetadata.get(path);
      if (sourceNodeMetadata == null) {
        NodeComparison comparison = new NodeComparison();
        comparison.setTitle(targetNodeMetadata.getTitle());
        comparison.setPath(path);
        comparison.setPublished(false);
        comparison.setLastModifierUserName(targetNodeMetadata.getLastModifier());
        comparison.setTargetModificationDateCalendar(targetNodeMetadata != null ? targetNodeMetadata.getLastModificationDate() : null);
        comparison.setTargetPublicationDateCalendar(targetNodeMetadata != null ? targetNodeMetadata.getLastModificationDate() : null);
        comparison.setSourceModificationDateCalendar(null);

        comparison.setState(NodeComparisonState.NOT_FOUND_ON_SOURCE);
        nodesComparison.add(comparison);
      }
    }
    Collections.sort(nodesComparison);
    return nodesComparison;
  }

  /**
   * Gets the source server metadata.
   *
   * @param exportOptions the export options
   * @return the source server metadata
   */
  private static Map<String, NodeMetadata> getSourceServerMetadata(Map<String, String> exportOptions) {
    ManagedResponse response = CONTENTS_HANDLER.getExportedResourceFromOperation(StagingService.CONTENT_SITES_PATH + "/shared", exportOptions);

    ExportResourceModel result = (ExportResourceModel) response.getResult();
    if (result.getTasks() == null || result.getTasks().size() == 0) {
      throw new IllegalStateException("Exported Gatein Management Tasks from local are different from what is expected.");
    }
    SiteMetaDataExportTask siteMetaDataExportTask = null;
    for (ExportTask exportTask : result.getTasks()) {
      if (exportTask instanceof SiteMetaDataExportTask) {
        siteMetaDataExportTask = (SiteMetaDataExportTask) exportTask;
      }
    }
    if (siteMetaDataExportTask == null) {
      throw new IllegalStateException("Exported Gatein Management Tasks from local are different from what is expected.");
    }
    SiteMetaData siteMetaData = siteMetaDataExportTask.getMetaData();
    Map<String, NodeMetadata> sourceServerMetadata = siteMetaData.getNodesMetadata();
    return sourceServerMetadata;
  }

  /**
   * Gets the target server metadata.
   *
   * @param targetServer the target server
   * @param exportOptions the export options
   * @return the target server metadata
   * @throws Exception the exception
   */
  private static Map<String, NodeMetadata> getTargetServerMetadata(TargetServer targetServer, Map<String, String> exportOptions) throws Exception {
    String targetServerURL = AbstractResourceHandler.getServerURL(targetServer, StagingService.CONTENT_SITES_PATH + "/shared.zip", exportOptions);
    URL url = new URL(targetServerURL);

    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    conn.setRequestMethod("GET");
    String passString = targetServer.getUsername() + ":" + targetServer.getPassword();
    String basicAuth = "Basic " + new String(Base64.encodeBase64(passString.getBytes()));
    conn.setRequestProperty("Authorization", basicAuth);
    conn.setUseCaches(false);
    conn.setRequestProperty("Connection", "Keep-Alive");

    if (conn.getResponseCode() != 200) {
      throw new IllegalStateException("Comparison operation error, HTTP error code from target server : " + conn.getResponseCode());
    }

    String tempParentFolderPath = null;
    try {
      // extract data from zip
      Map<String, List<FileEntry>> fileEntriesMap = SITE_CONTENTS_IMPORT_RESOURCE.extractDataFromZip(conn.getInputStream());
      List<FileEntry> fileEntries = fileEntriesMap.get("shared");
      SiteMetaData siteMetadata = SiteContentsImportResource.getSiteMetadata(fileEntries);
      return siteMetadata.getNodesMetadata();
    } finally {
      if (tempParentFolderPath != null) {
        File tempFolderFile = new File(tempParentFolderPath);
        if (tempFolderFile.exists()) {
          try {
            FileUtils.deleteDirectory(tempFolderFile);
          } catch (IOException e) {
            // Nothing to do
          }
        }
      }
    }
  }
}
