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
package org.exoplatform.management.calendar.operations;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.chromattic.common.collection.Collections;
import org.exoplatform.calendar.service.Calendar;
import org.exoplatform.calendar.service.CalendarEvent;
import org.exoplatform.calendar.service.CalendarService;
import org.exoplatform.calendar.service.GroupCalendarData;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.management.calendar.CalendarExtension;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.exceptions.ResourceNotFoundException;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.OperationHandler;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.ExportResourceModel;
import org.gatein.management.api.operation.model.ExportTask;

/**
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class CalendarDataExportResource implements OperationHandler {

  private boolean groupCalendar;
  private String type;

  public CalendarDataExportResource(boolean groupCalendar) {
    this.groupCalendar = groupCalendar;
    type = groupCalendar ? CalendarExtension.GROUP_CALENDAR_TYPE : CalendarExtension.PERSONAL_CALENDAR_TYPE;
  }

  @Override
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws ResourceNotFoundException, OperationException {
    CalendarService calendarService = operationContext.getRuntimeContext().getRuntimeComponent(CalendarService.class);
    UserACL userACL = operationContext.getRuntimeContext().getRuntimeComponent(UserACL.class);

    List<ExportTask> exportTasks = new ArrayList<ExportTask>();

    if (groupCalendar) {
      String groupId = operationContext.getAttributes().getValue("filter");
      if (groupId == null || groupId.trim().isEmpty()) {
        OrganizationService organizationService = operationContext.getRuntimeContext().getRuntimeComponent(OrganizationService.class);
        try {
          @SuppressWarnings("unchecked")
          Collection<Group> groups = organizationService.getGroupHandler().getAllGroups();
          for (Group group : groups) {
            exportGroupCalendar(calendarService, userACL, exportTasks, group.getId());
          }
        } catch (Exception e) {
          throw new OperationException(OperationNames.EXPORT_RESOURCE, "Error while exporting calendars.", e);
        }
      } else {
        exportGroupCalendar(calendarService, userACL, exportTasks, groupId);
      }
    } else {
      String username = operationContext.getAttributes().getValue("filter");
      if (username == null || username.trim().isEmpty()) {
        OrganizationService organizationService = operationContext.getRuntimeContext().getRuntimeComponent(OrganizationService.class);
        try {
          ListAccess<User> users = organizationService.getUserHandler().findAllUsers();
          int size = users.getSize(), i = 0;
          while (i < size) {
            int length = i + 10 < size ? 10 : size - i;
            User[] usersArr = users.load(0, length);
            for (User user : usersArr) {
              exportUserCalendar(calendarService, userACL, exportTasks, user.getUserName());
            }
          }
        } catch (Exception e) {
          throw new OperationException(OperationNames.EXPORT_RESOURCE, "Error while exporting calendars.", e);
        }
      } else {
        exportUserCalendar(calendarService, userACL, exportTasks, username);
      }
    }
    resultHandler.completed(new ExportResourceModel(exportTasks));
  }

  private void exportGroupCalendar(CalendarService calendarService, UserACL userACL, List<ExportTask> exportTasks, String groupId) {
    try {
      List<GroupCalendarData> groupCalendars = calendarService.getGroupCalendars(new String[] { groupId }, true, userACL.getSuperUser());
      if (groupCalendars.size() > 1) {
        throw new OperationException(OperationNames.EXPORT_RESOURCE, "More than one GroupCalendarData was returned by API for calendar: " + groupId);
      }
      if (groupCalendars.size() == 1) {
        for (GroupCalendarData groupCalendarData : groupCalendars) {
          List<Calendar> calendars = groupCalendarData.getCalendars();
          for (Calendar calendar : calendars) {
            List<CalendarEvent> events = calendarService.getGroupEventByCalendar(Collections.list(calendar.getId()));
            exportTasks.add(new CalendarExportTask(type, calendar, events));
          }
        }
      }
    } catch (Exception e) {
      throw new OperationException(OperationNames.EXPORT_RESOURCE, "Error occured while exporting Group Calendar data");
    }
  }

  private void exportUserCalendar(CalendarService calendarService, UserACL userACL, List<ExportTask> exportTasks, String username) {
    try {
      List<Calendar> userCalendars = calendarService.getUserCalendars(username, true);
      if (userCalendars.size() > 0) {
        for (Calendar calendar : userCalendars) {
          List<CalendarEvent> events = calendarService.getUserEventByCalendar(username, Collections.list(calendar.getId()));
          exportTasks.add(new CalendarExportTask(type, calendar, events));
        }
      }
    } catch (Exception e) {
      throw new OperationException(OperationNames.EXPORT_RESOURCE, "Error occured while exporting Group Calendar data");
    }
  }
}
