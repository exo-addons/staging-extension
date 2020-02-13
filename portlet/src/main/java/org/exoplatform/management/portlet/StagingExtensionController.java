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
package org.exoplatform.management.portlet;

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
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.inject.Inject;

/**
 * The Class StagingExtensionController.
 */
@SessionScoped
public class StagingExtensionController {
  
  /** The log. */
  private static Log log = ExoLogger.getLogger(StagingExtensionController.class);

  /** The Constant OPERATION_IMPORT_PREFIX. */
  public static final String OPERATION_IMPORT_PREFIX = "IMPORT";
  
  /** The Constant OPERATION_EXPORT_PREFIX. */
  public static final String OPERATION_EXPORT_PREFIX = "EXPORT";
  
  /** The Constant PARAM_PREFIX_OPTION. */
  public static final String PARAM_PREFIX_OPTION = "staging-option:";

  /** The Constant IMPORT_ZIP_PATH_EXCEPTIONS. */
  public static final Map<String, String> IMPORT_ZIP_PATH_EXCEPTIONS = new HashMap<String, String>();
  
  /** The Constant IMPORT_PATH_EXCEPTIONS. */
  public static final Map<String, String> IMPORT_PATH_EXCEPTIONS = new HashMap<String, String>();

  /** The file to import. */
  private FileItem fileToImport;

  // Don't use inject to not get the merge of all resource bundles
/** The bundle. */
  //  @Inject
  ResourceBundle bundle;

  /** The staging service. */
  @Inject
  StagingService stagingService;

  /** The synchronization service. */
  @Inject
  SynchronizationService synchronizationService;

  /** The management service. */
  @Inject
  ManagementService managementService;

  /** The selected resources tmpl. */
  @Inject
  @Path("selectedResources.gtmpl")
  Template selectedResourcesTmpl;

  /** The index tmpl. */
  @Inject
  @Path("index.gtmpl")
  Template indexTmpl;

  /** The bundle string. */
  private String bundleString;

  /** The Constant resourceCategories. */
  private static final List<ResourceCategory> resourceCategories = new ArrayList<ResourceCategory>();
  
  /** The spaces category index. */
  private static int SPACES_CATEGORY_INDEX = -1;
  
  /** The forum category index. */
  private static int FORUM_CATEGORY_INDEX = -1;
  
  /** The calendar category index. */
  private static int CALENDAR_CATEGORY_INDEX = -1;
  
  /** The wiki category index. */
  private static int WIKI_CATEGORY_INDEX = -1;
  
  /** The count all. */
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

  /**
   * Index.
   *
   * @return the response. content
   */
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

