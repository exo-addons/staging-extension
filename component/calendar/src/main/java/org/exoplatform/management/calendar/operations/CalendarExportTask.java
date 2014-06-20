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

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.List;

import org.chromattic.common.collection.Collections;
import org.exoplatform.calendar.service.Calendar;
import org.exoplatform.calendar.service.CalendarEvent;
import org.gatein.management.api.operation.model.ExportTask;

import com.thoughtworks.xstream.XStream;

/**
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class CalendarExportTask implements ExportTask {

  public static final String CALENDAR_SEPARATOR = "Calendar_";

  private final String type;
  private final Calendar calendar;
  private final List<CalendarEvent> events;

  public CalendarExportTask(String type, Calendar calendar, List<CalendarEvent> events) {
    this.events = events;
    this.calendar = calendar;
    this.type = type;
  }

  @Override
  public String getEntry() {
    return new StringBuilder("calendar/").append(type).append("/").append(CALENDAR_SEPARATOR).append(calendar.getId()).append(".xml").toString();
  }

  @Override
  public void export(OutputStream outputStream) throws IOException {

    XStream xStream = new XStream();
    xStream.alias("Calendar", Calendar.class);
    xStream.alias("Event", CalendarEvent.class);
    OutputStreamWriter writer = new OutputStreamWriter(outputStream, "UTF-8");

    List<Object> objects = Collections.list(calendar, events);
    xStream.toXML(objects, writer);
    writer.flush();
  }
}
