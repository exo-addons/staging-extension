package org.exoplatform.management.portlet;

import juzu.*;
import juzu.template.Template;
import org.apache.commons.fileupload.FileItem;
import org.exoplatform.commons.juzu.ajax.Ajax;
import org.exoplatform.management.service.api.*;
import org.exoplatform.management.service.api.Resource;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import javax.inject.Inject;
import java.io.IOException;
import java.util.*;

@SessionScoped
public class StagingExtensionController {
  private static Log log = ExoLogger.getLogger(StagingExtensionController.class);

  @Inject
  StagingService stagingService;

  @Inject
  SynchronizationService synchronizationService;

  @Inject
  @Path("selectedResources.gtmpl")
  Template selectedResourcesTmpl;

  @Inject
  @Path("index.gtmpl")
  Template indexTmpl;

  /** */
  Set<String> selectedResourcesCategories = Collections.synchronizedSet(new HashSet<String>());

  /** */
  Set<String> selectedResources = Collections.synchronizedSet(new HashSet<String>());

  /** */
  Map<String, String> selectedOptions = new Hashtable<String, String>();

  /** */
  Map<String, Set<Resource>> availableResources = new HashMap<String, Set<Resource>>();

  static List<ResourceCategory> resourceCategories = new ArrayList<ResourceCategory>();
  static Map<String, Object> parameters = new HashMap<String, Object>();

  static {
    // RESOURCES CATEGORIES
    ResourceCategory contents = new ResourceCategory("contents", "Contents", null);
    contents.getSubResourceCategories().add(new ResourceCategory("siteContentPath", "Sites Contents", StagingService.CONTENT_SITES_PATH));
    resourceCategories.add(contents);

    ResourceCategory sites = new ResourceCategory("sites", "Sites", null);
    sites.getSubResourceCategories().add(new ResourceCategory("portalSitePath", "Portal Sites", StagingService.SITES_PORTAL_PATH));
    sites.getSubResourceCategories().add(new ResourceCategory("groupSitePath", "Group Sites", StagingService.SITES_GROUP_PATH));
    sites.getSubResourceCategories().add(new ResourceCategory("userSitePath", "User Sites", StagingService.SITES_USER_PATH));
    resourceCategories.add(sites);

    ResourceCategory organization = new ResourceCategory("organization", "Organization", null);
    organization.getSubResourceCategories().add(new ResourceCategory("userPath", "Users", StagingService.USERS_PATH));
    organization.getSubResourceCategories().add(new ResourceCategory("groupPath", "Groups", StagingService.GROUPS_PATH));
    organization.getSubResourceCategories().add(new ResourceCategory("rolePath", "Roles", StagingService.ROLE_PATH));
    resourceCategories.add(organization);

    ResourceCategory applications = new ResourceCategory("applications", "Applications", null);
    //organization.getSubResourceCategories().add(new ResourceCategory("gadgetPath", "Gadgets", SynchronizationService.GADGET_PATH));
    applications.getSubResourceCategories().add(new ResourceCategory("applicationRegistryPath", "Applications Registry", StagingService.REGISTRY_PATH));
    resourceCategories.add(applications);

    ResourceCategory ecmAdmin = new ResourceCategory("ecmAdmin", "ECM Admin", null);
    ecmAdmin.getSubResourceCategories().add(new ResourceCategory("applicationCLVTemplatesPath", "Content List Templates", StagingService.ECM_TEMPLATES_APPLICATION_CLV_PATH));
    ecmAdmin.getSubResourceCategories().add(new ResourceCategory("applicationSearchTemplatesPath", "Search Templates", StagingService.ECM_TEMPLATES_APPLICATION_SEARCH_PATH));
    ecmAdmin.getSubResourceCategories().add(new ResourceCategory("documentTypeTemplatesPath", "Document Type templates", StagingService.ECM_TEMPLATES_DOCUMENT_TYPE_PATH));
    ecmAdmin.getSubResourceCategories().add(new ResourceCategory("metadataTemplatesPath", "Metadata Templates", StagingService.ECM_TEMPLATES_METADATA_PATH));
    ecmAdmin.getSubResourceCategories().add(new ResourceCategory("taxonomyPath", "Taxonomies", StagingService.ECM_TAXONOMY_PATH));
    ecmAdmin.getSubResourceCategories().add(new ResourceCategory("queryPath", "Queries", StagingService.ECM_QUERY_PATH));
    ecmAdmin.getSubResourceCategories().add(new ResourceCategory("drivePath", "Drives", StagingService.ECM_DRIVE_PATH));
    ecmAdmin.getSubResourceCategories().add(new ResourceCategory("scriptPath", "Programming Groovy Script", StagingService.ECM_SCRIPT_PATH));
    ecmAdmin.getSubResourceCategories().add(new ResourceCategory("viewTemplatePath", "Sites Explorer View Templates", StagingService.ECM_VIEW_TEMPLATES_PATH));
    ecmAdmin.getSubResourceCategories().add(new ResourceCategory("viewConfigurationPath", "Sites Explorer View Configuration", StagingService.ECM_VIEW_CONFIGURATION_PATH));
    ecmAdmin.getSubResourceCategories().add(new ResourceCategory("actionNodeTypePath", "Action JCR NodeType", StagingService.ECM_ACTION_PATH));
    ecmAdmin.getSubResourceCategories().add(new ResourceCategory("nodeTypePath", "JCR NodeType", StagingService.ECM_NODETYPE_PATH));
    resourceCategories.add(ecmAdmin);

    parameters.put("resourceCategories", resourceCategories);
  }

