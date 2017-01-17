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

import com.thoughtworks.xstream.XStream;

import org.exoplatform.calendar.service.Calendar;
import org.exoplatform.calendar.service.CalendarEvent;
import org.gatein.management.api.operation.model.ExportTask;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.List;

/**
 * The Class CalendarExportTask.
 *
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class CalendarExportTask implements ExportTask {

  /** The Constant CALENDAR_SEPARATOR. */
  public static final String CALENDAR_SEPARATOR = "Calendar_";

  /** The type. */
  private final String type;
  
  /** The calendar. */
  private final Calendar calendar;
  
  /** The events. */
  private final List<CalendarEvent> events;

  /**
   * Instantiates a new calendar export task.
   *
   * @param type the type
   * @param calendar the calendar
   * @param events the events
   */
  public CalendarExportTask(String type, Calendar calendar, List<CalendarEvent> events) {
    this.events = events;
    this.calendar = calendar;
    this.type = type;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getEntry() {
    return new StringBuilder("calendar/").append(type).append("/").append(CALENDAR_SEPARATOR).append(calendar.getId()).append(".xml").toString();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void export(OutputStream outputStream) throws IOException {

    XStream xStream = new XStream();
    xStream.alias("Calendar", Calendar.class);
    xStream.alias("Event", CalendarEvent.class);
    OutputStreamWriter writer = new OutputStreamWriter(outputStream, "UTF-8");

    List<Object> objects = Arrays.asList(calendar, events);
    xStream.toXML(objects, writer);
    writer.flush();
  }
}
