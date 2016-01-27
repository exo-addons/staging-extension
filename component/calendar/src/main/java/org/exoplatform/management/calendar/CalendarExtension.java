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
package org.exoplatform.management.calendar;

import org.exoplatform.management.calendar.operations.CalendarDataExportResource;
import org.exoplatform.management.calendar.operations.CalendarDataImportResource;
import org.exoplatform.management.calendar.operations.CalendarDataReadResource;
import org.exoplatform.management.common.AbstractManagementExtension;
import org.gatein.management.api.ComponentRegistration;
import org.gatein.management.api.ManagedResource;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.spi.ExtensionContext;

/**
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class CalendarExtension extends AbstractManagementExtension {

  public static final String EVENT_LINK_KEY = "EventLink";
  public static final String EVENT_ID_KEY = "EventID";
  public static final String SPACE_CALENDAR_TYPE = "space";
  public static final String GROUP_CALENDAR_TYPE = "group";
  public static final String PERSONAL_CALENDAR_TYPE = "personal";

  @Override
  public void initialize(ExtensionContext context) {
    ComponentRegistration calendarRegistration = context.registerManagedComponent("calendar");

    ManagedResource.Registration calendar = calendarRegistration.registerManagedResource(description("calendar resources."));

    calendar.registerOperationHandler(OperationNames.READ_RESOURCE, new ReadResource("Lists available calendars", SPACE_CALENDAR_TYPE, GROUP_CALENDAR_TYPE, PERSONAL_CALENDAR_TYPE), description("Lists available calendars"));

    ManagedResource.Registration space = calendar.registerSubResource(SPACE_CALENDAR_TYPE, description("space calendar"));
    space.registerOperationHandler(OperationNames.READ_RESOURCE, new CalendarDataReadResource(true, true), description("Read space calendars"));
    space.registerOperationHandler(OperationNames.EXPORT_RESOURCE, new CalendarDataExportResource(true, true), description("export space calendar"));
    space.registerOperationHandler(OperationNames.IMPORT_RESOURCE, new CalendarDataImportResource(true, true), description("import space calendar"));

    ManagedResource.Registration group = calendar.registerSubResource(GROUP_CALENDAR_TYPE, description("group calendar"));
    group.registerOperationHandler(OperationNames.READ_RESOURCE, new CalendarDataReadResource(true, false), description("Read group calendars"));
    group.registerOperationHandler(OperationNames.EXPORT_RESOURCE, new CalendarDataExportResource(true, false), description("export group calendar"));
    group.registerOperationHandler(OperationNames.IMPORT_RESOURCE, new CalendarDataImportResource(true, false), description("import group calendar"));

    ManagedResource.Registration personal = calendar.registerSubResource(PERSONAL_CALENDAR_TYPE, description("personal calendar"));
    personal.registerOperationHandler(OperationNames.READ_RESOURCE, new CalendarDataReadResource(false, false), description("Read personal calendars"));
    personal.registerOperationHandler(OperationNames.EXPORT_RESOURCE, new CalendarDataExportResource(false, false), description("export personal calendar"));
    personal.registerOperationHandler(OperationNames.IMPORT_RESOURCE, new CalendarDataImportResource(false, false), description("import personal calendar"));
  }
}