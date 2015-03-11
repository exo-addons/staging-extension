package org.exoplatform.management.portlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.inject.Inject;

import juzu.Action;
import juzu.Path;
import juzu.Response;
import juzu.Route;
import juzu.SessionScoped;
import juzu.View;
import juzu.impl.request.Request;
import juzu.template.Template;

import org.apache.commons.fileupload.FileItem;
import org.exoplatform.commons.juzu.ajax.Ajax;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.management.service.api.Resource;
import org.exoplatform.management.service.api.ResourceCategory;
import org.exoplatform.management.service.api.ResourceHandler;
import org.exoplatform.management.service.api.StagingService;
import org.exoplatform.management.service.api.SynchronizationService;
import org.exoplatform.management.service.api.TargetServer;
import org.exoplatform.management.service.handler.ResourceHandlerLocator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.gatein.management.api.ManagementService;

@SessionScoped
public class StagingExtensionController {
  private static Log log = ExoLogger.getLogger(StagingExtensionController.class);

  public static final String OPERATION_IMPORT_PREFIX = "IMPORT";
  public static final String OPERATION_EXPORT_PREFIX = "EXPORT";
  public static final String PARAM_PREFIX_OPTION = "staging-option:";

  public static final Map<String, String> IMPORT_ZIP_PATH_EXCEPTIONS = new HashMap<String, String>();
  public static final Map<String, String> IMPORT_PATH_EXCEPTIONS = new HashMap<String, String>();

  @Inject
  StagingService stagingService;

  @Inject
  SynchronizationService synchronizationService;

  @Inject
  ManagementService managementService;

  @Inject
  @Path("selectedResources.gtmpl")
  Template selectedResourcesTmpl;

  @Inject
  @Path("index.gtmpl")
  Template indexTmpl;

  private static final List<ResourceCategory> resourceCategories = new ArrayList<ResourceCategory>();
  private static int SPACES_CATEGORY_INDEX = -1;
  private static int FORUM_CATEGORY_INDEX = -1;
  private static int ANSWER_CATEGORY_INDEX = -1;
  private static int CALENDAR_CATEGORY_INDEX = -1;
  private static int WIKI_CATEGORY_INDEX = -1;
  private static int COUNT_ALL = 0;

