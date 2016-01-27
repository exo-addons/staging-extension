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
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class SocialExtension extends AbstractManagementExtension {

  public static final String SPACE_RESOURCE = "space";
  public static final String DASHBOARD_PORTLET = "DashboardPortlet";
  public static final String FORUM_PORTLET = "ForumPortlet";
  public static final String WIKI_PORTLET = "WikiPortlet";
  public static final String CALENDAR_PORTLET = "CalendarPortlet";
  public static final String ANSWERS_PORTLET = "AnswersPortlet";
  public static final String FAQ_PORTLET = "FAQPortlet";

  public static final String SPACE_RESOURCE_PARENT_PATH = "social/space";
  public static final String SPACE_RESOURCE_PATH = "/social/space";
  public static final String FORUM_RESOURCE_PATH = "/forum/space";
  public static final String CALENDAR_RESOURCE_PATH = "/calendar/space";
  public static final String ANSWER_RESOURCE_PATH = "/answer/space";
  public static final String FAQ_RESOURCE_PATH = "/answer/template";
  public static final String WIKI_RESOURCE_PATH = "/wiki/group";
  public static final String CONTENT_RESOURCE_PATH = "/content/sites/shared";
  public static final String SITES_RESOURCE_PATH = "/site/groupsites";
  public static final String SITES_IMPORT_RESOURCE_PATH = "/site";
  public static final String GROUP_SITE_RESOURCE_PATH = "/group/spaces";

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