package org.exoplatform.extension.generator.portlet;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
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
import org.exoplatform.extension.generator.service.api.ExtensionGenerator;
import org.exoplatform.extension.generator.service.api.Node;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

@SessionScoped
public class ExtensionGeneratorController {
  private static Log log = ExoLogger.getLogger(ExtensionGeneratorController.class);

  @Inject
  ExtensionGenerator extensionGeneratorService;

  @Inject
  @Path("form.gtmpl")
  Template form;

  @Inject
  @Path("index.gtmpl")
  Template index;

  Set<String> selectedResources = Collections.synchronizedSet(new HashSet<String>());
  Map<String, Set<Node>> resources = new HashMap<String, Set<Node>>();

  static Map<String, Object> parameters = new HashMap<String, Object>();
  static {
    // PATHS
    parameters.put("portalSitePath", ExtensionGenerator.SITES_PORTAL_PATH);
    parameters.put("groupSitePath", ExtensionGenerator.SITES_GROUP_PATH);
    parameters.put("userSitePath", ExtensionGenerator.SITES_USER_PATH);
    parameters.put("siteContentPath", ExtensionGenerator.CONTENT_SITES_PATH);
    parameters.put("applicationCLVTemplatesPath", ExtensionGenerator.ECM_TEMPLATES_APPLICATION_CLV_PATH);
    parameters.put("applicationSearchTemplatesPath", ExtensionGenerator.ECM_TEMPLATES_APPLICATION_SEARCH_PATH);
    parameters.put("documentTypeTemplatesPath", ExtensionGenerator.ECM_TEMPLATES_DOCUMENT_TYPE_PATH);
    parameters.put("metadataTemplatesPath", ExtensionGenerator.ECM_TEMPLATES_METADATA_PATH);
    parameters.put("taxonomyPath", ExtensionGenerator.ECM_TAXONOMY_PATH);
    parameters.put("queryPath", ExtensionGenerator.ECM_QUERY_PATH);
    parameters.put("drivePath", ExtensionGenerator.ECM_DRIVE_PATH);
    parameters.put("scriptPath", ExtensionGenerator.ECM_SCRIPT_PATH);
    parameters.put("actionNodeTypePath", ExtensionGenerator.ECM_ACTION_PATH);
    parameters.put("nodeTypePath", ExtensionGenerator.ECM_NODETYPE_PATH);
    parameters.put("registryPath", ExtensionGenerator.REGISTRY_PATH);
  }

  @View
  public Response.Render index() {
    // NODES
    resources.put(ExtensionGenerator.SITES_PORTAL_PATH, extensionGeneratorService.getPortalSiteNodes());
    resources.put(ExtensionGenerator.SITES_GROUP_PATH, extensionGeneratorService.getGroupSiteNodes());
    resources.put(ExtensionGenerator.SITES_USER_PATH, extensionGeneratorService.getUserSiteNodes());
    resources.put(ExtensionGenerator.CONTENT_SITES_PATH, extensionGeneratorService.getSiteContentNodes());
    resources.put(ExtensionGenerator.ECM_TEMPLATES_APPLICATION_CLV_PATH, extensionGeneratorService.getApplicationCLVTemplatesNodes());
    resources.put(ExtensionGenerator.ECM_TEMPLATES_APPLICATION_SEARCH_PATH, extensionGeneratorService.getApplicationSearchTemplatesNodes());
    resources.put(ExtensionGenerator.ECM_TEMPLATES_DOCUMENT_TYPE_PATH, extensionGeneratorService.getDocumentTypeTemplatesNodes());
    resources.put(ExtensionGenerator.ECM_TEMPLATES_METADATA_PATH, extensionGeneratorService.getMetadataTemplatesNodes());
    resources.put(ExtensionGenerator.ECM_TAXONOMY_PATH, extensionGeneratorService.getTaxonomyNodes());
    resources.put(ExtensionGenerator.ECM_QUERY_PATH, extensionGeneratorService.getQueryNodes());
    resources.put(ExtensionGenerator.ECM_DRIVE_PATH, extensionGeneratorService.getDriveNodes());
    resources.put(ExtensionGenerator.ECM_SCRIPT_PATH, extensionGeneratorService.getScriptNodes());
    resources.put(ExtensionGenerator.ECM_ACTION_PATH, extensionGeneratorService.getActionNodeTypeNodes());
    resources.put(ExtensionGenerator.ECM_NODETYPE_PATH, extensionGeneratorService.getNodeTypeNodes());
    resources.put(ExtensionGenerator.REGISTRY_PATH, extensionGeneratorService.getRegistryNodes());

    // Set Nodes in parameters
    parameters.put("portalSiteNodes", resources.get(ExtensionGenerator.SITES_PORTAL_PATH));
    parameters.put("groupSiteNodes", resources.get(ExtensionGenerator.SITES_GROUP_PATH));
    parameters.put("userSiteNodes", resources.get(ExtensionGenerator.SITES_USER_PATH));
    parameters.put("siteContentNodes", resources.get(ExtensionGenerator.CONTENT_SITES_PATH));
    parameters.put("applicationCLVTemplatesNodes", resources.get(ExtensionGenerator.ECM_TEMPLATES_APPLICATION_CLV_PATH));
    parameters.put("applicationSearchTemplatesNodes", resources.get(ExtensionGenerator.ECM_TEMPLATES_APPLICATION_SEARCH_PATH));
    parameters.put("documentTypeTemplatesNodes", resources.get(ExtensionGenerator.ECM_TEMPLATES_DOCUMENT_TYPE_PATH));
    parameters.put("metadataTemplatesNodes", resources.get(ExtensionGenerator.ECM_TEMPLATES_METADATA_PATH));
    parameters.put("taxonomyNodes", resources.get(ExtensionGenerator.ECM_TAXONOMY_PATH));
    parameters.put("queryNodes", resources.get(ExtensionGenerator.ECM_QUERY_PATH));
    parameters.put("driveNodes", resources.get(ExtensionGenerator.ECM_DRIVE_PATH));
    parameters.put("scriptNodes", resources.get(ExtensionGenerator.ECM_SCRIPT_PATH));
    parameters.put("actionNodeTypeNodes", resources.get(ExtensionGenerator.ECM_ACTION_PATH));
    parameters.put("nodeTypeNodes", resources.get(ExtensionGenerator.ECM_NODETYPE_PATH));
    parameters.put("registryNodes", resources.get(ExtensionGenerator.REGISTRY_PATH));

    parameters.put("selectedResources", selectedResources);
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
    form.render(parameters);
  }

  @Resource
  public Response.Content<?> exportExtensionWAR() throws IOException {
    try {
      InputStream inputStream = extensionGeneratorService.generateWARExtension(selectedResources);
      return Response.ok(inputStream).withMimeType("application/zip").withHeader("Content-Disposition", "filename=\"custom-extension.war\"");
    } catch (Exception e) {
      log.error(e);
      return Response.content(500, "Error occured while importing resource. See full stack trace in log file");
    }
  }
}
