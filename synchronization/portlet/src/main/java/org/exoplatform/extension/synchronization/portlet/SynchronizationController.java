package org.exoplatform.extension.synchronization.portlet;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import juzu.Path;
import juzu.Resource;
import juzu.Response;
import juzu.SessionScoped;
import juzu.View;
import juzu.template.Template;

import org.exoplatform.commons.juzu.ajax.Ajax;
import org.exoplatform.extension.synchronization.service.api.Node;
import org.exoplatform.extension.synchronization.service.api.SynchronizationService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

@SessionScoped
public class SynchronizationController {
  private static Log log = ExoLogger.getLogger(SynchronizationController.class);

  @Inject
  SynchronizationService synchronizationService;

  @Inject
  @Path("form.gtmpl")
  Template form;

  @Inject
  @Path("index.gtmpl")
  Template index;

  Set<String> selectedResources = Collections.synchronizedSet(new HashSet<String>());
  Map<String, String> selectedOptions = new Hashtable<String, String>();
  Map<String, Set<Node>> resources = new HashMap<String, Set<Node>>();

  static Map<String, Object> parameters = new HashMap<String, Object>();
  static {
    // PATHS
    parameters.put("portalSitePath", SynchronizationService.SITES_PORTAL_PATH);
    parameters.put("groupSitePath", SynchronizationService.SITES_GROUP_PATH);
    parameters.put("userSitePath", SynchronizationService.SITES_USER_PATH);
    parameters.put("siteContentPath", SynchronizationService.CONTENT_SITES_PATH);
    parameters.put("applicationCLVTemplatesPath", SynchronizationService.ECM_TEMPLATES_APPLICATION_CLV_PATH);
    parameters.put("applicationSearchTemplatesPath", SynchronizationService.ECM_TEMPLATES_APPLICATION_SEARCH_PATH);
    parameters.put("documentTypeTemplatesPath", SynchronizationService.ECM_TEMPLATES_DOCUMENT_TYPE_PATH);
    parameters.put("metadataTemplatesPath", SynchronizationService.ECM_TEMPLATES_METADATA_PATH);
    parameters.put("taxonomyPath", SynchronizationService.ECM_TAXONOMY_PATH);
    parameters.put("queryPath", SynchronizationService.ECM_QUERY_PATH);
    parameters.put("drivePath", SynchronizationService.ECM_DRIVE_PATH);
    parameters.put("scriptPath", SynchronizationService.ECM_SCRIPT_PATH);
    parameters.put("viewTemplatePath", SynchronizationService.ECM_VIEW_TEMPLATES_PATH);
    parameters.put("viewConfigurationPath", SynchronizationService.ECM_VIEW_CONFIGURATION_PATH);
    parameters.put("actionNodeTypePath", SynchronizationService.ECM_ACTION_PATH);
    parameters.put("nodeTypePath", SynchronizationService.ECM_NODETYPE_PATH);
    parameters.put("registryPath", SynchronizationService.REGISTRY_PATH);
  }

