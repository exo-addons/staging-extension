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
package org.exoplatform.management.social;

import org.exoplatform.management.common.AbstractManagementExtension;
import org.exoplatform.management.social.operations.SocialDataExportResource;
import org.exoplatform.management.social.operations.SocialDataImportResource;
import org.exoplatform.management.social.operations.SocialDataReadResource;
import org.gatein.management.api.ComponentRegistration;
import org.gatein.management.api.ManagedResource;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.spi.ExtensionContext;

/**
 * The Class SocialExtension.
 *
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class SocialExtension extends AbstractManagementExtension {

  /** The Constant SPACE_RESOURCE. */
  public static final String SPACE_RESOURCE = "space";
  
  /** The Constant DASHBOARD_PORTLET. */
  public static final String DASHBOARD_PORTLET = "DashboardPortlet";
  
  /** The Constant FORUM_PORTLET. */
  public static final String FORUM_PORTLET = "ForumPortlet";
  
  /** The Constant WIKI_PORTLET. */
  public static final String WIKI_PORTLET = "WikiPortlet";
  
  /** The Constant CALENDAR_PORTLET. */
  public static final String CALENDAR_PORTLET = "CalendarPortlet";

  /** The Constant SPACE_RESOURCE_PARENT_PATH. */
  public static final String SPACE_RESOURCE_PARENT_PATH = "social/space";
  
  /** The Constant SPACE_RESOURCE_PATH. */
  public static final String SPACE_RESOURCE_PATH = "/social/space";
  
  /** The Constant FORUM_RESOURCE_PATH. */
  public static final String FORUM_RESOURCE_PATH = "/forum/space";
  
  /** The Constant CALENDAR_RESOURCE_PATH. */
  public static final String CALENDAR_RESOURCE_PATH = "/calendar/space";
  
  /** The Constant WIKI_RESOURCE_PATH. */
  public static final String WIKI_RESOURCE_PATH = "/wiki/group";
  
  /** The Constant CONTENT_RESOURCE_PATH. */
  public static final String CONTENT_RESOURCE_PATH = "/content/sites/shared";
  
  /** The Constant SITES_RESOURCE_PATH. */
  public static final String SITES_RESOURCE_PATH = "/site/groupsites";
  
  /** The Constant SITES_IMPORT_RESOURCE_PATH. */
  public static final String SITES_IMPORT_RESOURCE_PATH = "/site";
  
  /** The Constant GROUP_SITE_RESOURCE_PATH. */
  public static final String GROUP_SITE_RESOURCE_PATH = "/group/spaces";

  /**
   * {@inheritDoc}
   */
  @Override
  public void initialize(ExtensionContext context) {
    ComponentRegistration socialRegistration = context.registerManagedComponent("social");

    ManagedResource.Registration social = socialRegistration.registerManagedResource(description("Social resources."));
    social.registerOperationHandler(OperationNames.READ_RESOURCE, new ReadResource("Lists available social resources", SPACE_RESOURCE), description("Lists available social resources"));

    ManagedResource.Registration spaces = social.registerSubResource(SPACE_RESOURCE, description("Spaces"));
    spaces.registerOperationHandler(OperationNames.READ_RESOURCE, new SocialDataReadResource(), description("Lists available spaces"));
    spaces.registerOperationHandler(OperationNames.IMPORT_RESOURCE, new SocialDataImportResource(), description("import spaces"));

    ManagedResource.Registration space = spaces.registerSubResource("{space-name: .*}", description("Space"));
    space.registerOperationHandler(OperationNames.EXPORT_RESOURCE, new SocialDataExportResource(), description("export space"));
    space.registerOperationHandler(OperationNames.IMPORT_RESOURCE, new SocialDataImportResource(), description("import space"));
    space.registerOperationHandler(OperationNames.READ_RESOURCE, new ReadResource("Empty resource"), description("Empty resource"));
  }
}