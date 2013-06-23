package org.exoplatform.extension.generator.service.api;

import java.io.InputStream;
import java.util.Set;


public interface ExtensionGenerator {

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
  public static final String REGISTRY_PATH = "/registry";
  
  Set<Node> getPortalSiteNodes();

  Set<Node> getGroupSiteNodes();

  Set<Node> getUserSiteNodes();

  Set<Node> getSiteContentNodes();

  Set<Node> getApplicationCLVTemplatesNodes();

  Set<Node> getApplicationSearchTemplatesNodes();

  Set<Node> getDocumentTypeTemplatesNodes();

  Set<Node> getMetadataTemplatesNodes();

  Set<Node> getTaxonomyNodes();

  Set<Node> getQueryNodes();

  Set<Node> getDriveNodes();

  Set<Node> getScriptNodes();

  Set<Node> getActionNodeTypeNodes();

  Set<Node> getNodeTypeNodes();

  Set<Node> getRegistryNodes();

  InputStream generateWARExtension(Set<String> selectedResources);

}
