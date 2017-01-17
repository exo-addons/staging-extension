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
package org.exoplatform.management.calendar.operations;

import org.exoplatform.calendar.service.Calendar;
import org.exoplatform.calendar.service.CalendarService;
import org.exoplatform.calendar.service.GroupCalendarData;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.management.common.AbstractOperationHandler;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.social.core.space.SpaceUtils;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.exceptions.ResourceNotFoundException;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.ReadResourceModel;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The Class CalendarDataReadResource.
 *
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class CalendarDataReadResource extends AbstractOperationHandler {

  /** The group calendar. */
  private boolean groupCalendar;
  
  /** The space calendar. */
  private boolean spaceCalendar;

  /**
   * Instantiates a new calendar data read resource.
   *
   * @param groupCalendar the group calendar
   * @param spaceCalendar the space calendar
   */
  public CalendarDataReadResource(boolean groupCalendar, boolean spaceCalendar) {
    this.groupCalendar = groupCalendar;
    this.spaceCalendar = spaceCalendar;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws ResourceNotFoundException, OperationException {
    Set<String> children = new LinkedHashSet<String>();
    OrganizationService organizationService = operationContext.getRuntimeContext().getRuntimeComponent(OrganizationService.class);
    SpaceService spaceService = operationContext.getRuntimeContext().getRuntimeComponent(SpaceService.class);
    CalendarService calendarService = operationContext.getRuntimeContext().getRuntimeComponent(CalendarService.class);
    UserACL userACL = operationContext.getRuntimeContext().getRuntimeComponent(UserACL.class);

    try {
      if (groupCalendar) {
        Collection<Group> groups = organizationService.getGroupHandler().getAllGroups();
        for (Group group : groups) {
          if (spaceCalendar) {
            if (group.getId().startsWith(SpaceUtils.SPACE_GROUP + "/")) {
              Space space = spaceService.getSpaceByGroupId(group.getId());
              if (space == null) {
                continue;
              }
              List<GroupCalendarData> calendars = calendarService.getGroupCalendars(new String[] { group.getId() }, true, userACL.getSuperUser());
              if (calendars != null && calendars.size() > 0) {
                children.add(space.getDisplayName());
              }
            }
          } else {
            if (!group.getId().startsWith(SpaceUtils.SPACE_GROUP + "/")) {
              List<GroupCalendarData> calendarsData = calendarService.getGroupCalendars(new String[] { group.getId() }, true, userACL.getSuperUser());
              if (calendarsData != null && calendarsData.size() > 0) {
                for (GroupCalendarData groupCalendarData : calendarsData) {
                  List<Calendar> calendars = groupCalendarData.getCalendars();
                  for (Calendar calendar : calendars) {
                    children.add(calendar.getName());
                  }
                }
              }
            }
          }
        }
      } else {
        ListAccess<User> users = organizationService.getUserHandler().findAllUsers();
        int size = users.getSize(), i = 0;
        while (i < size) {
          int length = i + 10 < size ? 10 : size - i;
          User[] usersArr = users.load(i, length);
          for (User user : usersArr) {
            children.add(user.getUserName());
          }
          i += 10;
        }
      }
    } catch (Exception e) {
      throw new OperationException(OperationNames.READ_RESOURCE, "Error while listing calendars.", e);
    }
    resultHandler.completed(new ReadResourceModel("All calendars:", children));
  }
}