  @View
  public Response.Render index() {
    // Clear selection
    selectedResources.clear();
    selectedOptions.clear();

    // NODES
    resources.put(SynchronizationService.SITES_PORTAL_PATH, synchronizationService.getPortalSiteNodes());
    resources.put(SynchronizationService.SITES_GROUP_PATH, synchronizationService.getGroupSiteNodes());
    resources.put(SynchronizationService.SITES_USER_PATH, synchronizationService.getUserSiteNodes());
    resources.put(SynchronizationService.CONTENT_SITES_PATH, synchronizationService.getSiteContentNodes());
    resources.put(SynchronizationService.ECM_TEMPLATES_APPLICATION_CLV_PATH, synchronizationService.getApplicationCLVTemplatesNodes());
    resources.put(SynchronizationService.ECM_TEMPLATES_APPLICATION_SEARCH_PATH, synchronizationService.getApplicationSearchTemplatesNodes());
    resources.put(SynchronizationService.ECM_TEMPLATES_DOCUMENT_TYPE_PATH, synchronizationService.getDocumentTypeTemplatesNodes());
    resources.put(SynchronizationService.ECM_TEMPLATES_METADATA_PATH, synchronizationService.getMetadataTemplatesNodes());
    resources.put(SynchronizationService.ECM_TAXONOMY_PATH, synchronizationService.getTaxonomyNodes());
    resources.put(SynchronizationService.ECM_QUERY_PATH, synchronizationService.getQueryNodes());
    resources.put(SynchronizationService.ECM_DRIVE_PATH, synchronizationService.getDriveNodes());
    resources.put(SynchronizationService.ECM_SCRIPT_PATH, synchronizationService.getScriptNodes());
    resources.put(SynchronizationService.ECM_ACTION_PATH, synchronizationService.getActionNodeTypeNodes());
    resources.put(SynchronizationService.ECM_NODETYPE_PATH, synchronizationService.getNodeTypeNodes());
    resources.put(SynchronizationService.REGISTRY_PATH, synchronizationService.getRegistryNodes());
    resources.put(SynchronizationService.ECM_VIEW_TEMPLATES_PATH, synchronizationService.getViewTemplatesNodes());
    resources.put(SynchronizationService.ECM_VIEW_CONFIGURATION_PATH, synchronizationService.getViewConfigurationNodes());

    // Set Nodes in parameters
    parameters.put("portalSiteNodes", resources.get(SynchronizationService.SITES_PORTAL_PATH));
    parameters.put("groupSiteNodes", resources.get(SynchronizationService.SITES_GROUP_PATH));
    parameters.put("userSiteNodes", resources.get(SynchronizationService.SITES_USER_PATH));
    parameters.put("siteContentNodes", resources.get(SynchronizationService.CONTENT_SITES_PATH));
    parameters.put("applicationCLVTemplatesNodes", resources.get(SynchronizationService.ECM_TEMPLATES_APPLICATION_CLV_PATH));
    parameters.put("applicationSearchTemplatesNodes", resources.get(SynchronizationService.ECM_TEMPLATES_APPLICATION_SEARCH_PATH));
    parameters.put("documentTypeTemplatesNodes", resources.get(SynchronizationService.ECM_TEMPLATES_DOCUMENT_TYPE_PATH));
    parameters.put("metadataTemplatesNodes", resources.get(SynchronizationService.ECM_TEMPLATES_METADATA_PATH));
    parameters.put("taxonomyNodes", resources.get(SynchronizationService.ECM_TAXONOMY_PATH));
    parameters.put("queryNodes", resources.get(SynchronizationService.ECM_QUERY_PATH));
    parameters.put("driveNodes", resources.get(SynchronizationService.ECM_DRIVE_PATH));
    parameters.put("scriptNodes", resources.get(SynchronizationService.ECM_SCRIPT_PATH));
    parameters.put("actionNodeTypeNodes", resources.get(SynchronizationService.ECM_ACTION_PATH));
    parameters.put("nodeTypeNodes", resources.get(SynchronizationService.ECM_NODETYPE_PATH));
    parameters.put("registryNodes", resources.get(SynchronizationService.REGISTRY_PATH));
    parameters.put("viewTemplateNodes", resources.get(SynchronizationService.ECM_VIEW_TEMPLATES_PATH));
    parameters.put("viewConfigurationNodes", resources.get(SynchronizationService.ECM_VIEW_CONFIGURATION_PATH));

    parameters.put("selectedResources", selectedResources);

    parameters.put("portalSiteSelectedNodes", getSelectedResources(SynchronizationService.SITES_PORTAL_PATH));
    parameters.put("groupSiteSelectedNodes", getSelectedResources(SynchronizationService.SITES_GROUP_PATH));
    parameters.put("userSiteSelectedNodes", getSelectedResources(SynchronizationService.SITES_USER_PATH));
    parameters.put("siteContentSelectedNodes", getSelectedResources(SynchronizationService.CONTENT_SITES_PATH));
    parameters.put("applicationCLVTemplatesSelectedNodes", getSelectedResources(SynchronizationService.ECM_TEMPLATES_APPLICATION_CLV_PATH));
    parameters.put("applicationSearchTemplatesSelectedNodes", getSelectedResources(SynchronizationService.ECM_TEMPLATES_APPLICATION_SEARCH_PATH));
    parameters.put("documentTypeTemplatesSelectedNodes", getSelectedResources(SynchronizationService.ECM_TEMPLATES_DOCUMENT_TYPE_PATH));
    parameters.put("metadataTemplatesSelectedNodes", getSelectedResources(SynchronizationService.ECM_TEMPLATES_METADATA_PATH));
    parameters.put("taxonomySelectedNodes", getSelectedResources(SynchronizationService.ECM_TAXONOMY_PATH));
    parameters.put("querySelectedNodes", getSelectedResources(SynchronizationService.ECM_QUERY_PATH));
    parameters.put("driveSelectedNodes", getSelectedResources(SynchronizationService.ECM_DRIVE_PATH));
    parameters.put("scriptSelectedNodes", getSelectedResources(SynchronizationService.ECM_SCRIPT_PATH));
    parameters.put("actionNodeTypeSelectedNodes", getSelectedResources(SynchronizationService.ECM_ACTION_PATH));
    parameters.put("nodeTypeSelectedNodes", getSelectedResources(SynchronizationService.ECM_NODETYPE_PATH));
    parameters.put("registrySelectedNodes", getSelectedResources(SynchronizationService.REGISTRY_PATH));
    parameters.put("viewTemplateSelectedNodes", getSelectedResources(SynchronizationService.ECM_VIEW_TEMPLATES_PATH));
    parameters.put("viewConfigurationSelectedNodes", getSelectedResources(SynchronizationService.ECM_VIEW_CONFIGURATION_PATH));

    return index.ok(parameters);
  }