  @View
  public Response.Render index() {
    // Clear selection
    selectedResources.clear();
    selectedOptions.clear();

    // NODES
    availableResources.put(StagingService.SITES_PORTAL_PATH, stagingService.getPortalSiteResources());
    availableResources.put(StagingService.SITES_GROUP_PATH, stagingService.getGroupSiteResources());
    availableResources.put(StagingService.SITES_USER_PATH, stagingService.getUserSiteResources());
    availableResources.put(StagingService.CONTENT_SITES_PATH, stagingService.getSiteContentResources());
    availableResources.put(StagingService.ECM_TEMPLATES_APPLICATION_CLV_PATH, stagingService.getApplicationCLVTemplatesResources());
    availableResources.put(StagingService.ECM_TEMPLATES_APPLICATION_SEARCH_PATH, stagingService.getApplicationSearchTemplatesResources());
    availableResources.put(StagingService.ECM_TEMPLATES_DOCUMENT_TYPE_PATH, stagingService.getDocumentTypeTemplatesResources());
    availableResources.put(StagingService.ECM_TEMPLATES_METADATA_PATH, stagingService.getMetadataTemplatesResources());
    availableResources.put(StagingService.ECM_TAXONOMY_PATH, stagingService.getTaxonomyResources());
    availableResources.put(StagingService.ECM_QUERY_PATH, stagingService.getQueryResources());
    availableResources.put(StagingService.ECM_DRIVE_PATH, stagingService.getDriveResources());
    availableResources.put(StagingService.ECM_SCRIPT_PATH, stagingService.getScriptResources());
    availableResources.put(StagingService.ECM_ACTION_PATH, stagingService.getActionNodeTypeResources());
    availableResources.put(StagingService.ECM_NODETYPE_PATH, stagingService.getNodeTypeResources());
    availableResources.put(StagingService.REGISTRY_PATH, stagingService.getRegistryResources());
    availableResources.put(StagingService.ECM_VIEW_TEMPLATES_PATH, stagingService.getViewTemplatesResources());
    availableResources.put(StagingService.ECM_VIEW_CONFIGURATION_PATH, stagingService.getViewConfigurationResources());
    availableResources.put(StagingService.USERS_PATH, stagingService.getUserResources());
    availableResources.put(StagingService.GROUPS_PATH, stagingService.getGroupResources());
    availableResources.put(StagingService.ROLE_PATH, stagingService.getRoleResources());

    parameters.put("availableResources", availableResources);

    parameters.put("selectedResources", selectedResources);

    parameters.put("portalSiteSelectedNodes", getSelectedResources(StagingService.SITES_PORTAL_PATH));
    parameters.put("groupSiteSelectedNodes", getSelectedResources(StagingService.SITES_GROUP_PATH));
    parameters.put("userSiteSelectedNodes", getSelectedResources(StagingService.SITES_USER_PATH));
    parameters.put("siteContentSelectedNodes", getSelectedResources(StagingService.CONTENT_SITES_PATH));
    parameters.put("applicationCLVTemplatesSelectedNodes", getSelectedResources(StagingService.ECM_TEMPLATES_APPLICATION_CLV_PATH));
    parameters.put("applicationSearchTemplatesSelectedNodes", getSelectedResources(StagingService.ECM_TEMPLATES_APPLICATION_SEARCH_PATH));
    parameters.put("documentTypeTemplatesSelectedNodes", getSelectedResources(StagingService.ECM_TEMPLATES_DOCUMENT_TYPE_PATH));
    parameters.put("metadataTemplatesSelectedNodes", getSelectedResources(StagingService.ECM_TEMPLATES_METADATA_PATH));
    parameters.put("taxonomySelectedNodes", getSelectedResources(StagingService.ECM_TAXONOMY_PATH));
    parameters.put("querySelectedNodes", getSelectedResources(StagingService.ECM_QUERY_PATH));
    parameters.put("driveSelectedNodes", getSelectedResources(StagingService.ECM_DRIVE_PATH));
    parameters.put("scriptSelectedNodes", getSelectedResources(StagingService.ECM_SCRIPT_PATH));
    parameters.put("actionNodeTypeSelectedNodes", getSelectedResources(StagingService.ECM_ACTION_PATH));
    parameters.put("nodeTypeSelectedNodes", getSelectedResources(StagingService.ECM_NODETYPE_PATH));
    parameters.put("registrySelectedNodes", getSelectedResources(StagingService.REGISTRY_PATH));
    parameters.put("viewTemplateSelectedNodes", getSelectedResources(StagingService.ECM_VIEW_TEMPLATES_PATH));
    parameters.put("viewConfigurationSelectedNodes", getSelectedResources(StagingService.ECM_VIEW_CONFIGURATION_PATH));
    parameters.put("userSelectedNodes", getSelectedResources(StagingService.USERS_PATH));
    parameters.put("groupSelectedNodes", getSelectedResources(StagingService.GROUPS_PATH));
    parameters.put("roleSelectedNodes", getSelectedResources(StagingService.ROLE_PATH));

    parameters.put("selectedOptions", selectedOptions);

    return indexTmpl.ok(parameters);
  }

