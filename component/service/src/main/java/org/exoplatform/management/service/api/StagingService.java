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
package org.exoplatform.management.service.api;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The Interface StagingService.
 */
public interface StagingService {

  /** The Constant CALENDARS_PARENT_PATH. */
  public static final String CALENDARS_PARENT_PATH = "/calendar";
  
  /** The Constant GROUP_CALENDAR_PATH. */
  public static final String GROUP_CALENDAR_PATH = "/calendar/group";
  
  /** The Constant SPACE_CALENDAR_PATH. */
  public static final String SPACE_CALENDAR_PATH = "/calendar/space";
  
  /** The Constant PERSONAL_FORUM_PATH. */
  public static final String PERSONAL_FORUM_PATH = "/calendar/personal";
  
  /** The Constant FORUMS_PARENT_PATH. */
  public static final String FORUMS_PARENT_PATH = "/forum";
  
  /** The Constant PUBLIC_FORUM_PATH. */
  public static final String PUBLIC_FORUM_PATH = "/forum/public";
  
  /** The Constant SPACE_FORUM_PATH. */
  public static final String SPACE_FORUM_PATH = "/forum/space";
  
  /** The Constant FORUM_SETTINGS. */
  public static final String FORUM_SETTINGS = "/forum/settings";
  
  /** The Constant SOCIAL_PARENT_PATH. */
  public static final String SOCIAL_PARENT_PATH = "/social";
  
  /** The Constant SOCIAL_SPACE_PATH. */
  public static final String SOCIAL_SPACE_PATH = "/social/space";
  
  /** The Constant WIKIS_PARENT_PATH. */
  public static final String WIKIS_PARENT_PATH = "/wiki";
  
  /** The Constant USER_WIKIS_PATH. */
  public static final String USER_WIKIS_PATH = "/wiki/user";
  
  /** The Constant GROUP_WIKIS_PATH. */
  public static final String GROUP_WIKIS_PATH = "/wiki/group";
  
  /** The Constant PORTAL_WIKIS_PATH. */
  public static final String PORTAL_WIKIS_PATH = "/wiki/portal";
  
  /** The Constant SITES_PARENT_PATH. */
  public static final String SITES_PARENT_PATH = "/site";
  
  /** The Constant SITES_PORTAL_PATH. */
  public static final String SITES_PORTAL_PATH = "/site/portalsites";
  
  /** The Constant SITES_GROUP_PATH. */
  public static final String SITES_GROUP_PATH = "/site/groupsites";
  
  /** The Constant SITES_USER_PATH. */
  public static final String SITES_USER_PATH = "/site/usersites";
  
  /** The Constant CONTENT_SITES_PATH. */
  public static final String CONTENT_SITES_PATH = "/content/sites";
  
  /** The Constant ECM_TEMPLATES_APPLICATION_CLV_PATH. */
  public static final String ECM_TEMPLATES_APPLICATION_CLV_PATH = "/ecmadmin/templates/applications/content-list-viewer";
  
  /** The Constant ECM_TEMPLATES_DOCUMENT_TYPE_PATH. */
  public static final String ECM_TEMPLATES_DOCUMENT_TYPE_PATH = "/ecmadmin/templates/nodetypes";
  
  /** The Constant ECM_TEMPLATES_METADATA_PATH. */
  public static final String ECM_TEMPLATES_METADATA_PATH = "/ecmadmin/templates/metadata";
  
  /** The Constant ECM_TAXONOMY_PATH. */
  public static final String ECM_TAXONOMY_PATH = "/ecmadmin/taxonomy";
  
  /** The Constant ECM_QUERY_PATH. */
  public static final String ECM_QUERY_PATH = "/ecmadmin/queries";
  
  /** The Constant ECM_DRIVE_PATH. */
  public static final String ECM_DRIVE_PATH = "/ecmadmin/drive";
  
  /** The Constant ECM_SCRIPT_PATH. */
  public static final String ECM_SCRIPT_PATH = "/ecmadmin/script";
  
  /** The Constant ECM_NODETYPE_PATH. */
  public static final String ECM_NODETYPE_PATH = "/ecmadmin/nodetype";
  
  /** The Constant ECM_VIEW_CONFIGURATION_PATH. */
  public static final String ECM_VIEW_CONFIGURATION_PATH = "/ecmadmin/view/configuration";
  
  /** The Constant ECM_VIEW_TEMPLATES_PATH. */
  public static final String ECM_VIEW_TEMPLATES_PATH = "/ecmadmin/view/templates";
  
  /** The Constant REGISTRY_PATH. */
  public static final String REGISTRY_PATH = "/registry";
  
  /** The Constant GADGET_PATH. */
  public static final String GADGET_PATH = "/gadget";
  
  /** The Constant USERS_PATH. */
  public static final String USERS_PATH = "/organization/user";
  
  /** The Constant GROUPS_PATH. */
  public static final String GROUPS_PATH = "/organization/group";
  
  /** The Constant ROLE_PATH. */
  public static final String ROLE_PATH = "/organization/role";
  
  /** The Constant BACKUP_PATH. */
  public static final String BACKUP_PATH = "/backup";

  /**
   * Export selected resources with selected options.
   *
   * @param selectedResourceCategoriesWithExceptions the selected resource categories with exceptions
   * @return the file
   * @throws Exception the exception
   */
  public File export(List<ResourceCategory> selectedResourceCategoriesWithExceptions) throws Exception;

  /**
   * Import resources.
   *
   * @param selectedResourcePath the selected resource path
   * @param inputStream the input stream
   * @param attributes the attributes
   * @throws IOException Signals that an I/O exception has occurred.
   */
  void importResource(String selectedResourcePath, InputStream inputStream, Map<String, List<String>> attributes) throws IOException;

  /**
   * Returns the list of sub resources of the given path.
   *
   * @param path the path
   * @return list of resources
   */
  public Set<Resource> getResources(String path);

  /**
   * Returns the list of sub resources of portal wikis computed from GateIN
   * Management SPI.
   *
   * @return list of portal sites managed paths.
   */
  Set<Resource> getWikiPortalResources();

  /**
   * Gets the wiki group resources.
   *
   * @return the wiki group resources
   */
  Set<Resource> getWikiGroupResources();

  /**
   * Gets the wiki user resources.
   *
   * @return the wiki user resources
   */
  Set<Resource> getWikiUserResources();

  /**
   * Execute an SQL JCR Query.
   *
   * @param sql the sql
   * @param sites the sites
   * @return the sets the
   * @throws Exception the exception
   */
  Set<String> executeSQL(String sql, Set<String> sites) throws Exception;

}
