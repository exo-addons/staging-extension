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

import java.util.List;

import org.exoplatform.management.common.AbstractActivitiesExportTask;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.manager.IdentityManager;

/**
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class CalendarActivitiesExportTask extends AbstractActivitiesExportTask {

  public static final String FILENAME = "/CalendarActivities.metadata";

  private final String type;
  private final String calendarId;

  public CalendarActivitiesExportTask(IdentityManager identityManager, List<ExoSocialActivity> activitiesList, String type, String calendarId) {
    super(identityManager, activitiesList);
    this.calendarId = calendarId;
    this.type = type;
  }

  @Override
  public String getEntry() {
    return new StringBuilder("calendar/").append(type).append("/").append(CalendarExportTask.CALENDAR_SEPARATOR).append(calendarId).append(FILENAME).toString();
  }

}