  static {
    String activatedModules = System.getProperty("exo.staging.portlet.modules", "");
    activatedModules = activatedModules == null ? "" : activatedModules.trim();

    // ZIP PATH EXCEPTIONS
    IMPORT_ZIP_PATH_EXCEPTIONS.put("/portal", "/site/portalsites");
    IMPORT_ZIP_PATH_EXCEPTIONS.put("/group", "/site/groupsites");
    IMPORT_ZIP_PATH_EXCEPTIONS.put("/user", "/site/usersites");

    // PATH EXCEPTIONS
    IMPORT_PATH_EXCEPTIONS.put("/site/portalsites", "/site");
    IMPORT_PATH_EXCEPTIONS.put("/site/groupsites", "/site");
    IMPORT_PATH_EXCEPTIONS.put("/site/usersites", "/site");

    // RESOURCES CATEGORIES
    if (activatedModules.isEmpty() || activatedModules.contains("/site:activated")) {
      ResourceCategory sites = new ResourceCategory("Sites", "/site");
      if (activatedModules.isEmpty() || activatedModules.contains(StagingService.SITES_PORTAL_PATH + ":activated")) {
        sites.getSubResourceCategories().add(new ResourceCategory("Portal Sites", StagingService.SITES_PORTAL_PATH));
      }
      if (activatedModules.isEmpty() || activatedModules.contains(StagingService.SITES_GROUP_PATH + ":activated")) {
        sites.getSubResourceCategories().add(new ResourceCategory("Group Sites", StagingService.SITES_GROUP_PATH));
      }
      if (activatedModules.isEmpty() || activatedModules.contains(StagingService.SITES_USER_PATH + ":activated")) {
        sites.getSubResourceCategories().add(new ResourceCategory("User Sites", StagingService.SITES_USER_PATH));
      }
      resourceCategories.add(sites);
    }

    if (activatedModules.isEmpty() || activatedModules.contains(StagingService.SOCIAL_PARENT_PATH + ":activated")) {
      ResourceCategory social = new ResourceCategory("Social", StagingService.SOCIAL_PARENT_PATH);
      if (activatedModules.isEmpty() || activatedModules.contains(StagingService.SOCIAL_SPACE_PATH + ":activated")) {
        social.getSubResourceCategories().add(new ResourceCategory("Spaces", StagingService.SOCIAL_SPACE_PATH));
      }
      resourceCategories.add(social);
      SPACES_CATEGORY_INDEX = resourceCategories.size() - 1;
    }

    if (activatedModules.isEmpty() || activatedModules.contains("/content:activated")) {
      ResourceCategory contents = new ResourceCategory("Contents", "/content");
      if (activatedModules.isEmpty() || activatedModules.contains(StagingService.CONTENT_SITES_PATH + ":activated")) {
        contents.getSubResourceCategories().add(new ResourceCategory("Sites Contents", StagingService.CONTENT_SITES_PATH));
      }
      resourceCategories.add(contents);
    }

    if (activatedModules.isEmpty() || activatedModules.contains(StagingService.ANSWERS_PARENT_PATH + ":activated")) {
      ResourceCategory answers = new ResourceCategory("Answers", StagingService.ANSWERS_PARENT_PATH);
      if (activatedModules.isEmpty() || activatedModules.contains(StagingService.PUBLIC_ANSWER_PATH + ":activated")) {
        answers.getSubResourceCategories().add(new ResourceCategory("Public Answers", StagingService.PUBLIC_ANSWER_PATH));
      }
      if (activatedModules.isEmpty() || activatedModules.contains(StagingService.SPACE_ANSWER_PATH + ":activated")) {
        answers.getSubResourceCategories().add(new ResourceCategory("Space Answers", StagingService.SPACE_ANSWER_PATH));
      }
      if (activatedModules.isEmpty() || activatedModules.contains(StagingService.FAQ_TEMPLATE_PATH + ":activated")) {
        answers.getSubResourceCategories().add(new ResourceCategory("FAQ Template", StagingService.FAQ_TEMPLATE_PATH));
      }
      resourceCategories.add(answers);
      ANSWER_CATEGORY_INDEX = resourceCategories.size() - 1;
    }

    if (activatedModules.isEmpty() || activatedModules.contains(StagingService.FORUMS_PARENT_PATH + ":activated")) {
      ResourceCategory forums = new ResourceCategory("Forums", StagingService.FORUMS_PARENT_PATH);
      if (activatedModules.isEmpty() || activatedModules.contains(StagingService.PUBLIC_FORUM_PATH + ":activated")) {
        forums.getSubResourceCategories().add(new ResourceCategory("Public Forum", StagingService.PUBLIC_FORUM_PATH));
      }
      if (activatedModules.isEmpty() || activatedModules.contains(StagingService.SPACE_FORUM_PATH + ":activated")) {
        forums.getSubResourceCategories().add(new ResourceCategory("Space Forum", StagingService.SPACE_FORUM_PATH));
      }
      if (activatedModules.isEmpty() || activatedModules.contains(StagingService.FORUM_SETTINGS + ":activated")) {
        forums.getSubResourceCategories().add(new ResourceCategory("Forum settings", StagingService.FORUM_SETTINGS));
      }
      resourceCategories.add(forums);
      FORUM_CATEGORY_INDEX = resourceCategories.size() - 1;
    }

    if (activatedModules.isEmpty() || activatedModules.contains(StagingService.CALENDARS_PARENT_PATH + ":activated")) {
      ResourceCategory calendars = new ResourceCategory("Calendars", StagingService.CALENDARS_PARENT_PATH);
      if (activatedModules.isEmpty() || activatedModules.contains(StagingService.GROUP_CALENDAR_PATH + ":activated")) {
        calendars.getSubResourceCategories().add(new ResourceCategory("Group Calendar", StagingService.GROUP_CALENDAR_PATH));
      }
      if (activatedModules.isEmpty() || activatedModules.contains(StagingService.SPACE_CALENDAR_PATH + ":activated")) {
        calendars.getSubResourceCategories().add(new ResourceCategory("Space Calendar", StagingService.SPACE_CALENDAR_PATH));
      }
      if (activatedModules.isEmpty() || activatedModules.contains(StagingService.PERSONAL_FORUM_PATH + ":activated")) {
        calendars.getSubResourceCategories().add(new ResourceCategory("Personal Calendar", StagingService.PERSONAL_FORUM_PATH));
      }
      resourceCategories.add(calendars);
      CALENDAR_CATEGORY_INDEX = resourceCategories.size() - 1;
    }

    if (activatedModules.isEmpty() || activatedModules.contains(StagingService.WIKIS_PARENT_PATH + ":activated")) {
      ResourceCategory wikis = new ResourceCategory("Wikis", StagingService.WIKIS_PARENT_PATH);
      if (activatedModules.isEmpty() || activatedModules.contains(StagingService.PORTAL_WIKIS_PATH + ":activated")) {
        wikis.getSubResourceCategories().add(new ResourceCategory("Portal wikis", StagingService.PORTAL_WIKIS_PATH));
      }
      if (activatedModules.isEmpty() || activatedModules.contains(StagingService.GROUP_WIKIS_PATH + ":activated")) {
        wikis.getSubResourceCategories().add(new ResourceCategory("Space wikis", StagingService.GROUP_WIKIS_PATH));
      }
      if (activatedModules.isEmpty() || activatedModules.contains(StagingService.USER_WIKIS_PATH + ":activated")) {
        wikis.getSubResourceCategories().add(new ResourceCategory("User wikis", StagingService.USER_WIKIS_PATH));
      }
      resourceCategories.add(wikis);
      WIKI_CATEGORY_INDEX = resourceCategories.size() - 1;
    }

    if (activatedModules.isEmpty() || activatedModules.contains("/organization:activated")) {
      ResourceCategory organization = new ResourceCategory("Organization", "/organization");
      if (activatedModules.isEmpty() || activatedModules.contains(StagingService.USERS_PATH + ":activated")) {
        organization.getSubResourceCategories().add(new ResourceCategory("Users", StagingService.USERS_PATH));
      }
      if (activatedModules.isEmpty() || activatedModules.contains(StagingService.GROUPS_PATH + ":activated")) {
        organization.getSubResourceCategories().add(new ResourceCategory("Groups", StagingService.GROUPS_PATH));
      }
      if (activatedModules.isEmpty() || activatedModules.contains(StagingService.ROLE_PATH + ":activated")) {
        organization.getSubResourceCategories().add(new ResourceCategory("Roles", StagingService.ROLE_PATH));
      }
      resourceCategories.add(organization);
    }

    if (activatedModules.isEmpty() || activatedModules.contains("/application:activated")) {
      ResourceCategory applications = new ResourceCategory("Applications", "/application");
      if (activatedModules.isEmpty() || activatedModules.contains(StagingService.REGISTRY_PATH + ":activated")) {
        applications.getSubResourceCategories().add(new ResourceCategory("Applications Registry", StagingService.REGISTRY_PATH));
      }
      if (activatedModules.isEmpty() || activatedModules.contains(StagingService.GADGET_PATH + ":activated")) {
        applications.getSubResourceCategories().add(new ResourceCategory("Gadgets", StagingService.GADGET_PATH));
      }
      resourceCategories.add(applications);
    }

    if (activatedModules.isEmpty() || activatedModules.contains("/ecmadmin:activated")) {
      ResourceCategory ecmAdmin = new ResourceCategory("ECM Admin", "/ecmadmin");
      if (activatedModules.isEmpty() || activatedModules.contains(StagingService.ECM_TEMPLATES_APPLICATION_CLV_PATH + ":activated")) {
        ecmAdmin.getSubResourceCategories().add(new ResourceCategory("Content List Templates", StagingService.ECM_TEMPLATES_APPLICATION_CLV_PATH));
      }
      if (activatedModules.isEmpty() || activatedModules.contains(StagingService.ECM_TEMPLATES_APPLICATION_SEARCH_PATH + ":activated")) {
        ecmAdmin.getSubResourceCategories().add(new ResourceCategory("Search Templates", StagingService.ECM_TEMPLATES_APPLICATION_SEARCH_PATH));
      }
      if (activatedModules.isEmpty() || activatedModules.contains(StagingService.ECM_TEMPLATES_DOCUMENT_TYPE_PATH + ":activated")) {
        ecmAdmin.getSubResourceCategories().add(new ResourceCategory("Document Type templates", StagingService.ECM_TEMPLATES_DOCUMENT_TYPE_PATH));
      }
      if (activatedModules.isEmpty() || activatedModules.contains(StagingService.ECM_TEMPLATES_METADATA_PATH + ":activated")) {
        ecmAdmin.getSubResourceCategories().add(new ResourceCategory("Metadata Templates", StagingService.ECM_TEMPLATES_METADATA_PATH));
      }
      if (activatedModules.isEmpty() || activatedModules.contains(StagingService.ECM_TAXONOMY_PATH + ":activated")) {
        ecmAdmin.getSubResourceCategories().add(new ResourceCategory("Taxonomies", StagingService.ECM_TAXONOMY_PATH));
      }
      if (activatedModules.isEmpty() || activatedModules.contains(StagingService.ECM_QUERY_PATH + ":activated")) {
        ecmAdmin.getSubResourceCategories().add(new ResourceCategory("Queries", StagingService.ECM_QUERY_PATH));
      }
      if (activatedModules.isEmpty() || activatedModules.contains(StagingService.ECM_DRIVE_PATH + ":activated")) {
        ecmAdmin.getSubResourceCategories().add(new ResourceCategory("Drives", StagingService.ECM_DRIVE_PATH));
      }
      if (activatedModules.isEmpty() || activatedModules.contains(StagingService.ECM_SCRIPT_PATH + ":activated")) {
        ecmAdmin.getSubResourceCategories().add(new ResourceCategory("ECMS Groovy Script", StagingService.ECM_SCRIPT_PATH));
      }
      if (activatedModules.isEmpty() || activatedModules.contains(StagingService.ECM_VIEW_TEMPLATES_PATH + ":activated")) {
        ecmAdmin.getSubResourceCategories().add(new ResourceCategory("Sites Explorer View Templates", StagingService.ECM_VIEW_TEMPLATES_PATH));
      }
      if (activatedModules.isEmpty() || activatedModules.contains(StagingService.ECM_VIEW_CONFIGURATION_PATH + ":activated")) {
        ecmAdmin.getSubResourceCategories().add(new ResourceCategory("Sites Explorer View Configuration", StagingService.ECM_VIEW_CONFIGURATION_PATH));
      }
      if (activatedModules.isEmpty() || activatedModules.contains(StagingService.ECM_ACTION_PATH + ":activated")) {
        ecmAdmin.getSubResourceCategories().add(new ResourceCategory("Action NodeTypes", StagingService.ECM_ACTION_PATH));
      }
      if (activatedModules.isEmpty() || activatedModules.contains(StagingService.ECM_NODETYPE_PATH + ":activated")) {
        ecmAdmin.getSubResourceCategories().add(new ResourceCategory("NodeTypes", StagingService.ECM_NODETYPE_PATH));
      }
      resourceCategories.add(ecmAdmin);
    }

    COUNT_ALL = resourceCategories.size();
  }

