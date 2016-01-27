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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.chromattic.common.collection.Collections;
import org.exoplatform.calendar.service.Calendar;
import org.exoplatform.calendar.service.CalendarEvent;
import org.exoplatform.calendar.service.CalendarService;
import org.exoplatform.calendar.service.GroupCalendarData;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.management.calendar.CalendarExtension;
import org.exoplatform.management.common.exportop.AbstractExportOperationHandler;
import org.exoplatform.management.common.exportop.ActivityExportOperationInterface;
import org.exoplatform.management.common.exportop.SpaceMetadataExportTask;
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
import org.exoplatform.social.core.storage.api.IdentityStorage;
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
public class CalendarDataExportResource extends AbstractExportOperationHandler implements ActivityExportOperationInterface {

  private boolean groupCalendar;
  private boolean spaceCalendar;
  private String type;

  private UserACL userACL;
  private OrganizationService organizationService;
  private CalendarService calendarService;

  public CalendarDataExportResource(boolean groupCalendar, boolean spaceCalendar) {
    this.groupCalendar = groupCalendar;
    this.spaceCalendar = spaceCalendar;
    type = groupCalendar ? spaceCalendar ? CalendarExtension.SPACE_CALENDAR_TYPE : CalendarExtension.GROUP_CALENDAR_TYPE : CalendarExtension.PERSONAL_CALENDAR_TYPE;
  }

  @Override
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws ResourceNotFoundException, OperationException {
    calendarService = operationContext.getRuntimeContext().getRuntimeComponent(CalendarService.class);
    userACL = operationContext.getRuntimeContext().getRuntimeComponent(UserACL.class);
    organizationService = operationContext.getRuntimeContext().getRuntimeComponent(OrganizationService.class);
    spaceService = operationContext.getRuntimeContext().getRuntimeComponent(SpaceService.class);
    activityManager = operationContext.getRuntimeContext().getRuntimeComponent(ActivityManager.class);
    identityManager = operationContext.getRuntimeContext().getRuntimeComponent(IdentityManager.class);
    identityStorage = operationContext.getRuntimeContext().getRuntimeComponent(IdentityStorage.class);

    String excludeSpaceMetadataString = operationContext.getAttributes().getValue("exclude-space-metadata");
    boolean exportSpaceMetadata = excludeSpaceMetadataString == null || excludeSpaceMetadataString.trim().equalsIgnoreCase("false");

    List<ExportTask> exportTasks = new ArrayList<ExportTask>();

    if (groupCalendar) {
      String filterText = operationContext.getAttributes().getValue("filter");
      if (spaceCalendar) {
        Space space = spaceService.getSpaceByDisplayName(filterText);
        if (space == null) {
          throw new OperationException(OperationNames.EXPORT_RESOURCE, "Can't find space with display name: " + filterText);
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
                exportGroupCalendar(exportTasks, group.getId(), null, exportSpaceMetadata);
              }
            } else {
              if (!group.getId().startsWith(SpaceUtils.SPACE_GROUP + "/")) {
                exportGroupCalendar(exportTasks, group.getId(), null, exportSpaceMetadata);
              }
            }
          }
        } catch (Exception e) {
          throw new OperationException(OperationNames.EXPORT_RESOURCE, "Error while exporting calendars.", e);
        }
      } else {
        // Calendar groupId in case of space or Calendar name in case of simple
        // Group calendar
        exportGroupCalendar(exportTasks, spaceCalendar ? filterText : null, spaceCalendar ? null : filterText, exportSpaceMetadata);
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
            User[] usersArr = users.load(i, length);
            for (User user : usersArr) {
              exportUserCalendar(exportTasks, user.getUserName());
            }
          }
        } catch (Exception e) {
          throw new OperationException(OperationNames.EXPORT_RESOURCE, "Error while exporting calendars.", e);
        }
      } else {
        exportUserCalendar(exportTasks, username);
      }
    }
    resultHandler.completed(new ExportResourceModel(exportTasks));
  }

  private void exportGroupCalendar(List<ExportTask> exportTasks, String groupId, String calendarName, boolean exportSpaceMetadata) {
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

      Set<String> exportedSpaces = new HashSet<String>();
      for (Calendar calendar : calendars) {
        exportGroupCalendar(exportTasks, calendar);
        if (exportSpaceMetadata && spaceCalendar) {
          Space space = spaceService.getSpaceByGroupId(calendar.getCalendarOwner());
          if (space == null) {
            log.error("Can't export space of calendar '" + calendar.getName() + "', can't find space of owner : " + calendar.getCalendarOwner());
          } else {
            exportedSpaces.add(calendar.getCalendarOwner());
            String prefix = "calendar/space/" + CalendarExportTask.CALENDAR_SEPARATOR + calendar.getId() + "/";
            exportTasks.add(new SpaceMetadataExportTask(space, prefix));
          }
        }
      }
    } catch (Exception e) {
      throw new OperationException(OperationNames.EXPORT_RESOURCE, "Error occured while exporting Group Calendar data");
    }
  }

  private String[] getAllGroupIDs() throws Exception {
    @SuppressWarnings("unchecked")
    Collection<Group> groups = organizationService.getGroupHandler().getAllGroups();
    String[] groupIDs = new String[groups.size()];
    int i = 0;
    for (Group group : groups) {
      groupIDs[i++] = group.getId();
    }
    return groupIDs;
  }

  private void exportGroupCalendar(List<ExportTask> exportTasks, Calendar calendar) throws Exception {
    List<CalendarEvent> events = calendarService.getGroupEventByCalendar(Collections.list(calendar.getId()));
    exportTasks.add(new CalendarExportTask(type, calendar, events));

    if (events.size() > 0) {
      String spaceGroupId = SpaceUtils.SPACE_GROUP + "/" + calendar.getId().replace("_space_calendar", "");
      String prefix = "calendar/" + type + "/" + CalendarExportTask.CALENDAR_SEPARATOR + calendar.getId() + "/";
      exportActivities(exportTasks, spaceGroupId, prefix, CALENDAR_ACTIVITY_TYPE);
    }
  }

  private void exportUserCalendar(List<ExportTask> exportTasks, String username) {
    try {
      List<Calendar> userCalendars = calendarService.getUserCalendars(username, true);
      if (userCalendars.size() > 0) {
        for (Calendar calendar : userCalendars) {
          List<CalendarEvent> events = calendarService.getUserEventByCalendar(username, Collections.list(calendar.getId()));
          exportTasks.add(new CalendarExportTask(type, calendar, events));
          String prefix = "calendar/" + type + "/" + CalendarExportTask.CALENDAR_SEPARATOR + calendar.getId() + "/";
          exportActivities(exportTasks, username, prefix, CALENDAR_ACTIVITY_TYPE);
        }
      }
    } catch (Exception e) {
      throw new OperationException(OperationNames.EXPORT_RESOURCE, "Error occured while exporting Group Calendar data", e);
    }
  }

  public boolean isActivityValid(ExoSocialActivity activity) throws Exception {
    String eventId = activity.getTemplateParams().get(CalendarExtension.EVENT_ID_KEY);
    if (eventId == null) {
      log.warn("Can't find EventID param in calendar activity: " + activity.getTitle());
      return false;
    }
    CalendarEvent event = calendarService.getEventById(eventId);
    if (event == null) {
      log.warn("Can't find event of calendar activity: " + activity.getTitle());
      return false;
    }
    return true;
  }
}