  @Ajax
  @Resource
  public void selectResources(String path, String checked) {
    if (checked != null && path != null && !checked.isEmpty() && !path.isEmpty()) {
      if (checked.equals("true")) {
        if (resources.containsKey(path)) {
          Set<Node> children = resources.get(path);
          for (Node node : children) {
            selectedResources.add(node.getPath());
          }
        } else {
          selectedResources.add(path);
        }
      } else {
        if (resources.containsKey(path)) {
          Set<Node> children = resources.get(path);
          for (Node node : children) {
            selectedResources.remove(node.getPath());
          }
        } else {
          selectedResources.remove(path);
        }
      }
    } else {
      log.warn("displayForm: Selection not considered.");
    }

    parameters.put("portalSiteSelectedNodes", getSelectedResources(SynchronizationService.SITES_PORTAL_PATH));
    parameters.put("groupSiteSelectedNodes", getSelectedResources(SynchronizationService.SITES_GROUP_PATH));
    parameters.put("userSiteSelectedNodes", getSelectedResources(SynchronizationService.SITES_USER_PATH));
    parameters.put("siteContentSelectedNodes", getSelectedResources(SynchronizationService.CONTENT_SITES_PATH));
    parameters.put("applicationCLVTemplatesSelectedNodes", getSelectedResources(SynchronizationService.ECM_TEMPLATES_APPLICATION_CLV_PATH));
    parameters.put("applicationSearchTemplatesSelectedNodes", getSelectedResources(SynchronizationService.ECM_TEMPLATES_APPLICATION_SEARCH_PATH));
    parameters.put("documentTypeTemplatesSelectedNodes", getSelectedResources(SynchronizationService.ECM_TEMPLATES_DOCUMENT_TYPE_PATH));
    parameters.put("metadataTemplatesSelectedNodes", getSelectedResources(SynchronizationService.ECM_TEMPLATES_METADATA_PATH));
    parameters.put("taxonomySelectedNodes", getSelectedResources(SynchronizationService.ECM_TAXONOMY_PATH));
    parameters.put("querySelectedNodes", getSelectedResources(SynchronizationService.ECM_QUERY_PATH));
    parameters.put("driveSelectedNodes", getSelectedResources(SynchronizationService.ECM_DRIVE_PATH));
    parameters.put("scriptSelectedNodes", getSelectedResources(SynchronizationService.ECM_SCRIPT_PATH));
    parameters.put("actionNodeTypeSelectedNodes", getSelectedResources(SynchronizationService.ECM_ACTION_PATH));
    parameters.put("nodeTypeSelectedNodes", getSelectedResources(SynchronizationService.ECM_NODETYPE_PATH));
    parameters.put("registrySelectedNodes", getSelectedResources(SynchronizationService.REGISTRY_PATH));
    parameters.put("viewTemplateSelectedNodes", getSelectedResources(SynchronizationService.ECM_VIEW_TEMPLATES_PATH));
    parameters.put("viewConfigurationSelectedNodes", getSelectedResources(SynchronizationService.ECM_VIEW_CONFIGURATION_PATH));

    form.render(parameters);
  }

  @Ajax
  @Resource
  public void selectOption(String name, String value) {
    if (value == null || value.trim().isEmpty()) {
      selectedOptions.remove(name);
    } else {
      selectedOptions.put(name, value);
    }
  }

  @Ajax
  @Resource
  public Response synchronize(String isSSLString, String host, String port, String username, String password) throws IOException {
    try {
      synchronizationService.synchronize(selectedResources, selectedOptions, isSSLString, host, port, username, password);
      return Response.ok("Successfully proceeded.");
    } catch (Exception e) {
      log.error("Error while synchronization, ", e);
      return Response.ok("Error occured while synchronizing Managed Resources: " + e.getMessage());
    }
  }

  @Ajax
  @Resource
  public Response executeSQL(String sql) throws IOException {
    try {
      Set<String> resultedNodePaths = synchronizationService.executeSQL(sql, selectedResources);
      StringBuilder builder = new StringBuilder("<ul>");
      for (String path : resultedNodePaths) {
        builder.append("<li>");
        builder.append(path);
        builder.append("</li>");
      }
      builder.append("</ul>");
      return Response.ok(builder.toString());
    } catch (Exception e) {
      log.error("Error while executing request: " + sql, e);
      return Response.ok("Error while executing request: " + e.getMessage());
    }
  }

  private Set<String> getSelectedResources(String parentPath) {
    Set<String> resources = synchronizationService.filterSelectedResources(selectedResources, parentPath);
    Set<String> selectedResources = new HashSet<String>();
    for (String resource : resources) {
      resource = resource.replace(parentPath, "");
      if (resource.startsWith("/")) {
        resource = resource.substring(1);
      }
      selectedResources.add(resource);
    }
    return selectedResources;
  }

}