  /**
   * Gets the categories.
   *
   * @return the categories
   */
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
    } catch (Throwable e) {
      log.error("error while getting categories", e);
      return Response.status(500).content(getResourceBundle().getString("staging.error"));
    }
  }

  /**
   * Gets the bundle.
   *
   * @return the bundle
   */
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
          try {
            data.set(key.replace("staging.", ""), getResourceBundle().getObject(key));
          } catch(MissingResourceException e) {
            // Nothing to do, this happens sometimes
          }
        }
      }
      bundleString = data.toString();
      return Response.ok(bundleString);
    } catch (Throwable e) {
      log.error("error while getting categories", e);
      return Response.status(500).content(getResourceBundle().getString("staging.error"));
    }
  }

  /**
   * Gets the resources of category.
   *
   * @param path the path
   * @return the resources of category
   */
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
    } catch (Throwable e) {
      log.error(e);
      return Response.content(500, getResourceBundle().getString("staging.error"));
    }
  }

  /**
   * Prepare import resources.
   *
   * @param file the file
   * @return the response. content
   * @throws IOException Signals that an I/O exception has occurred.
   */
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
    } catch (Throwable e) {
      log.error(e);
      return Response.content(500, getResourceBundle().getString("staging.error"));
    }
  }

  /**
   * Backup.
   *
   * @param backupDirectory the backup directory
   * @param exportJCR the export JCR
   * @param exportIDM the export IDM
   * @param writeStrategy the write strategy
   * @param displayMessageFor the display message for
   * @param message the message
   * @return the response
   * @throws IOException Signals that an I/O exception has occurred.
   */
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
    } catch (Throwable e) {
      log.error("Error occured while backup: ", e);
      return Response.content(500, getResourceBundle().getString("staging.error"));
    }
  }

  /**
   * Restore.
   *
   * @param backupDirectory the backup directory
   * @return the response
   * @throws IOException Signals that an I/O exception has occurred.
   */
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
    } catch (Throwable e) {
      log.error("Error occured while restoring databases", e);
      return Response.content(500, getResourceBundle().getString("staging.error"));
    }
  }

  /**
   * Export.
   *
   * @param resourceCategories the resource categories
   * @param resources the resources
   * @param options the options
   * @return the response. content
   * @throws IOException Signals that an I/O exception has occurred.
   */
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
        return Response.ok(new FileInputStream(file)).withMimeType("application/zip")
                .withHeader("Content-Disposition", "attachment;filename=\"StagingExport.zip\"");
      }
    } catch (Throwable e) {
      log.error("Error while exporting resources, ", e);
      return Response.content(500, getResourceBundle().getString("staging.exportError"));
    }
  }

  /**
   * Import resources.
   *
   * @return the response. content
   * @throws IOException Signals that an I/O exception has occurred.
   */
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
    } catch (Throwable e) {
      log.error("Error occured while importing content", e);
      return Response.content(500, getResourceBundle().getString("staging.importError"));
    }
  }

  /**
   * Gets the synchonization servers.
   *
   * @return the synchonization servers
   */
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
    } catch (Throwable e) {
      log.error("Error while getting target server list", e);
      return Response.content(500, getResourceBundle().getString("staging.error"));
    }
  }

  /**
   * Test server connection.
   *
   * @param name the name
   * @param host the host
   * @param port the port
   * @param username the username
   * @param password the password
   * @param ssl the ssl
   * @return the response
   * @throws Exception the exception
   */
  @Ajax
  @juzu.Resource
  public Response testServerConnection(String name, String host, String port, String username, String password, String ssl) throws Exception {
    try {
      TargetServer targetServer = new TargetServer(name, host, port, username, password, "true".equals(ssl));
      synchronizationService.testServerConnection(targetServer);
      return Response.ok(getResourceBundle().getString("staging.connectionSuccess"));
    } catch (Throwable e) {
      if (log.isTraceEnabled()) {
        log.warn("Test connection error: ", e);
      }
      return Response.content(500, getResourceBundle().getString("staging.connectionError"));
    }
  }

  /**
   * Adds the synchonization server.
   *
   * @param name the name
   * @param host the host
   * @param port the port
   * @param username the username
   * @param password the password
   * @param ssl the ssl
   * @return the response
   */
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
    } catch (Throwable e) {
      log.error("Error while creating target server", e);
      return Response.content(500, getResourceBundle().getString("staging.serverCreationError"));
    }
  }

  /**
   * Removes the synchonization server.
   *
   * @param id the id
   * @return the response
   */
  @Ajax
  @juzu.Resource
  public Response removeSynchonizationServer(String id) {
    try {
      TargetServer targetServer = new TargetServer(id, null, null, null, null, null, false);
      synchronizationService.removeSynchonizationServer(targetServer);
      return Response.ok(getResourceBundle().getString("staging.serverRemoved"));
    } catch (Throwable e) {
      log.error("Error while deleting target server", e);
      return Response.content(500, getResourceBundle().getString("staging.serverRemovalError"));
    }
  }

  /**
   * Synchronize.
   *
   * @param isSSLString the is SSL string
   * @param host the host
   * @param port the port
   * @param username the username
   * @param password the password
   * @param resourceCategories the resource categories
   * @param resources the resources
   * @param options the options
   * @return the response
   * @throws IOException Signals that an I/O exception has occurred.
   */
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
    } catch (Throwable e) {
      log.error("Error while synchronizing data to target server: " + targetServer, e);
      return Response.content(500, getResourceBundle().getString("staging.synchronizationError"));
    }
  }

  /**
   * Execute SQL.
   *
   * @param sql the sql
   * @param sites the sites
   * @return the response
   * @throws IOException Signals that an I/O exception has occurred.
   */
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
    } catch (Throwable e) {
      log.error("Error while executing request: " + sql, e);
      return Response.content(500, getResourceBundle().getString("staging.testSQLError") + ":" + e.getMessage());
    }
  }

  /**
   * Gets the resource bundle.
   *
   * @param locale the locale
   * @return the resource bundle
   */
  private ResourceBundle getResourceBundle(Locale locale) {
    return bundle = ResourceBundle.getBundle("locale.portlet.staging", locale, PortalContainer.getInstance().getPortalClassLoader());
  }

  /**
   * Gets the resource bundle.
   *
   * @return the resource bundle
   */
  private ResourceBundle getResourceBundle() {
    if(bundle == null) {
      bundle = ResourceBundle.getBundle("locale.portlet.staging", PortalRequestContext.getCurrentInstance().getLocale(), PortalContainer.getInstance().getPortalClassLoader());
    }
    return bundle;
  }

  /**
   * Checks if is wiki activated.
   *
   * @return true, if is wiki activated
   */
  private boolean isWikiActivated() {
    try {
      return PortalContainer.getInstance().getComponentInstanceOfType(Class.forName("org.exoplatform.wiki.service.WikiService")) != null;
    } catch (ClassNotFoundException e) {
      return false;
    }
  }

  /**
   * Checks if is calendar activated.
   *
   * @return true, if is calendar activated
   */
  private boolean isCalendarActivated() {
    try {
      return PortalContainer.getInstance().getComponentInstanceOfType(Class.forName("org.exoplatform.calendar.service.CalendarService")) != null;
    } catch (ClassNotFoundException e) {
      return false;
    }
  }

  /**
   * Checks if is forum activated.
   *
   * @return true, if is forum activated
   */
  private boolean isForumActivated() {
    try {
      return PortalContainer.getInstance().getComponentInstanceOfType(Class.forName("org.exoplatform.forum.service.ForumService")) != null;
    } catch (ClassNotFoundException e) {
      return false;
    }
  }

  /**
   * Checks if is social activated.
   *
   * @return true, if is social activated
   */
  private boolean isSocialActivated() {
    try {
      return PortalContainer.getInstance().getComponentInstanceOfType(Class.forName("org.exoplatform.social.core.space.spi.SpaceService")) != null;
    } catch (ClassNotFoundException e) {
      return false;
    }
  }

  /**
   * Parent already added in list.
   *
   * @param foundResources the found resources
   * @param address the address
   * @return true, if successful
   */
  private boolean parentAlreadyAddedInList(Set<String> foundResources, String address) {
    for (String path : foundResources) {
      if (address.startsWith(path)) {
        return true;
      }
    }
    return false;
  }

  /**
   * To string.
   *
   * @param foundResources the found resources
   * @return the char sequence
   */
  private CharSequence toString(Set<String> foundResources) {
    StringBuilder result = new StringBuilder();
    for (String string : foundResources) {
      result.append(string + ",");
    }
    return result;
  }

  /**
   * Transform special path.
   *
   * @param resourcePath the resource path
   * @return the string
   */
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

  /**
   * Checks if is user admin.
   *
   * @return true, if is user admin
   */
  private boolean isUserAdmin() {
    try {
      return ConversationState.getCurrent().getIdentity().getRoles().contains("administrators");
    } catch (Throwable e) {
      log.error(e);
      return false;
    }
  }

  /**
   * Gets the current user.
   *
   * @return the current user
   */
  private String getCurrentUser() {
    try {
      return ConversationState.getCurrent().getIdentity().getUserId();
    } catch (Throwable e) {
      log.error(e);
      return null;
    }
  }

}