  @Ajax
  @juzu.Resource
  public void selectResources(String path, String checked) {
    if (checked != null && path != null && !checked.isEmpty() && !path.isEmpty()) {
      if (checked.equals("true")) {
        selectedResourcesCategories.add(path);
        if (availableResources.containsKey(path)) {
          Set<Resource> children = availableResources.get(path);
          for (Resource resource : children) {
            selectedResources.add(resource.getPath());
          }
        } else {
          selectedResources.add(path);
        }
      } else {
        selectedResourcesCategories.remove(path);
        if (availableResources.containsKey(path)) {
          Set<Resource> children = availableResources.get(path);
          for (Resource resource : children) {
            selectedResources.remove(resource.getPath());
          }
        } else {
          selectedResources.remove(path);
        }
      }
    } else {
      log.warn("Selection not considered.");
    }

    parameters.put("portalSiteSelectedNodes", getSelectedResources(StagingService.SITES_PORTAL_PATH));
    parameters.put("groupSiteSelectedNodes", getSelectedResources(StagingService.SITES_GROUP_PATH));
    parameters.put("userSiteSelectedNodes", getSelectedResources(StagingService.SITES_USER_PATH));
    parameters.put("siteContentSelectedNodes", getSelectedResources(StagingService.CONTENT_SITES_PATH));
    parameters.put("applicationCLVTemplatesSelectedNodes", getSelectedResources(StagingService.ECM_TEMPLATES_APPLICATION_CLV_PATH));
    parameters.put("applicationSearchTemplatesSelectedNodes", getSelectedResources(StagingService.ECM_TEMPLATES_APPLICATION_SEARCH_PATH));
    parameters.put("documentTypeTemplatesSelectedNodes", getSelectedResources(StagingService.ECM_TEMPLATES_DOCUMENT_TYPE_PATH));
    parameters.put("metadataTemplatesSelectedNodes", getSelectedResources(StagingService.ECM_TEMPLATES_METADATA_PATH));
    parameters.put("taxonomySelectedNodes", getSelectedResources(StagingService.ECM_TAXONOMY_PATH));
    parameters.put("querySelectedNodes", getSelectedResources(StagingService.ECM_QUERY_PATH));
    parameters.put("driveSelectedNodes", getSelectedResources(StagingService.ECM_DRIVE_PATH));
    parameters.put("scriptSelectedNodes", getSelectedResources(StagingService.ECM_SCRIPT_PATH));
    parameters.put("actionNodeTypeSelectedNodes", getSelectedResources(StagingService.ECM_ACTION_PATH));
    parameters.put("nodeTypeSelectedNodes", getSelectedResources(StagingService.ECM_NODETYPE_PATH));
    parameters.put("registrySelectedNodes", getSelectedResources(StagingService.REGISTRY_PATH));
    parameters.put("viewTemplateSelectedNodes", getSelectedResources(StagingService.ECM_VIEW_TEMPLATES_PATH));
    parameters.put("viewConfigurationSelectedNodes", getSelectedResources(StagingService.ECM_VIEW_CONFIGURATION_PATH));
    parameters.put("userSelectedNodes", getSelectedResources(StagingService.USERS_PATH));
    parameters.put("groupSelectedNodes", getSelectedResources(StagingService.GROUPS_PATH));
    parameters.put("roleSelectedNodes", getSelectedResources(StagingService.ROLE_PATH));

    parameters.put("selectedResources", selectedResources);
    parameters.put("selectedOptions", selectedOptions);

    selectedResourcesTmpl.render(parameters);
  }

