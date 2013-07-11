package org.exoplatform.extension.synchronization.service.api;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

public interface SynchronizationService {

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

  /**
   * Returns the list of sub resources of MOP of type portalsites computed from
   * GateIN Management SPI
   * 
   * @return list of portal sites managed paths.
   */
  Set<Node> getPortalSiteNodes();

  /**
   * Returns the list of sub resources of MOP of type groupsites computed from
   * GateIN Management SPI
   * 
   * @return list of portal sites managed paths.
   */
  Set<Node> getGroupSiteNodes();

  /**
   * Returns the list of sub resources of MOP of type usersites computed from
   * GateIN Management SPI
   * 
   * @return list of portal sites managed paths.
   */
  Set<Node> getUserSiteNodes();

  /**
   * Returns the list of sub resources of /content/sites managed resources
   * computed from GateIN Management SPI
   * 
   * @return list of site contents managed paths.
   */
  Set<Node> getSiteContentNodes();

  /**
   * Returns the list of sub resources of
   * /ecmadmin/templates/applications/content-list-viewer managed resources
   * computed from GateIN Management SPI
   * 
   * @return list of CLV templates managed paths.
   */
  Set<Node> getApplicationCLVTemplatesNodes();

  /**
   * Returns the list of sub resources of
   * /ecmadmin/templates/applications/search managed resources computed from
   * GateIN Management SPI
   * 
   * @return list of Search Portlet Templates managed paths.
   */
  Set<Node> getApplicationSearchTemplatesNodes();

  /**
   * Returns the list of sub resources of
   * /ecmadmin/templates/applications/nodetype managed resources computed from
   * GateIN Management SPI
   * 
   * @return list of DocumentType templates managed paths.
   */
  Set<Node> getDocumentTypeTemplatesNodes();

  /**
   * Returns the list of sub resources of
   * /ecmadmin/templates/applications/metadata managed resources computed from
   * GateIN Management SPI
   * 
   * @return list of metadata templates managed paths.
   */
  Set<Node> getMetadataTemplatesNodes();

  /**
   * Returns the list of sub resources of /ecmadmin/taxonomy managed resources
   * computed from GateIN Management SPI
   * 
   * @return list of taxonomy managed paths.
   */
  Set<Node> getTaxonomyNodes();

  /**
   * Returns the list of sub resources of /ecmadmin/queries managed resources
   * computed from GateIN Management SPI
   * 
   * @return list of JCR Query managed paths.
   */
  Set<Node> getQueryNodes();

  /**
   * Returns the list of sub resources of /ecmadmin/drive managed resources
   * computed from GateIN Management SPI
   * 
   * @return list of ECMS Drives managed paths.
   */
  Set<Node> getDriveNodes();

  /**
   * Returns the list of sub resources of /ecmadmin/script managed resources
   * computed from GateIN Management SPI
   * 
   * @return list of ECMS Script managed paths.
   */
  Set<Node> getScriptNodes();

  /**
   * Returns the list of sub resources of /ecmadmin/action managed resources
   * computed from GateIN Management SPI
   * 
   * @return list of action nodetype managed paths.
   */
  Set<Node> getActionNodeTypeNodes();

  /**
   * Returns the list of sub resources of /ecmadmin/nodetype managed resources
   * computed from GateIN Management SPI
   * 
   * @return list of JCR Nodetype managed paths.
   */
  Set<Node> getNodeTypeNodes();

  /**
   * Returns the list of sub resources of /ecmadmin/taxonomy managed resources
   * computed from GateIN Management SPI
   * 
   * @return list of application registry categories managed paths.
   */
  Set<Node> getRegistryNodes();

  /**
   * Returns the list of sub resources of /ecmadmin/view/templates managed
   * resources computed from GateIN Management SPI
   * 
   * @return list of ECMS View Templates managed paths.
   */
  Set<Node> getViewTemplatesNodes();

  /**
   * Returns the list of sub resources of /ecmadmin/view managed resources
   * computed from GateIN Management SPI
   * 
   * @return list of ECMS View Configuration managed paths.
   */
  Set<Node> getViewConfigurationNodes();

  /**
   * Synchronize Managed Resources
   * 
   * @param selectedResources
   * @param options
   * @param host
   * @param port
   * 
   * @throws IOException
   */
  void synchronize(Set<String> selectedResources, Map<String, String> options, String isSSLString, String host, String port, String username, String password) throws IOException;

  /**
   * Filters subresources of parentPath. This operation retains only paths that
   * contains parentPath.
   * 
   * @param selectedResources
   *          Set of managed resources paths
   * @param parentPath
   *          parent resource path
   * @return Set of sub resources path of type String
   */
  Set<String> filterSelectedResources(Collection<String> selectedResources, String parentPath);

  /**
   * Execute an SQL JCR Query
   * 
   * @param sql
   * @param selectedResources 
   * @throws Exception 
   */
  Set<String> executeSQL(String sql, Set<String> selectedResources) throws Exception;

}
