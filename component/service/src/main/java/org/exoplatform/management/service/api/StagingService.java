package org.exoplatform.management.service.api;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.fileupload.FileItem;

public interface StagingService {

  public static final String WIKIS_PARENT_PATH = "/wiki";
  public static final String USER_WIKIS_PATH = "/wiki/user";
  public static final String GROUP_WIKIS_PATH = "/wiki/group";
  public static final String PORTAL_WIKIS_PATH = "/wiki/portal";
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
   * Returns the list of sub resources of portal wikis computed from
   * GateIN Management SPI
   * 
   * @return list of portal sites managed paths.
   */
  Set<Resource> getWikiPortalResources();
  Set<Resource> getWikiGroupResources();
  Set<Resource> getWikiUserResources();
  
  /**
   * Returns the list of sub resources of MOP of type portalsites computed from
   * GateIN Management SPI
   * 
   * @return list of portal sites managed paths.
   */
  Set<Resource> getPortalSiteResources();

  /**
   * Returns the list of sub resources of MOP of type groupsites computed from
   * GateIN Management SPI
   * 
   * @return list of portal sites managed paths.
   */
  Set<Resource> getGroupSiteResources();

  /**
   * Returns the list of sub resources of MOP of type usersites computed from
   * GateIN Management SPI
   * 
   * @return list of portal sites managed paths.
   */
  Set<Resource> getUserSiteResources();

  /**
   * Returns the list of sub resources of /content/sites managed resources
   * computed from GateIN Management SPI
   * 
   * @return list of site contents managed paths.
   */
  Set<Resource> getSiteContentResources();

  /**
   * Returns the list of sub resources of
   * /ecmadmin/templates/applications/content-list-viewer managed resources
   * computed from GateIN Management SPI
   * 
   * @return list of CLV templates managed paths.
   */
  Set<Resource> getApplicationCLVTemplatesResources();

  /**
   * Returns the list of sub resources of
   * /ecmadmin/templates/applications/search managed resources computed from
   * GateIN Management SPI
   * 
   * @return list of Search Portlet Templates managed paths.
   */
  Set<Resource> getApplicationSearchTemplatesResources();

  /**
   * Returns the list of sub resources of
   * /ecmadmin/templates/applications/nodetype managed resources computed from
   * GateIN Management SPI
   * 
   * @return list of DocumentType templates managed paths.
   */
  Set<Resource> getDocumentTypeTemplatesResources();

  /**
   * Returns the list of sub resources of
   * /ecmadmin/templates/applications/metadata managed resources computed from
   * GateIN Management SPI
   * 
   * @return list of metadata templates managed paths.
   */
  Set<Resource> getMetadataTemplatesResources();

  /**
   * Returns the list of sub resources of /ecmadmin/taxonomy managed resources
   * computed from GateIN Management SPI
   * 
   * @return list of taxonomy managed paths.
   */
  Set<Resource> getTaxonomyResources();

  /**
   * Returns the list of sub resources of /ecmadmin/queries managed resources
   * computed from GateIN Management SPI
   * 
   * @return list of JCR Query managed paths.
   */
  Set<Resource> getQueryResources();

  /**
   * Returns the list of sub resources of /ecmadmin/drive managed resources
   * computed from GateIN Management SPI
   * 
   * @return list of ECMS Drives managed paths.
   */
  Set<Resource> getDriveResources();

  /**
   * Returns the list of sub resources of /ecmadmin/script managed resources
   * computed from GateIN Management SPI
   * 
   * @return list of ECMS Script managed paths.
   */
  Set<Resource> getScriptResources();

  /**
   * Returns the list of sub resources of /ecmadmin/action managed resources
   * computed from GateIN Management SPI
   * 
   * @return list of action nodetype managed paths.
   */
  Set<Resource> getActionNodeTypeResources();

  /**
   * Returns the list of sub resources of /ecmadmin/nodetype managed resources
   * computed from GateIN Management SPI
   * 
   * @return list of JCR Nodetype managed paths.
   */
  Set<Resource> getNodeTypeResources();

  /**
   * Returns the list of sub resources of /ecmadmin/taxonomy managed resources
   * computed from GateIN Management SPI
   * 
   * @return list of application registry categories managed paths.
   */
  Set<Resource> getRegistryResources();

  /**
   * Returns the list of sub resources of /ecmadmin/view/templates managed
   * resources computed from GateIN Management SPI
   * 
   * @return list of ECMS View Templates managed paths.
   */
  Set<Resource> getViewTemplatesResources();

  /**
   * Returns the list of sub resources of /ecmadmin/view managed resources
   * computed from GateIN Management SPI
   * 
   * @return list of ECMS View Configuration managed paths.
   */
  Set<Resource> getViewConfigurationResources();

  /**
   * Returns the list of sub resources of /gadget managed resources computed
   * from GateIN Management SPI
   * 
   * @return list of Gadgets managed paths.
   */
  Set<Resource> getGadgetResources();

  /**
   * Returns the list of sub resources of /organization/user managed resources
   * computed from GateIN Management SPI
   * 
   * @return list of Users managed paths.
   */
  Set<Resource> getUserResources();

  /**
   * Returns the list of sub resources of /organization/group managed resources
   * computed from GateIN Management SPI
   * 
   * @return list of Groups managed paths.
   */
  Set<Resource> getGroupResources();

  /**
   * Returns the list of sub resources of /organization/role managed resources
   * computed from GateIN Management SPI
   * 
   * @return list of MembershipTypes/Roles managed paths.
   */
  Set<Resource> getRoleResources();

  /**
   * Execute an SQL JCR Query
   * 
   * @param sql
   * @param selectedResources
   * @throws Exception
   */
  Set<String> executeSQL(String sql, Set<String> sites) throws Exception;

}