  @Ajax
  @juzu.Resource
  public void selectOption(String name, String value) {
    if (value == null || value.trim().isEmpty()) {
      selectedOptions.remove(name);
    } else {
      selectedOptions.put(name, value);
    }
  }

  @Ajax
  @juzu.Resource
  public Response.Content<?> importResources(FileItem file) throws IOException {
    if (file == null || file.getSize() == 0) {
      return Response.content(500, "File is empty.");
    }
    if (selectedResourcesCategories == null || selectedResourcesCategories.isEmpty()) {
      return Response.content(500, "You must select a resource category.");
    }
    if(selectedResourcesCategories.size() > 1) {
      return Response.content(500, "Only one resource can be imported at a time.");
    }
    try {
      stagingService.importResource(selectedResourcesCategories.iterator().next(), file);
      return Response.ok("Successfully proceeded!");
    } catch (Exception e) {
      log.error("Error occured while importing content", e);
      return Response.content(500, "Error occured while importing resource. See full stack trace in log file.");
    }
  }

  @Ajax
  @juzu.Resource
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
  @juzu.Resource
  public Response executeSQL(String sql) throws IOException {
    try {
      Set<String> resultedNodePaths = stagingService.executeSQL(sql, selectedResources);
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
    Set<String> resources = stagingService.filterSelectedResources(selectedResources, parentPath);
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
