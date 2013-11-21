package org.exoplatform.management.portlet;

import juzu.*;
import juzu.impl.request.Request;
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

  public static final String OPERATION_IMPORT_PREFIX = "IMPORT";
  public static final String OPERATION_EXPORT_PREFIX = "EXPORT";
  public static final String PARAM_PREFIX_OPTION = "staging-option:";


  /** */
  StagingService stagingService;

  /** */
  SynchronizationService synchronizationService;

  @Inject
  @Path("selectedResources.gtmpl")
  Template selectedResourcesTmpl;

  @Inject
  @Path("index.gtmpl")
  Template indexTmpl;

  static List<ResourceCategory> resourceCategories = new ArrayList<ResourceCategory>();

  static {
    // RESOURCES CATEGORIES
    ResourceCategory contents = new ResourceCategory("Contents", null);
    contents.getSubResourceCategories().add(new ResourceCategory("Sites Contents", StagingService.CONTENT_SITES_PATH));
    resourceCategories.add(contents);

    ResourceCategory sites = new ResourceCategory("Sites", null);
    sites.getSubResourceCategories().add(new ResourceCategory("Portal Sites", StagingService.SITES_PORTAL_PATH));
    sites.getSubResourceCategories().add(new ResourceCategory("Group Sites", StagingService.SITES_GROUP_PATH));
    sites.getSubResourceCategories().add(new ResourceCategory("User Sites", StagingService.SITES_USER_PATH));
    resourceCategories.add(sites);

    ResourceCategory organization = new ResourceCategory("Organization", null);
    organization.getSubResourceCategories().add(new ResourceCategory("Users", StagingService.USERS_PATH));
    organization.getSubResourceCategories().add(new ResourceCategory("Groups", StagingService.GROUPS_PATH));
    organization.getSubResourceCategories().add(new ResourceCategory("Roles", StagingService.ROLE_PATH));
    resourceCategories.add(organization);

    ResourceCategory applications = new ResourceCategory("Applications", null);
    applications.getSubResourceCategories().add(new ResourceCategory("Applications Registry", StagingService.REGISTRY_PATH));
    applications.getSubResourceCategories().add(new ResourceCategory("Gadgets", StagingService.GADGET_PATH));
    resourceCategories.add(applications);

    ResourceCategory ecmAdmin = new ResourceCategory("ECM Admin", null);
    ecmAdmin.getSubResourceCategories().add(new ResourceCategory("Content List Templates", StagingService.ECM_TEMPLATES_APPLICATION_CLV_PATH));
    ecmAdmin.getSubResourceCategories().add(new ResourceCategory("Search Templates", StagingService.ECM_TEMPLATES_APPLICATION_SEARCH_PATH));
    ecmAdmin.getSubResourceCategories().add(new ResourceCategory("Document Type templates", StagingService.ECM_TEMPLATES_DOCUMENT_TYPE_PATH));
    ecmAdmin.getSubResourceCategories().add(new ResourceCategory("Metadata Templates", StagingService.ECM_TEMPLATES_METADATA_PATH));
    ecmAdmin.getSubResourceCategories().add(new ResourceCategory("Taxonomies", StagingService.ECM_TAXONOMY_PATH));
    ecmAdmin.getSubResourceCategories().add(new ResourceCategory("Queries", StagingService.ECM_QUERY_PATH));
    ecmAdmin.getSubResourceCategories().add(new ResourceCategory("Drives", StagingService.ECM_DRIVE_PATH));
    ecmAdmin.getSubResourceCategories().add(new ResourceCategory("ECMS Groovy Script", StagingService.ECM_SCRIPT_PATH));
    ecmAdmin.getSubResourceCategories().add(new ResourceCategory("Sites Explorer View Templates", StagingService.ECM_VIEW_TEMPLATES_PATH));
    ecmAdmin.getSubResourceCategories().add(new ResourceCategory("Sites Explorer View Configuration", StagingService.ECM_VIEW_CONFIGURATION_PATH));
    ecmAdmin.getSubResourceCategories().add(new ResourceCategory("Action NodeTypes", StagingService.ECM_ACTION_PATH));
    ecmAdmin.getSubResourceCategories().add(new ResourceCategory("NodeTypes", StagingService.ECM_NODETYPE_PATH));
    resourceCategories.add(ecmAdmin);
  }

  @Inject
  public StagingExtensionController(StagingService stagingService, SynchronizationService synchronizationService, ChromatticService chromatticService) {
    this.stagingService = stagingService;
    this.synchronizationService = synchronizationService;

    // FIXME Need to pass through the Controller to be able to inject the ChromatticService
    // Can't do it directly in the SynchronizationService. Need to figure out how to do it.
    synchronizationService.init(chromatticService);
  }

  @View
  public Response.Render index() {
    Map<String, Object> parameters = new HashMap<String, Object>();

    parameters.put("resourceCategories", resourceCategories);

    return indexTmpl.ok(parameters);
  }

  @Ajax
  @juzu.Resource
  public Response getCategories() {
    StringBuilder jsonCategories = new StringBuilder(50);
    jsonCategories.append("{\"categories\":[");

    for(ResourceCategory category : resourceCategories) {
      jsonCategories.append("{\"path\":\"")
              .append(category.getPath())
              .append("\",\"label\":\"")
              .append(category.getLabel())
              .append("\",\"subcategories\":[");
      for(ResourceCategory subcategory : category.getSubResourceCategories()) {
        jsonCategories.append("{\"path\":\"")
                .append(subcategory.getPath())
                .append("\",\"label\":\"")
                .append(subcategory.getLabel())
                .append("\"},");
      }
      if(!category.getSubResourceCategories().isEmpty()) {
        jsonCategories.deleteCharAt(jsonCategories.length()-1);
      }
      jsonCategories.append("]},");
    }
    if(!resourceCategories.isEmpty()) {
      jsonCategories.deleteCharAt(jsonCategories.length()-1);
    }
    jsonCategories.append("]}");

    return Response.ok(jsonCategories.toString());
  }

  @Ajax
  @juzu.Resource
  public Response getResourcesOfCategory(String path) {
    Set<Resource> resources = stagingService.getResources(path);

    StringBuilder jsonResources = new StringBuilder(50);
    jsonResources.append("{\"resources\":[");

    for(Resource resource : resources) {
      jsonResources.append("{\"path\":\"")
                .append(resource.getPath())
                .append("\",\"description\":\"")
                .append(resource.getDescription())
                .append("\",\"text\":\"")
                .append(resource.getText())
                .append("\"},");
    }
    if(!resources.isEmpty()) {
      jsonResources.deleteCharAt(jsonResources.length()-1);
    }
    jsonResources.append("]}");

    return Response.ok(jsonResources.toString());
  }

  @Ajax
  @juzu.Resource
  public Response.Content<?> importResources(FileItem file) throws IOException {
    if (file == null || file.getSize() == 0) {
      return Response.content(500, "File is empty.");
    }

    Map<String, String[]> parameters = Request.getCurrent().getParameters();
    String[] selectedResourcesCategories = parameters.get("staging:resourceCategory");

    if (selectedResourcesCategories == null || selectedResourcesCategories.length == 0) {
      return Response.content(500, "You must select a resource category.");
    }
    if(selectedResourcesCategories.length > 1) {
      return Response.content(500, "Only one resource can be imported at a time.");
    }

    try {
      Map<String, List<String>> attributes = new HashMap<String, List<String>>();
      for(String param : parameters.keySet()) {
        if(param.startsWith(PARAM_PREFIX_OPTION)) {
          attributes.put(param.substring(PARAM_PREFIX_OPTION.length()), Arrays.asList(parameters.get(param)));
        }
      }

      stagingService.importResource(selectedResourcesCategories[0], file, attributes);
      return Response.ok("Successfully proceeded!");
    } catch (Exception e) {
      log.error("Error occured while importing content", e);
      return Response.content(500, "Error occured while importing resource. See full stack trace in log file.");
    }
  }

  @Ajax
  @juzu.Resource
  @Route("/servers")
  public Response getSynchonizationServers() {
    List<TargetServer> synchronizationServers;
    try {
      synchronizationServers = synchronizationService.getSynchonizationServers();
    } catch (Exception e) {
      log.error("Error while fetching synchronization target servers", e);
      synchronizationServers = new ArrayList<TargetServer>();
    }

    StringBuilder jsonServers = new StringBuilder(50);
    jsonServers.append("{\"synchronizationServers\":[");
    for(TargetServer targetServer : synchronizationServers) {
      jsonServers.append("{\"id\":\"")
        .append(targetServer.getId())
        .append("\",\"name\":\"")
        .append(targetServer.getName())
        .append("\",\"host\":\"")
        .append(targetServer.getHost())
        .append("\",\"port\":\"")
        .append(targetServer.getPort())
        .append("\",\"username\":\"")
        .append(targetServer.getUsername())
        .append("\",\"password\":\"")
        .append(targetServer.getPassword())
        .append("\",\"ssl\":")
        .append(targetServer.isSsl())
        .append("},");
    }
    if(!synchronizationServers.isEmpty()) {
      jsonServers.deleteCharAt(jsonServers.length()-1);
    }
    jsonServers.append("]}");

    return Response.ok(jsonServers.toString());
  }

  @Ajax
  @Action
  public void addSynchonizationServer(String name, String host, String port, String username, String password, String ssl) {
    TargetServer targetServer = new TargetServer(name, host, port, username, password, "true".equals(ssl));

    synchronizationService.addSynchonizationServer(targetServer);
  }

  @Ajax
  @Action
  public void removeSynchonizationServer(String id) {
    TargetServer targetServer = new TargetServer(id, null, null, null, null, null, false);

    synchronizationService.removeSynchonizationServer(targetServer);
  }

  @Ajax
  @juzu.Resource
  public Response synchronize(String isSSLString, String host, String port, String username, String password, String[] resourceCategories, String[] resources, String[] options) throws IOException {
    TargetServer targetServer = new TargetServer(host, port, username, password, "true".equals(isSSLString));

    // Create selected resources categories
    List<ResourceCategory> selectedResourceCategories = new ArrayList<ResourceCategory>();
    for(String selectedResourcesCategory : resourceCategories) {
      ResourceCategory resourceCategory = new ResourceCategory(selectedResourcesCategory);
      selectedResourceCategories.add(resourceCategory);
    }

    // Dispatch selected resources in resources categories
    for(String selectedResource : resources) {
      for(ResourceCategory resourceCategory : selectedResourceCategories) {
        if(selectedResource.startsWith(resourceCategory.getPath())) {
          resourceCategory.getResources().add(new Resource(selectedResource, null, null));
          break;
        }
      }
    }

    // Dispatch selected options in resources categories
    for(String selectedOption : options) {
      int indexColon = selectedOption.indexOf(":");
      if(indexColon > 0) {
        String optionName = selectedOption.substring(0, indexColon);
        String optionValue = selectedOption.substring(indexColon + 1);

        String optionParts[] = optionName.split("_");
        for(ResourceCategory resourceCategory : selectedResourceCategories) {
          if(optionParts[0].equals(resourceCategory.getPath())) {
            if(optionParts[1].equals(OPERATION_EXPORT_PREFIX)) {
              resourceCategory.getExportOptions().put(optionParts[2], optionValue);
            } else if(optionParts[1].equals(OPERATION_IMPORT_PREFIX)) {
              resourceCategory.getImportOptions().put(optionParts[2], optionValue);
            }
            break;
          }
        }
      }
    }

    try {
      synchronizationService.synchronize(selectedResourceCategories, targetServer);
      return Response.ok("Successfully proceeded.");
    } catch (Exception e) {
      log.error("Error while synchronization, ", e);
      return Response.content(500, "Error occured while synchronizing Managed Resources: " + e.getMessage());
    }
  }

  @Ajax
  @juzu.Resource
  public Response executeSQL(String sql, String[] sites) throws IOException {
    try {
      Set<String> resultedNodePaths = stagingService.executeSQL(sql, new HashSet<String>(Arrays.asList(sites)));
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
      return Response.content(500, "Error while executing request: " + e.getMessage());
    }
  }
}
