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
import org.exoplatform.management.common.AbstractOperationHandler;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.manager.ActivityManager;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.space.SpaceUtils;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.social.core.storage.api.ActivityStorage;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.exceptions.ResourceNotFoundException;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.ExportResourceModel;
import org.gatein.management.api.operation.model.ExportTask;

/**
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class CalendarDataExportResource extends AbstractOperationHandler {

  private boolean groupCalendar;
  private boolean spaceCalendar;
  private String type;

  private IdentityManager identityManager;
  private OrganizationService organizationService;

  public CalendarDataExportResource(boolean groupCalendar, boolean spaceCalendar) {
    this.groupCalendar = groupCalendar;
    this.spaceCalendar = spaceCalendar;
    type = groupCalendar ? spaceCalendar ? CalendarExtension.SPACE_CALENDAR_TYPE : CalendarExtension.GROUP_CALENDAR_TYPE : CalendarExtension.PERSONAL_CALENDAR_TYPE;
  }

  @Override
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws ResourceNotFoundException, OperationException {
    CalendarService calendarService = operationContext.getRuntimeContext().getRuntimeComponent(CalendarService.class);
    spaceService = operationContext.getRuntimeContext().getRuntimeComponent(SpaceService.class);
    UserACL userACL = operationContext.getRuntimeContext().getRuntimeComponent(UserACL.class);
    organizationService = operationContext.getRuntimeContext().getRuntimeComponent(OrganizationService.class);
    activityManager = operationContext.getRuntimeContext().getRuntimeComponent(ActivityManager.class);
    activityStorage = operationContext.getRuntimeContext().getRuntimeComponent(ActivityStorage.class);
    identityManager = operationContext.getRuntimeContext().getRuntimeComponent(IdentityManager.class);

    String excludeSpaceMetadataString = operationContext.getAttributes().getValue("exclude-space-metadata");
    boolean exportSpaceMetadata = excludeSpaceMetadataString == null || excludeSpaceMetadataString.trim().equalsIgnoreCase("false");

    List<ExportTask> exportTasks = new ArrayList<ExportTask>();

    if (groupCalendar) {
      String filterText = operationContext.getAttributes().getValue("filter");
      if (spaceCalendar) {
        String displayName = filterText;
        Space space = spaceService.getSpaceByDisplayName(displayName);
        if (space == null) {
          throw new OperationException(OperationNames.EXPORT_RESOURCE, "Can't find space with display name: " + displayName);
        }
        filterText = space.getGroupId();
      }
      if (filterText == null || filterText.trim().isEmpty()) {
        try {
          @SuppressWarnings("unchecked")
          Collection<Group> groups = organizationService.getGroupHandler().getAllGroups();
          for (Group group : groups) {
            if (spaceCalendar) {
              if (group.getId().startsWith(SpaceUtils.SPACE_GROUP + "/")) {
                exportGroupCalendar(calendarService, userACL, exportTasks, group.getId(), null, exportSpaceMetadata);
              }
            } else {
              if (!group.getId().startsWith(SpaceUtils.SPACE_GROUP + "/")) {
                exportGroupCalendar(calendarService, userACL, exportTasks, group.getId(), null, exportSpaceMetadata);
              }
            }
          }
        } catch (Exception e) {
          throw new OperationException(OperationNames.EXPORT_RESOURCE, "Error while exporting calendars.", e);
        }
      } else {
        // Calendar groupId in case of space or Calendar name in case of simple
        // Group calendar
        exportGroupCalendar(calendarService, userACL, exportTasks, spaceCalendar ? filterText : null, spaceCalendar ? null : filterText, exportSpaceMetadata);
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

  private void exportGroupCalendar(CalendarService calendarService, UserACL userACL, List<ExportTask> exportTasks, String groupId, String calendarName, boolean exportSpaceMetadata) {
    try {
      List<GroupCalendarData> groupCalendars = calendarService.getGroupCalendars(groupId == null ? getAllGroupIDs() : new String[] { groupId }, true, userACL.getSuperUser());
      List<Calendar> calendars = new ArrayList<Calendar>();

      GROUP_CALENDAR_LOOP: for (GroupCalendarData groupCalendarData : groupCalendars) {
        if (groupCalendarData.getCalendars() != null) {
          if (calendarName != null && !calendarName.isEmpty()) {
            for (Calendar calendar : groupCalendarData.getCalendars()) {
              if (calendar.getName().equals(calendarName)) {
                calendars.add(calendar);
                break GROUP_CALENDAR_LOOP;
              }
            }
          } else {
            calendars.addAll(groupCalendarData.getCalendars());
          }
        }
      }

      for (Calendar calendar : calendars) {
        exportGroupCalendar(calendarService, exportTasks, calendar);
      }

      if (exportSpaceMetadata && spaceCalendar) {
        Space space = spaceService.getSpaceByGroupId(groupId);
        if (space != null) {
          exportTasks.add(new SpaceMetadataExportTask(space, groupId.replace(SpaceUtils.SPACE_GROUP + "/", "")));
        }
      }
    } catch (Exception e) {
      throw new OperationException(OperationNames.EXPORT_RESOURCE, "Error occured while exporting Group Calendar data");
    }
  }

  private String[] getAllGroupIDs() throws Exception {
    @SuppressWarnings("unchecked")
    Collection<Group> groups = organizationService.getGroupHandler().getAllGroups();
    List<String> groupIDs = new ArrayList<String>();
    for (Group group : groups) {
      groupIDs.add(group.getId());
    }
    return groupIDs.toArray(new String[0]);
  }

  private void exportGroupCalendar(CalendarService calendarService, List<ExportTask> exportTasks, Calendar calendar) throws Exception {
    List<CalendarEvent> events = calendarService.getGroupEventByCalendar(Collections.list(calendar.getId()));
    exportTasks.add(new CalendarExportTask(type, calendar, events));
    exportActivities(exportTasks, events);
  }

  private void exportUserCalendar(CalendarService calendarService, UserACL userACL, List<ExportTask> exportTasks, String username) {
    try {
      List<Calendar> userCalendars = calendarService.getUserCalendars(username, true);
      if (userCalendars.size() > 0) {
        for (Calendar calendar : userCalendars) {
          List<CalendarEvent> events = calendarService.getUserEventByCalendar(username, Collections.list(calendar.getId()));
          exportTasks.add(new CalendarExportTask(type, calendar, events));
          exportActivities(exportTasks, events);
        }
      }
    } catch (Exception e) {
      throw new OperationException(OperationNames.EXPORT_RESOURCE, "Error occured while exporting Group Calendar data");
    }
  }

  private void exportActivities(List<ExportTask> exportTasks, List<CalendarEvent> events) throws Exception {
    List<ExoSocialActivity> activitiesList = new ArrayList<ExoSocialActivity>();
    for (CalendarEvent event : events) {
      addActivityWithComments(activitiesList, event.getActivityId());
    }
    if (!activitiesList.isEmpty()) {
      exportTasks.add(new CalendarActivitiesExportTask(identityManager, activitiesList, type, events.get(0).getCalendarId()));
    }
  }
}
