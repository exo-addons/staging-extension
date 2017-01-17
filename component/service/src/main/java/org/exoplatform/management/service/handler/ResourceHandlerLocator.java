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
package org.exoplatform.management.service.handler;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.management.service.api.ResourceHandler;
import org.exoplatform.management.service.api.StagingService;
import org.exoplatform.management.service.handler.common.ResourcesInFilterHandler;
import org.exoplatform.management.service.handler.common.SimpleHandler;
import org.exoplatform.management.service.handler.forum.ForumHandler;
import org.exoplatform.management.service.handler.mop.MOPSiteHandler;
import org.exoplatform.portal.mop.SiteType;

import java.util.Arrays;

/**
 * Service Locator for resources handlers, based on the resource category's path.
 *
 * @author Thomas Delhoménie
 */
public class ResourceHandlerLocator {
  
  /** The registry. */
  private static ResourceHandlerRegistry registry;

  static {
    registry = new ResourceHandlerRegistry();

    // Backup
    registry.register(new SimpleHandler(StagingService.BACKUP_PATH));

    // Social
    registry.register(new SimpleHandler(StagingService.SOCIAL_SPACE_PATH));

    // Wiki
    registry.register(new ResourcesInFilterHandler(StagingService.PORTAL_WIKIS_PATH));
    registry.register(new ResourcesInFilterHandler(StagingService.GROUP_WIKIS_PATH));
    registry.register(new ResourcesInFilterHandler(StagingService.USER_WIKIS_PATH));

    // Forum
    registry.register(new ForumHandler(StagingService.PUBLIC_FORUM_PATH));
    registry.register(new ForumHandler(StagingService.SPACE_FORUM_PATH));
    registry.register(new ForumHandler(StagingService.FORUM_SETTINGS));

    // Calendar
    registry.register(new ResourcesInFilterHandler(StagingService.GROUP_CALENDAR_PATH));
    registry.register(new ResourcesInFilterHandler(StagingService.SPACE_CALENDAR_PATH));
    registry.register(new ResourcesInFilterHandler(StagingService.PERSONAL_FORUM_PATH));

    // ECM Admin Handlers
    registry.register(new ResourcesInFilterHandler(StagingService.ECM_NODETYPE_PATH));
    registry.register(new ResourcesInFilterHandler(StagingService.ECM_SCRIPT_PATH));
    registry.register(new ResourcesInFilterHandler(StagingService.ECM_DRIVE_PATH));
    registry.register(new ResourcesInFilterHandler(StagingService.ECM_QUERY_PATH));
    registry.register(new ResourcesInFilterHandler(StagingService.ECM_TEMPLATES_METADATA_PATH));
    registry.register(new ResourcesInFilterHandler(StagingService.ECM_TEMPLATES_DOCUMENT_TYPE_PATH));
    registry.register(new SimpleHandler(StagingService.ECM_TEMPLATES_APPLICATION_CLV_PATH));
    registry.register(new SimpleHandler(StagingService.ECM_TAXONOMY_PATH));
    registry.register(new ResourcesInFilterHandler(StagingService.ECM_VIEW_TEMPLATES_PATH));
    registry.register(new ResourcesInFilterHandler(StagingService.ECM_VIEW_CONFIGURATION_PATH));

    // Organization Handlers
    registry.register(new SimpleHandler(StagingService.USERS_PATH));
    registry.register(new SimpleHandler(StagingService.GROUPS_PATH));
    registry.register(new SimpleHandler(StagingService.ROLE_PATH));

    // Gadget Handler
    registry.register(new SimpleHandler(StagingService.GADGET_PATH));

    // Aplication Registry Handler
    registry.register(new SimpleHandler(StagingService.REGISTRY_PATH));

    // Sites JCR Content Handler
    registry.register(new SimpleHandler(StagingService.CONTENT_SITES_PATH));

    // MOP Handlers
    registry.register(new MOPSiteHandler(SiteType.PORTAL));
    registry.register(new MOPSiteHandler(SiteType.GROUP));
    registry.register(new MOPSiteHandler(SiteType.USER));
  }

  /**
   * Gets the resource handler.
   *
   * @param path the path
   * @return the resource handler
   */
  public static ResourceHandler getResourceHandler(String path) {
    return registry.get(path);
  }

  /**
   * Find resource by path.
   *
   * @param path the path
   * @return the resource handler
   */
  public static ResourceHandler findResourceByPath(String path) {
    String[] fileNameParts = path.split("/");
    for (int i = fileNameParts.length; i > 0; i--) {
      String tmpPath = StringUtils.join(Arrays.copyOfRange(fileNameParts, 0, i), "/");
      ResourceHandler handler = getResourceHandler(tmpPath);
      if (handler != null) {
        return handler;
      }
    }
    return null;
  }
}
