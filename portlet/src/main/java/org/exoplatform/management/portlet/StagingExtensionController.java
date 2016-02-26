package org.exoplatform.management.portlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.inject.Inject;

import juzu.Path;
import juzu.Response;
import juzu.Route;
import juzu.SessionScoped;
import juzu.View;
import juzu.impl.common.JSON;
import juzu.impl.request.Request;
import juzu.request.RequestParameter;
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
import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.ConversationState;
import org.gatein.management.api.ManagementService;

@SessionScoped
public class StagingExtensionController {
  private static Log log = ExoLogger.getLogger(StagingExtensionController.class);

  public static final String OPERATION_IMPORT_PREFIX = "IMPORT";
  public static final String OPERATION_EXPORT_PREFIX = "EXPORT";
  public static final String PARAM_PREFIX_OPTION = "staging-option:";

  public static final Map<String, String> IMPORT_ZIP_PATH_EXCEPTIONS = new HashMap<String, String>();
  public static final Map<String, String> IMPORT_PATH_EXCEPTIONS = new HashMap<String, String>();

  private FileItem fileToImport;

  // Don't use inject to not get the merge of all resource bundles
//  @Inject
  ResourceBundle bundle;

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

  private String bundleString;

  private static final List<ResourceCategory> resourceCategories = new ArrayList<ResourceCategory>();
  private static int SPACES_CATEGORY_INDEX = -1;
  private static int FORUM_CATEGORY_INDEX = -1;
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
      ResourceCategory sites = new ResourceCategory("staging.sites", "/site");
      if (activatedModules.isEmpty() || activatedModules.contains(StagingService.SITES_PORTAL_PATH + ":activated")) {
        sites.getSubResourceCategories().add(new ResourceCategory("staging.portalSites", StagingService.SITES_PORTAL_PATH));
      }
      if (activatedModules.isEmpty() || activatedModules.contains(StagingService.SITES_GROUP_PATH + ":activated")) {
        sites.getSubResourceCategories().add(new ResourceCategory("staging.groupSites", StagingService.SITES_GROUP_PATH));
      }
      if (activatedModules.isEmpty() || activatedModules.contains(StagingService.SITES_USER_PATH + ":activated")) {
        sites.getSubResourceCategories().add(new ResourceCategory("staging.userSites", StagingService.SITES_USER_PATH));
      }
      resourceCategories.add(sites);
    }

    if (activatedModules.isEmpty() || activatedModules.contains(StagingService.SOCIAL_PARENT_PATH + ":activated")) {
      ResourceCategory social = new ResourceCategory("staging.social", StagingService.SOCIAL_PARENT_PATH);
      if (activatedModules.isEmpty() || activatedModules.contains(StagingService.SOCIAL_SPACE_PATH + ":activated")) {
        social.getSubResourceCategories().add(new ResourceCategory("staging.spaces", StagingService.SOCIAL_SPACE_PATH));
      }
      resourceCategories.add(social);
      SPACES_CATEGORY_INDEX = resourceCategories.size() - 1;
    }

    if (activatedModules.isEmpty() || activatedModules.contains("/content:activated")) {
      ResourceCategory contents = new ResourceCategory("staging.contents", "/content");
      if (activatedModules.isEmpty() || activatedModules.contains(StagingService.CONTENT_SITES_PATH + ":activated")) {
        contents.getSubResourceCategories().add(new ResourceCategory("staging.sitesContents", StagingService.CONTENT_SITES_PATH));
      }
      resourceCategories.add(contents);
    }

    if (activatedModules.isEmpty() || activatedModules.contains(StagingService.FORUMS_PARENT_PATH + ":activated")) {
      ResourceCategory forums = new ResourceCategory("staging.forums", StagingService.FORUMS_PARENT_PATH);
      if (activatedModules.isEmpty() || activatedModules.contains(StagingService.PUBLIC_FORUM_PATH + ":activated")) {
        forums.getSubResourceCategories().add(new ResourceCategory("staging.publicForums", StagingService.PUBLIC_FORUM_PATH));
      }
      if (activatedModules.isEmpty() || activatedModules.contains(StagingService.SPACE_FORUM_PATH + ":activated")) {
        forums.getSubResourceCategories().add(new ResourceCategory("staging.spaceForums", StagingService.SPACE_FORUM_PATH));
      }
      if (activatedModules.isEmpty() || activatedModules.contains(StagingService.FORUM_SETTINGS + ":activated")) {
        forums.getSubResourceCategories().add(new ResourceCategory("staging.forumSettings", StagingService.FORUM_SETTINGS));
      }
      resourceCategories.add(forums);
      FORUM_CATEGORY_INDEX = resourceCategories.size() - 1;
    }

    if (activatedModules.isEmpty() || activatedModules.contains(StagingService.CALENDARS_PARENT_PATH + ":activated")) {
      ResourceCategory calendars = new ResourceCategory("staging.calendar", StagingService.CALENDARS_PARENT_PATH);
      if (activatedModules.isEmpty() || activatedModules.contains(StagingService.GROUP_CALENDAR_PATH + ":activated")) {
        calendars.getSubResourceCategories().add(new ResourceCategory("staging.groupCalendar", StagingService.GROUP_CALENDAR_PATH));
      }
      if (activatedModules.isEmpty() || activatedModules.contains(StagingService.SPACE_CALENDAR_PATH + ":activated")) {
        calendars.getSubResourceCategories().add(new ResourceCategory("staging.spaceCalendar", StagingService.SPACE_CALENDAR_PATH));
      }
      if (activatedModules.isEmpty() || activatedModules.contains(StagingService.PERSONAL_FORUM_PATH + ":activated")) {
        calendars.getSubResourceCategories().add(new ResourceCategory("staging.personalCalendar", StagingService.PERSONAL_FORUM_PATH));
      }
      resourceCategories.add(calendars);
      CALENDAR_CATEGORY_INDEX = resourceCategories.size() - 1;
    }

    if (activatedModules.isEmpty() || activatedModules.contains(StagingService.WIKIS_PARENT_PATH + ":activated")) {
      ResourceCategory wikis = new ResourceCategory("staging.wikis", StagingService.WIKIS_PARENT_PATH);
      if (activatedModules.isEmpty() || activatedModules.contains(StagingService.PORTAL_WIKIS_PATH + ":activated")) {
        wikis.getSubResourceCategories().add(new ResourceCategory("staging.portalWikis", StagingService.PORTAL_WIKIS_PATH));
      }
      if (activatedModules.isEmpty() || activatedModules.contains(StagingService.GROUP_WIKIS_PATH + ":activated")) {
        wikis.getSubResourceCategories().add(new ResourceCategory("staging.spaceWikis", StagingService.GROUP_WIKIS_PATH));
      }
      if (activatedModules.isEmpty() || activatedModules.contains(StagingService.USER_WIKIS_PATH + ":activated")) {
        wikis.getSubResourceCategories().add(new ResourceCategory("staging.userWikis", StagingService.USER_WIKIS_PATH));
      }
      resourceCategories.add(wikis);
      WIKI_CATEGORY_INDEX = resourceCategories.size() - 1;
    }

    if (activatedModules.isEmpty() || activatedModules.contains("/organization:activated")) {
      ResourceCategory organization = new ResourceCategory("staging.organization", "/organization");
      if (activatedModules.isEmpty() || activatedModules.contains(StagingService.USERS_PATH + ":activated")) {
        organization.getSubResourceCategories().add(new ResourceCategory("staging.users", StagingService.USERS_PATH));
      }
      if (activatedModules.isEmpty() || activatedModules.contains(StagingService.GROUPS_PATH + ":activated")) {
        organization.getSubResourceCategories().add(new ResourceCategory("staging.groups", StagingService.GROUPS_PATH));
      }
      if (activatedModules.isEmpty() || activatedModules.contains(StagingService.ROLE_PATH + ":activated")) {
        organization.getSubResourceCategories().add(new ResourceCategory("staging.roles", StagingService.ROLE_PATH));
      }
      resourceCategories.add(organization);
    }

    if (activatedModules.isEmpty() || activatedModules.contains("/application:activated")) {
      ResourceCategory applications = new ResourceCategory("staging.applications", "/application");
      if (activatedModules.isEmpty() || activatedModules.contains(StagingService.REGISTRY_PATH + ":activated")) {
        applications.getSubResourceCategories().add(new ResourceCategory("staging.applicationRegistry", StagingService.REGISTRY_PATH));
      }
      if (activatedModules.isEmpty() || activatedModules.contains(StagingService.GADGET_PATH + ":activated")) {
        applications.getSubResourceCategories().add(new ResourceCategory("staging.gadgets", StagingService.GADGET_PATH));
      }
      resourceCategories.add(applications);
    }

    if (activatedModules.isEmpty() || activatedModules.contains("/ecmadmin:activated")) {
      ResourceCategory ecmAdmin = new ResourceCategory("staging.cmsAdmin", "/ecmadmin");
      if (activatedModules.isEmpty() || activatedModules.contains(StagingService.ECM_TEMPLATES_APPLICATION_CLV_PATH + ":activated")) {
        ecmAdmin.getSubResourceCategories().add(new ResourceCategory("staging.clv", StagingService.ECM_TEMPLATES_APPLICATION_CLV_PATH));
      }
      if (activatedModules.isEmpty() || activatedModules.contains(StagingService.ECM_TEMPLATES_DOCUMENT_TYPE_PATH + ":activated")) {
        ecmAdmin.getSubResourceCategories().add(new ResourceCategory("staging.docTemplates", StagingService.ECM_TEMPLATES_DOCUMENT_TYPE_PATH));
      }
      if (activatedModules.isEmpty() || activatedModules.contains(StagingService.ECM_TEMPLATES_METADATA_PATH + ":activated")) {
        ecmAdmin.getSubResourceCategories().add(new ResourceCategory("staging.metadataTemplates", StagingService.ECM_TEMPLATES_METADATA_PATH));
      }
      if (activatedModules.isEmpty() || activatedModules.contains(StagingService.ECM_TAXONOMY_PATH + ":activated")) {
        ecmAdmin.getSubResourceCategories().add(new ResourceCategory("staging.taxonomies", StagingService.ECM_TAXONOMY_PATH));
      }
      if (activatedModules.isEmpty() || activatedModules.contains(StagingService.ECM_QUERY_PATH + ":activated")) {
        ecmAdmin.getSubResourceCategories().add(new ResourceCategory("staging.queries", StagingService.ECM_QUERY_PATH));
      }
      if (activatedModules.isEmpty() || activatedModules.contains(StagingService.ECM_DRIVE_PATH + ":activated")) {
        ecmAdmin.getSubResourceCategories().add(new ResourceCategory("staging.drives", StagingService.ECM_DRIVE_PATH));
      }
      if (activatedModules.isEmpty() || activatedModules.contains(StagingService.ECM_SCRIPT_PATH + ":activated")) {
        ecmAdmin.getSubResourceCategories().add(new ResourceCategory("staging.scripts", StagingService.ECM_SCRIPT_PATH));
      }
      if (activatedModules.isEmpty() || activatedModules.contains(StagingService.ECM_VIEW_TEMPLATES_PATH + ":activated")) {
        ecmAdmin.getSubResourceCategories().add(new ResourceCategory("staging.viewTemplate", StagingService.ECM_VIEW_TEMPLATES_PATH));
      }
      if (activatedModules.isEmpty() || activatedModules.contains(StagingService.ECM_VIEW_CONFIGURATION_PATH + ":activated")) {
        ecmAdmin.getSubResourceCategories().add(new ResourceCategory("staging.viewConfiguration", StagingService.ECM_VIEW_CONFIGURATION_PATH));
      }
      if (activatedModules.isEmpty() || activatedModules.contains(StagingService.ECM_NODETYPE_PATH + ":activated")) {
        ecmAdmin.getSubResourceCategories().add(new ResourceCategory("staging.nodetypes", StagingService.ECM_NODETYPE_PATH));
      }
      resourceCategories.add(ecmAdmin);
    }

    COUNT_ALL = resourceCategories.size();
  }

  @View
  public Response.Content index() {
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
      if (!isSocialActivated() && SPACES_CATEGORY_INDEX > 0) {
        resourceCategories.remove(SPACES_CATEGORY_INDEX);
      }
    }

    return indexTmpl.ok(Collections.singletonMap("isAdmin", isUserAdmin()));
  }

  @Ajax
  @juzu.Resource
  public Response getCategories() {
    try {
      StringBuilder jsonCategories = new StringBuilder(50);
      jsonCategories.append("{\"categories\":[");

      for (ResourceCategory category : resourceCategories) {
        jsonCategories.append("{\"path\":\"").append(category.getPath()).append("\",\"label\":\"").append(getResourceBundle().getString(category.getLabel())).append("\",\"subcategories\":[");
        for (ResourceCategory subcategory : category.getSubResourceCategories()) {
          jsonCategories.append("{\"path\":\"").append(subcategory.getPath()).append("\",\"label\":\"").append(getResourceBundle().getString(subcategory.getLabel())).append("\"},");
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
    } catch (Exception e) {
      log.error("error while getting categories", e);
      return Response.status(500).content(getResourceBundle().getString("staging.error"));
    }
  }

  @Ajax
  @juzu.Resource
  @juzu.MimeType.JSON
  public Response getBundle() {
    try {
      if (bundleString != null && getResourceBundle().getLocale().equals(PortalRequestContext.getCurrentInstance().getLocale())) {
        return Response.ok(bundleString);
      }
      bundle = getResourceBundle(PortalRequestContext.getCurrentInstance().getLocale());
      JSON data = new JSON();
      Enumeration<String> enumeration = getResourceBundle().getKeys();
      while (enumeration.hasMoreElements()) {
        String key = (String) enumeration.nextElement();
        if (key.startsWith("staging.")) {
          data.set(key.replace("staging.", ""), getResourceBundle().getObject(key));
        }
      }
      bundleString = data.toString();
      return Response.ok(bundleString);
    } catch (Exception e) {
      log.error("error while getting categories", e);
      return Response.status(500).content(getResourceBundle().getString("staging.error"));
    }
  }

  @Ajax
  @juzu.Resource
  public Response getResourcesOfCategory(String path) {
    try {
      Set<Resource> resources = stagingService.getResources(path);

      StringBuilder jsonResources = new StringBuilder(50);
      jsonResources.append("{\"resources\":[");

      for (Resource resource : resources) {
        jsonResources.append("{\"path\":\"").append(resource.getPath()).append("\",\"description\":\"").append(resource.getDescription()).append("\",\"text\":\"").append(resource.getText()).append("\"},");
      }
      if (!resources.isEmpty()) {
        jsonResources.deleteCharAt(jsonResources.length() - 1);
      }
      jsonResources.append("]}");

      return Response.ok(jsonResources.toString());
    } catch (Exception e) {
      log.error(e);
      return Response.content(500, getResourceBundle().getString("staging.error"));
    }
  }

  @Ajax
  @juzu.Resource
  public Response.Content prepareImportResources(FileItem file) throws IOException {
    try {
      if (file == null || file.getSize() == 0) {
        return Response.content(500, getResourceBundle().getString("staging.emptyFile"));
      }

      ZipInputStream zipInputStream = new ZipInputStream(file.getInputStream());
      Set<String> foundResources = new HashSet<String>();
      try {
        ZipEntry entry = zipInputStream.getNextEntry();
        while (entry != null) {
          String fileName = entry.getName();
          if (entry.isDirectory()) {
            entry = zipInputStream.getNextEntry();
            continue;
          }
          fileName = fileName.startsWith("/") ? "" : "/" + fileName;
          String resourcePath = transformSpecialPath(fileName);
          // If resource path transformed and treated with Exceptions Resource
          // Paths
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

        fileToImport = file;

        log.info("Found resources zip file : {}", foundResources);
      } finally {
        zipInputStream.close();
      }

      if (!foundResources.isEmpty()) {
        return Response.ok(toString(foundResources));
      } else {
        return Response.content(500, getResourceBundle().getString("staging.invalidArchive"));
      }
    } catch (Exception e) {
      log.error(e);
      return Response.content(500, getResourceBundle().getString("staging.error"));
    }
  }

  @Ajax
  @juzu.Resource
  public Response backup(String backupDirectory, String exportJCR, String exportIDM, String writeStrategy, String displayMessageFor, String message) throws IOException {
    try {
      if (!isUserAdmin()) {
        log.warn("User '" + getCurrentUser() + "' is not authorized to backup the system.");
        return Response.content(403, getResourceBundle().getString("staging.unauthorizedUser"));
      }
      ResourceCategory category = new ResourceCategory("/backup");
      String resourcePath = "/backup/" + PortalContainer.getCurrentPortalContainerName();
      category.getResources().add(new Resource(resourcePath, resourcePath, ""));
      category.getExportOptions().put("directory", backupDirectory);
      category.getExportOptions().put("export-jcr", exportJCR);
      category.getExportOptions().put("export-idm", exportIDM);
      category.getExportOptions().put("writeStrategy", writeStrategy);
      category.getExportOptions().put("displayMessageFor", displayMessageFor);
      category.getExportOptions().put("message", message);
      List<ResourceCategory> resourceCategories = Collections.singletonList(category);
      stagingService.export(resourceCategories);
      return Response.ok(getResourceBundle().getString("staging.backupSuccess"));
    } catch (Exception e) {
      log.error("Error occured while backup: ", e);
      return Response.content(500, getResourceBundle().getString("staging.error"));
    }
  }

  @Ajax
  @juzu.Resource
  public Response restore(String backupDirectory) throws IOException {
    try {
      if (!isUserAdmin()) {
        log.warn("User '" + getCurrentUser() + "' is not authorized to restore the system.");
        return Response.content(403, "User is not authorized to restore the system.");
      }
      Map<String, List<String>> attributes = new HashMap<String, List<String>>();
      attributes.put("directory", Collections.singletonList(backupDirectory));

      stagingService.importResource("/backup/portal", null, attributes);

      return Response.ok(getResourceBundle().getString("staging.restoreSuccess"));
    } catch (Exception e) {
      log.error("Error occured while restoring databases", e);
      return Response.content(500, getResourceBundle().getString("staging.error"));
    }
  }

  @Ajax
  @juzu.Resource
  public Response.Content export(String[] resourceCategories, String[] resources, String[] options) throws IOException {
    try {
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

      File file = stagingService.export(selectedResourceCategoriesWithExceptions);
      if (file == null) {
        return Response.content(500, getResourceBundle().getString("staging.emptyResourceList"));
      } else {
        return Response.ok(new FileInputStream(file)).withMimeType("application/zip").withHeader("Set-Cookie", "fileDownload=true; path=/").withHeader("Content-Disposition", "filename=\"StagingExport.zip\"");
      }
    } catch (Exception e) {
      log.error("Error while exporting resources, ", e);
      return Response.content(500, getResourceBundle().getString("staging.exportError"));
    }
  }

  @Ajax
  @juzu.Resource
  public Response.Content importResources() throws IOException {
    if (fileToImport == null || fileToImport.getSize() == 0) {
      return Response.content(500, getResourceBundle().getString("staging.emptyFile"));
    }

    Map<String, RequestParameter> parameters = Request.getCurrent().getParameterArguments();
    RequestParameter selectedResourcesCategories = parameters.get("staging:resourceCategory");

    if (selectedResourcesCategories == null || selectedResourcesCategories.size() == 0) {
      return Response.content(500, getResourceBundle().getString("staging.emptyResourceList"));
    }

    try {
      List<String> selectedResourcesCategoriesList = new ArrayList<String>(selectedResourcesCategories);
      Collections.sort(selectedResourcesCategoriesList, new Comparator<String>() {
        @Override
        public int compare(String o1, String o2) {
          return ResourceCategory.getOrder(o1) - ResourceCategory.getOrder(o1);
        }
      });
      for (String selectedResourcesCategory : selectedResourcesCategoriesList) {
        log.info("inporting resources for : " + selectedResourcesCategory);
        String exceptionPathCategory = IMPORT_PATH_EXCEPTIONS.get(selectedResourcesCategory);

        // filter ResourceCategory options coming from request
        Map<String, List<String>> attributes = new HashMap<String, List<String>>();
        for (String param : parameters.keySet()) {
          if (param.startsWith(PARAM_PREFIX_OPTION)) {
            String paramPath = param.replace(PARAM_PREFIX_OPTION, "");
            if (parameters.get(param) == null || parameters.get(param).size() == 0) {
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
                if (parameters.get(param).size() > 1) {
                  log.error("Can't parse all parameters for filter: '" + paramPath + "' with values = " + parameters.get(param) + ". Only first parameter will be used: "
                      + parameters.get(param).get(0));
                }
                String value = parameters.get(param).get(0);
                attributeName = attributeName.replace("filter/", "");
                value = attributeName + ":" + value;
                values.add(value);
              } else {
                attributes.put(attributeName, parameters.get(param));
              }
            }
          }
        }

        if (exceptionPathCategory != null) {
          selectedResourcesCategory = exceptionPathCategory;
        }

        stagingService.importResource(selectedResourcesCategory, fileToImport.getInputStream(), attributes);
      }
      return Response.ok(getResourceBundle().getString("staging.importSuccess"));
    } catch (Exception e) {
      log.error("Error occured while importing content", e);
      return Response.content(500, getResourceBundle().getString("staging.importError"));
    }
  }

  @Ajax
  @juzu.Resource
  @Route("/servers")
  public Response getSynchonizationServers() {
    try {
      List<TargetServer> synchronizationServers = synchronizationService.getSynchonizationServers();

      StringBuilder jsonServers = new StringBuilder(50);
      jsonServers.append("{\"synchronizationServers\":[");
      for (TargetServer targetServer : synchronizationServers) {
        jsonServers.append("{\"id\":\"").append(targetServer.getId()).append("\",\"name\":\"").append(targetServer.getName()).append("\",\"host\":\"").append(targetServer.getHost()).append("\",\"port\":\"").append(targetServer.getPort()).append("\",\"username\":\"").append(targetServer.getUsername()).append("\",\"password\":\"").append(targetServer.getPassword()).append("\",\"ssl\":").append(targetServer.isSsl()).append("},");
      }
      if (!synchronizationServers.isEmpty()) {
        jsonServers.deleteCharAt(jsonServers.length() - 1);
      }
      jsonServers.append("]}");

      return Response.ok(jsonServers.toString());
    } catch (Exception e) {
      log.error("Error while getting target server list", e);
      return Response.content(500, getResourceBundle().getString("staging.error"));
    }
  }

  @Ajax
  @juzu.Resource
  public Response testServerConnection(String name, String host, String port, String username, String password, String ssl) throws Exception {
    try {
      TargetServer targetServer = new TargetServer(name, host, port, username, password, "true".equals(ssl));
      synchronizationService.testServerConnection(targetServer);
      return Response.ok(getResourceBundle().getString("staging.connectionSuccess"));
    } catch (Exception e) {
      if (log.isTraceEnabled()) {
        log.warn("Test connection error: ", e);
      }
      return Response.content(500, getResourceBundle().getString("staging.connectionError"));
    }
  }

  @Ajax
  @juzu.Resource
  public Response addSynchonizationServer(String name, String host, String port, String username, String password, String ssl) {
    try {
      TargetServer targetServer = new TargetServer(name, host, port, username, password, "true".equals(ssl));
      List<TargetServer> targetServers = synchronizationService.getSynchonizationServers();
      if(targetServers.contains(targetServer)) {
        return Response.content(500, getResourceBundle().getString("staging.serverAlreadyExists"));
      }
      
      synchronizationService.addSynchonizationServer(targetServer);
      return Response.ok(getResourceBundle().getString("staging.serverCreated"));
    } catch (Exception e) {
      log.error("Error while creating target server", e);
      return Response.content(500, getResourceBundle().getString("staging.serverCreationError"));
    }
  }

  @Ajax
  @juzu.Resource
  public Response removeSynchonizationServer(String id) {
    try {
      TargetServer targetServer = new TargetServer(id, null, null, null, null, null, false);
      synchronizationService.removeSynchonizationServer(targetServer);
      return Response.ok(getResourceBundle().getString("staging.serverRemoved"));
    } catch (Exception e) {
      log.error("Error while deleting target server", e);
      return Response.content(500, getResourceBundle().getString("staging.serverRemovalError"));
    }
  }

  @Ajax
  @juzu.Resource
  public Response synchronize(String isSSLString, String host, String port, String username, String password, String[] resourceCategories, String[] resources, String[] options) throws IOException {
    TargetServer targetServer = new TargetServer(host, port, username, password, "true".equals(isSSLString));
    try {
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

      synchronizationService.synchronize(selectedResourceCategoriesWithExceptions, targetServer);
      return Response.ok(getResourceBundle().getString("staging.synchronizationSuccess"));
    } catch (Exception e) {
      log.error("Error while synchronizing data to target server: " + targetServer, e);
      return Response.content(500, getResourceBundle().getString("staging.synchronizationError"));
    }
  }

  @Ajax
  @juzu.Resource
  public Response executeSQL(String sql, String[] sites) throws IOException {
    try {
      StringBuilder builder = new StringBuilder();
      if (sites != null && sites.length > 0) {
        builder.append("Results").append("\r\n");
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
      return Response.content(500, getResourceBundle().getString("staging.testSQLError") + ":" + e.getMessage());
    }
  }

  private ResourceBundle getResourceBundle(Locale locale) {
    return bundle = ResourceBundle.getBundle("locale.portlet.staging", locale, PortalContainer.getInstance().getPortalClassLoader());
  }

  private ResourceBundle getResourceBundle() {
    if(bundle == null) {
      bundle = ResourceBundle.getBundle("locale.portlet.staging", PortalRequestContext.getCurrentInstance().getLocale(), PortalContainer.getInstance().getPortalClassLoader());
    }
    return bundle;
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

  private boolean isUserAdmin() {
    try {
      return ConversationState.getCurrent().getIdentity().getRoles().contains("administrators");
    } catch (Exception e) {
      log.error(e);
      return false;
    }
  }

  private String getCurrentUser() {
    try {
      return ConversationState.getCurrent().getIdentity().getUserId();
    } catch (Exception e) {
      log.error(e);
      return null;
    }
  }

}
