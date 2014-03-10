package org.exoplatform.management.service.api;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.fileupload.FileItem;

public interface StagingService {

  public static final String SITES_PARENT_PATH = "/site";
  public static final String SITES_PORTAL_PATH = "/site/portalsites";
  public static final String SITES_GROUP_PATH = "/site/groupsites";
  public static final String SITES_USER_PATH = "/site/usersites";
  public static final String CONTENT_SITES_PATH = "/content/sites";
  public static final String ECM_TEMPLATES_APPLICATION_CLV_PATH = "/ecmadmin/templates/applications/content-list-viewer";
  public static final String ECM_TEMPLATES_APPLICATION_SEARCH_PATH = "/ecmadmin/templates/applications/search";
  public static final String ECM_TEMPLATES_DOCUMENT_TYPE_PATH = "/ecmadmin/templates/nodetypes";
  public static final String ECM_TEMPLATES_METADATA_PATH = "/ecmadmin/templates/metadata";
  public static final String ECM_TAXONOMY_PATH = "/ecmadmin/taxonomy";
  public static final String ECM_QUERY_PATH = "/ecmadmin/queries";
  public static final String ECM_DRIVE_PATH = "/ecmadmin/drive";
  public static final String ECM_SCRIPT_PATH = "/ecmadmin/script";
  public static final String ECM_ACTION_PATH = "/ecmadmin/action";
  public static final String ECM_NODETYPE_PATH = "/ecmadmin/nodetype";
  public static final String ECM_VIEW_CONFIGURATION_PATH = "/ecmadmin/view/configuration";
  public static final String ECM_VIEW_TEMPLATES_PATH = "/ecmadmin/view/templates";
  public static final String REGISTRY_PATH = "/registry";
  public static final String GADGET_PATH = "/gadget";
  public static final String USERS_PATH = "/organization/user";
  public static final String GROUPS_PATH = "/organization/group";
  public static final String ROLE_PATH = "/organization/role";

  /**
   * Export selected resources with selected options.
   * 
   * @param resourcesPaths
   * @param exportOptions
   * @return
   * @throws Exception
   */
  public File export(List<ResourceCategory> selectedResourceCategoriesWithExceptions) throws Exception;

  /**
   * Import resources
   * 
   * @param selectedResourcePath
   * @param file
   * @throws IOException
   */
  void importResource(String selectedResourcePath, FileItem file, Map<String, List<String>> attributes) throws IOException;

  /**
   * Returns the list of sub resources of the given path
   * 
   * @return list of resources
   */
  public Set<Resource> getResources(String path);

  /**
   * Execute an SQL JCR Query
   * 
   * @param sql
   * @param selectedResources
   * @throws Exception
   */
  Set<String> executeSQL(String sql, Set<String> sites) throws Exception;

}
