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
package org.exoplatform.management.forum;

import org.exoplatform.management.common.AbstractManagementExtension;
import org.exoplatform.management.forum.operations.ForumDataExportResource;
import org.exoplatform.management.forum.operations.ForumDataImportResource;
import org.exoplatform.management.forum.operations.ForumDataReadResource;
import org.exoplatform.management.forum.operations.ForumReadResource;
import org.exoplatform.management.forum.operations.ForumSettingsExportResource;
import org.exoplatform.management.forum.operations.ForumSettingsImportResource;
import org.gatein.management.api.ComponentRegistration;
import org.gatein.management.api.ManagedResource;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.spi.ExtensionContext;

/**
 * The Class ForumExtension.
 *
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class ForumExtension extends AbstractManagementExtension {

  /** The Constant PUBLIC_FORUM_TYPE. */
  public static final String PUBLIC_FORUM_TYPE = "public";
  
  /** The Constant SPACE_FORUM_TYPE. */
  public static final String SPACE_FORUM_TYPE = "space";

  /**
   * {@inheritDoc}
   */
  @Override
  public void initialize(ExtensionContext context) {
    ComponentRegistration forumRegistration = context.registerManagedComponent("forum");

    ManagedResource.Registration forum = forumRegistration.registerManagedResource(description("Forum resources."));

    forum.registerOperationHandler(OperationNames.READ_RESOURCE, new ForumReadResource(), description("Lists available forums"));

    ManagedResource.Registration settings = forum.registerSubResource("settings", description("Forum settings"));
    settings.registerOperationHandler(OperationNames.READ_RESOURCE, new ReadResource("Forum settings", "general-administration", "banned-ip", "user-profiles", "bb-codes", "tags"), description("Forum settings"));
    settings.registerOperationHandler(OperationNames.EXPORT_RESOURCE, new ForumSettingsExportResource(), description("export forum settings"));
    settings.registerOperationHandler(OperationNames.IMPORT_RESOURCE, new ForumSettingsImportResource(), description("import forum settings"));

    ManagedResource.Registration setting = settings.registerSubResource("{resource-name: .*}", description("Forum settings"));
    setting.registerOperationHandler(OperationNames.READ_RESOURCE, new ReadResource("Forum settings", "Settings"), description("Forum settings"));
    setting.registerOperationHandler(OperationNames.EXPORT_RESOURCE, new ForumSettingsExportResource(true), description("export forum setting"));

    ManagedResource.Registration portal = forum.registerSubResource(PUBLIC_FORUM_TYPE, description("public forum categories"));
    portal.registerOperationHandler(OperationNames.READ_RESOURCE, new ForumDataReadResource(false), description("Read public forum categories"));
    portal.registerOperationHandler(OperationNames.EXPORT_RESOURCE, new ForumDataExportResource(false), description("export forum category"));
    portal.registerOperationHandler(OperationNames.IMPORT_RESOURCE, new ForumDataImportResource(false), description("import forum categories"));

    ManagedResource.Registration portalForumCategory = portal.registerSubResource("{name: .*}", description("public forum categories"));
    portalForumCategory.registerOperationHandler(OperationNames.READ_RESOURCE, new EmptyReadResource(), description("Empty sub resources"));
    portalForumCategory.registerOperationHandler(OperationNames.EXPORT_RESOURCE, new ForumDataExportResource(false), description("export forum category"));

    ManagedResource.Registration group = forum.registerSubResource(SPACE_FORUM_TYPE, description("space forums"));
    group.registerOperationHandler(OperationNames.READ_RESOURCE, new ForumDataReadResource(true), description("Read spaces forum categories"));
    group.registerOperationHandler(OperationNames.EXPORT_RESOURCE, new ForumDataExportResource(true), description("export forum category"));
    group.registerOperationHandler(OperationNames.IMPORT_RESOURCE, new ForumDataImportResource(true), description("import forum space categories"));

    ManagedResource.Registration groupForumCategory = group.registerSubResource("{name: .*}", description("space forum categories"));
    groupForumCategory.registerOperationHandler(OperationNames.READ_RESOURCE, new EmptyReadResource(), description("Empty sub resources"));
    groupForumCategory.registerOperationHandler(OperationNames.EXPORT_RESOURCE, new ForumDataExportResource(true), description("export forum category"));
  }
}