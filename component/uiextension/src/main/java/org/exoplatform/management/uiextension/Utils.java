package org.exoplatform.management.uiextension;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.exoplatform.management.common.FileEntry;
import org.exoplatform.management.content.operations.site.contents.NodeMetadata;
import org.exoplatform.management.content.operations.site.contents.SiteContentsImportResource;
import org.exoplatform.management.content.operations.site.contents.SiteMetaData;
import org.exoplatform.management.content.operations.site.contents.SiteMetaDataExportTask;
import org.exoplatform.management.service.api.StagingService;
import org.exoplatform.management.service.api.TargetServer;
import org.exoplatform.management.service.handler.ResourceHandlerLocator;
import org.exoplatform.management.service.handler.content.SiteContentsHandler;
import org.exoplatform.management.uiextension.comparison.NodeComparison;
import org.exoplatform.management.uiextension.comparison.NodeComparisonState;
import org.exoplatform.services.security.ConversationState;
import org.gatein.management.api.controller.ManagedResponse;
import org.gatein.management.api.operation.model.ExportResourceModel;
import org.gatein.management.api.operation.model.ExportTask;

public class Utils {
  private static final SiteContentsImportResource SITE_CONTENTS_IMPORT_RESOURCE = new SiteContentsImportResource();

  private static SiteContentsHandler CONTENTS_HANDLER = (SiteContentsHandler) ResourceHandlerLocator.getResourceHandler(StagingService.CONTENT_SITES_PATH);

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

      comparison.setTargetModificationDateCalendar(targetNodeMetadata != null && targetNodeMetadata.getLiveDate() != null ? targetNodeMetadata.getLiveDate() : null);
      comparison.setSourceModificationDateCalendar(sourceNodeMetadata.getLiveDate() != null ? sourceNodeMetadata.getLiveDate() : null);

      comparison.setSourcePermissions(sourceNodeMetadata != null ? sourceNodeMetadata.getPermissions() : null);
      comparison.setTargetPermissions(targetNodeMetadata != null ? targetNodeMetadata.getPermissions() : null);

      comparison.setState(NodeComparisonState.SAME);
      comparison.setLastModifierUserName(sourceNodeMetadata.getLastModifier());

      if (targetNodeMetadata == null) {
        comparison.setState(NodeComparisonState.NOT_FOUND_ON_TARGET);
        comparison.setLastModifierUserName(sourceNodeMetadata.getLastModifier());
      } else {
        // Compare using live date
        boolean sameContent = false;
        if (targetNodeMetadata.getLiveDate() != null && sourceNodeMetadata.getLiveDate() != null) {
          int comp = targetNodeMetadata.getLiveDate().compareTo(sourceNodeMetadata.getLiveDate());
          sameContent = comp == 0;
          if (!sameContent) {
            comparison.setState(comp > 0 ? NodeComparisonState.MODIFIED_ON_TARGET : NodeComparisonState.MODIFIED_ON_SOURCE);
            comparison.setLastModifierUserName(comp > 0 ? targetNodeMetadata.getLastModifier() : sourceNodeMetadata.getLastModifier());
          } else if (targetNodeMetadata.isPublished() && !sourceNodeMetadata.isPublished()) {
            comparison.setState(NodeComparisonState.MODIFIED_ON_SOURCE);
          }
        } else if (targetNodeMetadata.getLiveDate() == null && sourceNodeMetadata.getLiveDate() == null) {
          sameContent = true;
        } else if (targetNodeMetadata.getLiveDate() == null) {
          comparison.setState(NodeComparisonState.MODIFIED_ON_SOURCE);
        } else if (sourceNodeMetadata.getLiveDate() == null) {
          comparison.setState(NodeComparisonState.MODIFIED_ON_TARGET);
          comparison.setLastModifierUserName(targetNodeMetadata.getLastModifier());
        } else {
          comparison.setState(NodeComparisonState.UNKNOWN);
        }
        // Compare using permissions
        if (sameContent) {
          if (comparison.getSourcePermissions() != comparison.getTargetPermissions()) {
            if (comparison.getSourcePermissions() == null) {
              comparison.setState(NodeComparisonState.MODIFIED_ON_TARGET);
              comparison.setLastModifierUserName(targetNodeMetadata.getLastModifier());
            } else if (comparison.getTargetPermissions() == null || !equals(comparison.getSourcePermissions(), comparison.getTargetPermissions())) {
              comparison.setState(NodeComparisonState.MODIFIED_ON_SOURCE);
            }
          }
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
        comparison.setPublished(targetNodeMetadata.isPublished());
        comparison.setLastModifierUserName(targetNodeMetadata.getLastModifier());
        comparison.setTargetModificationDateCalendar(targetNodeMetadata != null ? targetNodeMetadata.getLiveDate() : null);
        comparison.setSourceModificationDateCalendar(null);

        comparison.setState(NodeComparisonState.NOT_FOUND_ON_SOURCE);
        nodesComparison.add(comparison);
      }
    }
    Collections.sort(nodesComparison);
    return nodesComparison;
  }

  private static boolean equals(String[] sourcePermissions, String[] targetPermissions) {
    Arrays.sort(sourcePermissions);
    Arrays.sort(targetPermissions);
    return Arrays.equals(sourcePermissions, targetPermissions);
  }

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

  private static Map<String, NodeMetadata> getTargetServerMetadata(TargetServer targetServer, Map<String, String> exportOptions) throws Exception {
    String targetServerURL = CONTENTS_HANDLER.getServerURL(targetServer, StagingService.CONTENT_SITES_PATH + "/shared.zip", exportOptions);
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
