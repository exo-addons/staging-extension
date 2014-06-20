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

import java.util.Arrays;
import java.util.HashSet;

import org.exoplatform.management.calendar.operations.CalendarDataExportResource;
import org.exoplatform.management.calendar.operations.CalendarDataImportResource;
import org.exoplatform.management.calendar.operations.CalendarDataReadResource;
import org.gatein.management.api.ComponentRegistration;
import org.gatein.management.api.ManagedDescription;
import org.gatein.management.api.ManagedResource;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.exceptions.ResourceNotFoundException;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.OperationHandler;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.ReadResourceModel;
import org.gatein.management.spi.ExtensionContext;
import org.gatein.management.spi.ManagementExtension;

/**
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class CalendarExtension implements ManagementExtension {

  public static final String GROUP_CALENDAR_TYPE = "group";
  public static final String PERSONAL_CALENDAR_TYPE = "personal";

  @Override
  public void initialize(ExtensionContext context) {
    ComponentRegistration calendarRegistration = context.registerManagedComponent("calendar");

    ManagedResource.Registration calendar = calendarRegistration.registerManagedResource(description("calendar resources."));

    calendar.registerOperationHandler(OperationNames.READ_RESOURCE, new ReadResource("Lists available calendars", GROUP_CALENDAR_TYPE, PERSONAL_CALENDAR_TYPE),
        description("Lists available calendars"));

    ManagedResource.Registration group = calendar.registerSubResource(GROUP_CALENDAR_TYPE, description("group calendar"));
    group.registerOperationHandler(OperationNames.READ_RESOURCE, new CalendarDataReadResource(true), description("Read group calendars"));
    group.registerOperationHandler(OperationNames.EXPORT_RESOURCE, new CalendarDataExportResource(true), description("export group calendar"));
    group.registerOperationHandler(OperationNames.IMPORT_RESOURCE, new CalendarDataImportResource(true), description("import group calendar"));

    ManagedResource.Registration personal = calendar.registerSubResource(PERSONAL_CALENDAR_TYPE, description("personal calendar"));
    personal.registerOperationHandler(OperationNames.READ_RESOURCE, new CalendarDataReadResource(false), description("Read personal calendars"));
    personal.registerOperationHandler(OperationNames.EXPORT_RESOURCE, new CalendarDataExportResource(false), description("export personal calendar"));
    personal.registerOperationHandler(OperationNames.IMPORT_RESOURCE, new CalendarDataImportResource(false), description("import personal calendar"));
  }

  @Override
  public void destroy() {
  }

  private static ManagedDescription description(final String description) {
    return new ManagedDescription() {
      @Override
      public String getDescription() {
        return description;
      }
    };
  }

  public static class ReadResource implements OperationHandler {
    private String[] values;
    private String description;

    public ReadResource(String description, String... values) {
      this.values = values;
      this.description = description;
    }

    @Override
    public void execute(OperationContext operationContext, ResultHandler resultHandler) throws ResourceNotFoundException, OperationException {
      resultHandler.completed(new ReadResourceModel(description, values == null ? new HashSet<String>() : new HashSet<String>(Arrays.asList(values))));
    }

  }
}