  @View
  public Response.Render index() {
    if (resourceCategories.size() == COUNT_ALL) {

      if (!isWikiActivated() && WIKI_CATEGORY_INDEX > 0) {
        resourceCategories.remove(WIKI_CATEGORY_INDEX);
      }
      if (!isCalendarActivated() && CALENDAR_CATEGORY_INDEX > 0) {
        resourceCategories.remove(CALENDAR_CATEGORY_INDEX);
      }
      if (!isForumActivated() && FORUM_CATEGORY_INDEX > 0) {
        resourceCategories.remove(FORUM_CATEGORY_INDEX);
      }
      if (!isAnswerActivated() && ANSWER_CATEGORY_INDEX > 0) {
        resourceCategories.remove(ANSWER_CATEGORY_INDEX);
      }
      if (!isSocialActivated() && SPACES_CATEGORY_INDEX > 0) {
        resourceCategories.remove(SPACES_CATEGORY_INDEX);
      }
    }

    Map<String, Object> parameters = new HashMap<String, Object>();

    parameters.put("resourceCategories", resourceCategories);

    return indexTmpl.ok(parameters);
  }

  @Ajax
  @juzu.Resource
  public Response getCategories() {
    StringBuilder jsonCategories = new StringBuilder(50);
    jsonCategories.append("{\"categories\":[");

    for (ResourceCategory category : resourceCategories) {
      jsonCategories.append("{\"path\":\"").append(category.getPath()).append("\",\"label\":\"").append(category.getLabel()).append("\",\"subcategories\":[");
      for (ResourceCategory subcategory : category.getSubResourceCategories()) {
        jsonCategories.append("{\"path\":\"").append(subcategory.getPath()).append("\",\"label\":\"").append(subcategory.getLabel()).append("\"},");
      }
      if (!category.getSubResourceCategories().isEmpty()) {
        jsonCategories.deleteCharAt(jsonCategories.length() - 1);
      }
      jsonCategories.append("]},");
    }
    if (!resourceCategories.isEmpty()) {
      jsonCategories.deleteCharAt(jsonCategories.length() - 1);
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

    for (Resource resource : resources) {
      jsonResources.append("{\"path\":\"").append(resource.getPath()).append("\",\"description\":\"").append(resource.getDescription()).append("\",\"text\":\"").append(resource.getText())
          .append("\"},");
    }
    if (!resources.isEmpty()) {
      jsonResources.deleteCharAt(jsonResources.length() - 1);
    }
    jsonResources.append("]}");

    return Response.ok(jsonResources.toString());
  }

  @Ajax
  @juzu.Resource
  public Response.Content<?> prepareImportResources(FileItem file) throws IOException {
    if (file == null || file.getSize() == 0) {
      return Response.content(500, "File is empty");
    }

    ZipInputStream zipInputStream = new ZipInputStream(file.getInputStream());
    ZipEntry entry = zipInputStream.getNextEntry();
    Set<String> foundResources = new HashSet<String>();
    while (entry != null) {
      String fileName = entry.getName();
      if (entry.isDirectory()) {
        entry = zipInputStream.getNextEntry();
        continue;
      }
      fileName = fileName.startsWith("/") ? "" : "/" + fileName;
      String resourcePath = transformSpecialPath(fileName);
      // If resource path transformed and treated with Exceptions Resource Paths
      if (!resourcePath.equals(fileName)) {
        // Got it !
        foundResources.add(resourcePath);
        // Manage only one resource at a time for the moment
      } else if (!parentAlreadyAddedInList(foundResources, resourcePath)) {
        ResourceHandler resourceHandler = ResourceHandlerLocator.findResourceByPath(resourcePath);
        foundResources.add(resourceHandler.getPath());
      }
      entry = zipInputStream.getNextEntry();
    }

    log.info("Found resources zip file : {}", foundResources);

    zipInputStream.close();

    if (!foundResources.isEmpty()) {
      return Response.ok(toString(foundResources));
    } else {
      return Response.content(500, "Zip file does not contain known resources to import");
    }
  }

  @Ajax
  @juzu.Resource
  public Response.Content<?> export(String[] resourceCategories, String[] resources, String[] options) throws IOException {
    // Create selected resources categories
    List<ResourceCategory> selectedResourceCategories = new ArrayList<ResourceCategory>();
    for (String selectedResourcesCategory : resourceCategories) {
      ResourceCategory resourceCategory = new ResourceCategory(selectedResourcesCategory);
      selectedResourceCategories.add(resourceCategory);
    }

    // Dispatch selected resources in resources categories
    for (String selectedResource : resources) {
      for (ResourceCategory resourceCategory : selectedResourceCategories) {
        if (selectedResource.startsWith(resourceCategory.getPath())) {
          resourceCategory.getResources().add(new Resource(selectedResource, null, null));
          break;
        }
      }
    }

    // Dispatch selected options in resources categories
    for (String selectedOption : options) {
      int indexColon = selectedOption.indexOf(":");
      if (indexColon > 0) {
        String optionName = selectedOption.substring(0, indexColon);
        String optionValue = selectedOption.substring(indexColon + 1);

        String optionParts[] = optionName.split("_");
        for (ResourceCategory resourceCategory : selectedResourceCategories) {
          if (optionParts[0].equals(resourceCategory.getPath())) {
            if (optionParts[1].equals(OPERATION_EXPORT_PREFIX)) {
              resourceCategory.getExportOptions().put(optionParts[2], optionValue);
            } else if (optionParts[1].equals(OPERATION_IMPORT_PREFIX)) {
              resourceCategory.getImportOptions().put(optionParts[2], optionValue);
            }
            break;
          }
        }
      }
    }

    // Manage paths exceptions (/site/portalsites -> /site)
    List<ResourceCategory> selectedResourceCategoriesWithExceptions = new ArrayList<ResourceCategory>();
    for (ResourceCategory resourceCategory : selectedResourceCategories) {
      if (IMPORT_PATH_EXCEPTIONS.containsKey(resourceCategory.getPath())) {
        resourceCategory.setPath(IMPORT_PATH_EXCEPTIONS.get(resourceCategory.getPath()));
      }
      selectedResourceCategoriesWithExceptions.add(resourceCategory);
    }

    try {
      File file = stagingService.export(selectedResourceCategoriesWithExceptions);
      return Response.ok(new FileInputStream(file)).withMimeType("application/zip").withHeader("Set-Cookie", "fileDownload=true; path=/")
          .withHeader("Content-Disposition", "filename=\"StagingExport.zip\"");
    } catch (Exception e) {
      log.error("Error while exporting resources, ", e);
      return Response.content(500, "Error occured while exporting Managed Resources: " + e.getMessage());
    }
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

    List<String> selectedResourcesCategoriesList = new ArrayList<String>(Arrays.asList(selectedResourcesCategories));
    Collections.sort(selectedResourcesCategoriesList, new Comparator<String>() {
      @Override
      public int compare(String o1, String o2) {
        return ResourceCategory.getOrder(o1) - ResourceCategory.getOrder(o1);
      }
    });

    try {
      for (String selectedResourcesCategory : selectedResourcesCategoriesList) {
        log.info("inporting resources for : " + selectedResourcesCategory);
        String exceptionPathCategory = IMPORT_PATH_EXCEPTIONS.get(selectedResourcesCategory);

        // filter ResourceCategory options coming from request
        Map<String, List<String>> attributes = new HashMap<String, List<String>>();
        for (String param : parameters.keySet()) {
          if (param.startsWith(PARAM_PREFIX_OPTION)) {
            String paramPath = param.replace(PARAM_PREFIX_OPTION, "");
            if (parameters.get(param) == null || parameters.get(param).length == 0) {
              log.error("Can't parse empty parameters for filter: " + paramPath);
              continue;
            }
            String categoryPath = paramPath.substring(0, paramPath.indexOf("_"));
            if (categoryPath.equals(selectedResourcesCategory) || (exceptionPathCategory != null && categoryPath.equals(exceptionPathCategory))) {
              String attributeName = paramPath.substring(paramPath.indexOf("_") + 1);
              if (attributeName.startsWith("filter/")) {
                if (!attributes.containsKey("filter")) {
                  attributes.put("filter", new ArrayList<String>());
                }
                List<String> values = attributes.get("filter");
                if (parameters.get(param).length > 1) {
                  log.error("Can't parse all parameters for filter: '" + paramPath + "' with values = " + parameters.get(param) + ". Only first parameter will be used: " + parameters.get(param)[0]);
                }
                String value = parameters.get(param)[0];
                attributeName = attributeName.replace("filter/", "");
                value = attributeName + ":" + value;
                values.add(value);
              } else {
                attributes.put(attributeName, Arrays.asList(parameters.get(param)));
              }
            }
          }
        }

        if (exceptionPathCategory != null) {
          selectedResourcesCategory = exceptionPathCategory;
        }

        stagingService.importResource(selectedResourcesCategory, file, attributes);
      }
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
    for (TargetServer targetServer : synchronizationServers) {
      jsonServers.append("{\"id\":\"").append(targetServer.getId()).append("\",\"name\":\"").append(targetServer.getName()).append("\",\"host\":\"").append(targetServer.getHost())
          .append("\",\"port\":\"").append(targetServer.getPort()).append("\",\"username\":\"").append(targetServer.getUsername()).append("\",\"password\":\"").append(targetServer.getPassword())
          .append("\",\"ssl\":").append(targetServer.isSsl()).append("},");
    }
    if (!synchronizationServers.isEmpty()) {
      jsonServers.deleteCharAt(jsonServers.length() - 1);
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
    for (String selectedResourcesCategory : resourceCategories) {
      ResourceCategory resourceCategory = new ResourceCategory(selectedResourcesCategory);
      selectedResourceCategories.add(resourceCategory);
    }

    // Dispatch selected resources in resources categories
    for (String selectedResource : resources) {
      for (ResourceCategory resourceCategory : selectedResourceCategories) {
        if (selectedResource.startsWith(resourceCategory.getPath())) {
          resourceCategory.getResources().add(new Resource(selectedResource, null, null));
          break;
        }
      }
    }

    // Dispatch selected options in resources categories
    for (String selectedOption : options) {
      int indexColon = selectedOption.indexOf(":");
      if (indexColon > 0) {
        String optionName = selectedOption.substring(0, indexColon);
        String optionValue = selectedOption.substring(indexColon + 1);

        String optionParts[] = optionName.split("_");
        for (ResourceCategory resourceCategory : selectedResourceCategories) {
          if (optionParts[0].equals(resourceCategory.getPath())) {
            if (optionParts[1].equals(OPERATION_EXPORT_PREFIX)) {
              resourceCategory.getExportOptions().put(optionParts[2], optionValue);
            } else if (optionParts[1].equals(OPERATION_IMPORT_PREFIX)) {
              resourceCategory.getImportOptions().put(optionParts[2], optionValue);
            }
            break;
          }
        }
      }
    }

    // Manage paths exceptions (/site/portalsites -> /site)
    List<ResourceCategory> selectedResourceCategoriesWithExceptions = new ArrayList<ResourceCategory>();
    for (ResourceCategory resourceCategory : selectedResourceCategories) {
      if (IMPORT_PATH_EXCEPTIONS.containsKey(resourceCategory.getPath())) {
        resourceCategory.setPath(IMPORT_PATH_EXCEPTIONS.get(resourceCategory.getPath()));
      }
      selectedResourceCategoriesWithExceptions.add(resourceCategory);
    }

    // Sort categories with order of export/import operation dependency
    Collections.sort(selectedResourceCategoriesWithExceptions);

    try {
      synchronizationService.synchronize(selectedResourceCategoriesWithExceptions, targetServer);
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
      StringBuilder builder = new StringBuilder();
      if (sites != null && sites.length > 0) {
        builder.append("<ul>");
        Set<String> resultedNodePaths = stagingService.executeSQL(sql, new HashSet<String>(Arrays.asList(sites)));
        for (String path : resultedNodePaths) {
          builder.append("<li>");
          builder.append(path);
          builder.append("</li>");
        }
        builder.append("</ul>");
      }
      return Response.ok(builder.toString());
    } catch (Exception e) {
      log.error("Error while executing request: " + sql, e);
      return Response.content(500, "Error while executing request: " + e.getMessage());
    }
  }

  private boolean isWikiActivated() {
    try {
      return PortalContainer.getInstance().getComponentInstanceOfType(Class.forName("org.exoplatform.wiki.service.WikiService")) != null;
    } catch (ClassNotFoundException e) {
      return false;
    }
  }

  private boolean isCalendarActivated() {
    try {
      return PortalContainer.getInstance().getComponentInstanceOfType(Class.forName("org.exoplatform.calendar.service.CalendarService")) != null;
    } catch (ClassNotFoundException e) {
      return false;
    }
  }

  private boolean isForumActivated() {
    try {
      return PortalContainer.getInstance().getComponentInstanceOfType(Class.forName("org.exoplatform.forum.service.ForumService")) != null;
    } catch (ClassNotFoundException e) {
      return false;
    }
  }

  private boolean isAnswerActivated() {
    try {
      return PortalContainer.getInstance().getComponentInstanceOfType(Class.forName("org.exoplatform.faq.service.FAQService")) != null;
    } catch (ClassNotFoundException e) {
      return false;
    }
  }

  private boolean isSocialActivated() {
    try {
      return PortalContainer.getInstance().getComponentInstanceOfType(Class.forName("org.exoplatform.social.core.space.spi.SpaceService")) != null;
    } catch (ClassNotFoundException e) {
      return false;
    }
  }

  private boolean parentAlreadyAddedInList(Set<String> foundResources, String address) {
    for (String path : foundResources) {
      if (address.startsWith(path)) {
        return true;
      }
    }
    return false;
  }

  private CharSequence toString(Set<String> foundResources) {
    StringBuilder result = new StringBuilder();
    for (String string : foundResources) {
      result.append(string + ",");
    }
    return result;
  }

  private String transformSpecialPath(String resourcePath) {
    Set<String> keys = IMPORT_ZIP_PATH_EXCEPTIONS.keySet();
    KEYS: for (String key : keys) {
      if (resourcePath.startsWith(key)) {
        resourcePath = IMPORT_ZIP_PATH_EXCEPTIONS.get(key);
        break KEYS;
      }
    }
    return resourcePath;
  }

}
