package org.exoplatform.management.uiextension;

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
import org.exoplatform.management.uiextension.comparaison.NodeComparaison;
import org.exoplatform.management.uiextension.comparaison.NodeComparaisonState;
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

  public static List<NodeComparaison> compareLocalNodesWithTargetServer(String workspace, String nodePathToCompare, TargetServer targetServer) throws Exception {
    Map<String, String> exportOptions = new HashMap<String, String>();
    // Select all descendants
    String sqlQueryFilter = "query:select * from nt:base where publication:currentState IS NOT NULL and jcr:path like '" + nodePathToCompare + "/%' ";
    exportOptions.put("filter/workspace", workspace);
    exportOptions.put("filter/query", sqlQueryFilter);
    exportOptions.put("filter/only-metadata", "true");

    List<NodeComparaison> comparaisons = compareNodesUsingOptions(targetServer, exportOptions);

    exportOptions = new HashMap<String, String>();
    // Select the current node
    sqlQueryFilter = "query:select * from nt:base where publication:currentState IS NOT NULL and jcr:path = '" + nodePathToCompare + "'";
    exportOptions.put("filter/workspace", workspace);
    exportOptions.put("filter/query", sqlQueryFilter);
    exportOptions.put("filter/only-metadata", "true");

    comparaisons.addAll(compareNodesUsingOptions(targetServer, exportOptions));

    return comparaisons;
  }

  private static List<NodeComparaison> compareNodesUsingOptions(TargetServer targetServer, Map<String, String> exportOptions) throws Exception {
    Map<String, NodeMetadata> sourceServerMetadata = getSourceServerMetadata(exportOptions);
    Map<String, NodeMetadata> targetServerMetadata = getTargetServerMetadata(targetServer, exportOptions);

    List<NodeComparaison> nodesComparaison = new ArrayList<NodeComparaison>();
    Collection<NodeMetadata> sourceNodeMetadatas = sourceServerMetadata.values();
    for (NodeMetadata sourceNodeMetadata : sourceNodeMetadatas) {
      String path = sourceNodeMetadata.getPath();
      NodeMetadata targetNodeMetadata = targetServerMetadata.get(path);

      NodeComparaison comparaison = new NodeComparaison();
      comparaison.setTitle(sourceNodeMetadata.getTitle());
      comparaison.setPath(path);
      comparaison.setPublished(sourceNodeMetadata.isPublished());

      comparaison.setTargetModificationDateCalendar(targetNodeMetadata != null && targetNodeMetadata.getDateModified() != null ? targetNodeMetadata.getDateModified() : null);
      comparaison.setSourceModificationDateCalendar(sourceNodeMetadata.getDateModified() != null ? sourceNodeMetadata.getDateModified() : null);

      if (targetNodeMetadata == null) {
        comparaison.setState(NodeComparaisonState.NOT_FOUND_ON_TARGET);
        comparaison.setLastModifierUserName(sourceNodeMetadata.getLastModifier());
      } else {
        boolean sameDate = false;
        if (targetNodeMetadata.getDateModified() != null && sourceNodeMetadata.getDateModified() != null) {
          int comp = targetNodeMetadata.getDateModified().compareTo(sourceNodeMetadata.getDateModified());
          sameDate = comp == 0;
          if (!sameDate) {
            comparaison.setState(comp > 0 ? NodeComparaisonState.MODIFIED_ON_TARGET : NodeComparaisonState.MODIFIED_ON_SOURCE);
            comparaison.setLastModifierUserName(comp > 0 ? targetNodeMetadata.getLastModifier() : sourceNodeMetadata.getLastModifier());
          }
        } else if (targetNodeMetadata.getDateModified() == null && sourceNodeMetadata.getDateModified() == null) {
          sameDate = true;
        } else {
          // If one date is null and the other not, state = uknown
          comparaison.setState(NodeComparaisonState.UNKNOWN);
          comparaison.setLastModifierUserName((targetNodeMetadata != null && targetNodeMetadata.getLastModifier() != null) ? targetNodeMetadata.getLastModifier() : sourceNodeMetadata
              .getLastModifier());
        }
        if (comparaison.getState() == null) {
          if (targetNodeMetadata.getPublicationHistory() != null && sourceNodeMetadata.getPublicationHistory() != null) {
            if (targetNodeMetadata.getPublicationHistory().length() > sourceNodeMetadata.getPublicationHistory().length()) {
              comparaison.setState(NodeComparaisonState.MODIFIED_ON_TARGET);
              comparaison.setLastModifierUserName(targetNodeMetadata.getLastModifier());
            } else if (targetNodeMetadata.getPublicationHistory().length() < sourceNodeMetadata.getPublicationHistory().length()) {
              comparaison.setState(NodeComparaisonState.MODIFIED_ON_SOURCE);
              comparaison.setLastModifierUserName(sourceNodeMetadata.getLastModifier());
            } else {
              comparaison.setState(NodeComparaisonState.SAME);
              comparaison.setLastModifierUserName(sourceNodeMetadata.getLastModifier());
            }
          } else if (targetNodeMetadata.getPublicationHistory() == null && sourceNodeMetadata.getPublicationHistory() == null) {
            comparaison.setState(NodeComparaisonState.SAME);
            comparaison.setLastModifierUserName(sourceNodeMetadata.getLastModifier());
          } else {
            comparaison.setState(NodeComparaisonState.UNKNOWN);
            comparaison.setLastModifierUserName((targetNodeMetadata != null && targetNodeMetadata.getLastModifier() != null) ? targetNodeMetadata.getLastModifier() : sourceNodeMetadata
                .getLastModifier());
          }
        }
      }
      nodesComparaison.add(comparaison);
    }
    Collection<NodeMetadata> targetNodeMetadatas = targetServerMetadata.values();

    // Check only nodes added on target and not presen in source
    for (NodeMetadata targetNodeMetadata : targetNodeMetadatas) {
      String path = targetNodeMetadata.getPath();
      NodeMetadata sourceNodeMetadata = sourceServerMetadata.get(path);
      if (sourceNodeMetadata == null) {
        NodeComparaison comparaison = new NodeComparaison();
        comparaison.setTitle(targetNodeMetadata.getTitle());
        comparaison.setPath(path);
        comparaison.setPublished(targetNodeMetadata.isPublished());
        comparaison.setLastModifierUserName(targetNodeMetadata.getLastModifier());
        comparaison.setTargetModificationDateCalendar(targetNodeMetadata != null ? targetNodeMetadata.getDateModified() : null);
        comparaison.setSourceModificationDateCalendar(null);

        comparaison.setState(NodeComparaisonState.NOT_FOUND_ON_SOURCE);
        nodesComparaison.add(comparaison);
      }
    }
    Collections.sort(nodesComparaison);
    return nodesComparaison;
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
      throw new IllegalStateException("Comparaison operation error, HTTP error code from target server : " + conn.getResponseCode());
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
