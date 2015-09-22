/*
 * Copyright (C) 2003-2014 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.management.forum;

import java.util.Arrays;
import java.util.HashSet;

import org.exoplatform.management.common.AbstractManagementExtension;
import org.exoplatform.management.common.AbstractOperationHandler;
import org.exoplatform.management.forum.operations.ForumDataExportResource;
import org.exoplatform.management.forum.operations.ForumDataImportResource;
import org.exoplatform.management.forum.operations.ForumDataReadResource;
import org.exoplatform.management.forum.operations.ForumReadResource;
import org.exoplatform.management.forum.operations.ForumSettingsExportResource;
import org.exoplatform.management.forum.operations.ForumSettingsImportResource;
import org.gatein.management.api.ComponentRegistration;
import org.gatein.management.api.ManagedDescription;
import org.gatein.management.api.ManagedResource;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.exceptions.ResourceNotFoundException;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.ReadResourceModel;
import org.gatein.management.spi.ExtensionContext;

/**
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class ForumExtension extends AbstractManagementExtension {

  public static final String PUBLIC_FORUM_TYPE = "public";
  public static final String SPACE_FORUM_TYPE = "space";

  @Override
  public void initialize(ExtensionContext context) {
    ComponentRegistration forumRegistration = context.registerManagedComponent("forum");

    ManagedResource.Registration forum = forumRegistration.registerManagedResource(description("Forum resources."));

    forum.registerOperationHandler(OperationNames.READ_RESOURCE, new ForumReadResource(), description("Lists available forums"));

    ManagedResource.Registration settings = forum.registerSubResource("settings", description("Forum settings"));
    settings.registerOperationHandler(OperationNames.READ_RESOURCE, new ReadResource("Forum settings", "general-administration", "banned-ip", "user-profiles", "bb-codes", "tags"),
        description("Forum settings"));
    settings.registerOperationHandler(OperationNames.EXPORT_RESOURCE, new ForumSettingsExportResource(), description("export forum settings"));
    settings.registerOperationHandler(OperationNames.IMPORT_RESOURCE, new ForumSettingsImportResource(), description("import forum settings"));

    ManagedResource.Registration setting = settings.registerSubResource("{resource-name: .*}", description("Forum settings"));
    setting.registerOperationHandler(OperationNames.READ_RESOURCE, new ReadResource("Forum settings", "Settings"), description("Forum settings"));
    setting.registerOperationHandler(OperationNames.EXPORT_RESOURCE, new ForumSettingsExportResource(true), description("export forum setting"));

    ManagedResource.Registration portal = forum.registerSubResource(PUBLIC_FORUM_TYPE, description("public forum"));
    portal.registerOperationHandler(OperationNames.READ_RESOURCE, new ForumDataReadResource(false), description("Read non spaces forum categories"));
    portal.registerOperationHandler(OperationNames.EXPORT_RESOURCE, new ForumDataExportResource(false), description("export forum category"));
    portal.registerOperationHandler(OperationNames.IMPORT_RESOURCE, new ForumDataImportResource(false), description("import forum category"));

    ManagedResource.Registration group = forum.registerSubResource(SPACE_FORUM_TYPE, description("space forums"));
    group.registerOperationHandler(OperationNames.READ_RESOURCE, new ForumDataReadResource(true), description("Read spaces forum categories"));
    group.registerOperationHandler(OperationNames.EXPORT_RESOURCE, new ForumDataExportResource(true), description("export forum category"));
    group.registerOperationHandler(OperationNames.IMPORT_RESOURCE, new ForumDataImportResource(true), description("import forum category"));
  }
}