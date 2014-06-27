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
package org.exoplatform.management.social.operations;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.gatein.management.api.operation.model.ExportTask;

import com.thoughtworks.xstream.XStream;

/**
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class SpaceActivitiesExportTask implements ExportTask {

  public static final String FILENAME = "activities";

  private final ExoSocialActivity[] activities;
  private final String spacePrettyName;
  private final Integer index;

  public SpaceActivitiesExportTask(ExoSocialActivity[] activities, String spacePrettyName, int index) {
    this.index = index;
    this.spacePrettyName = spacePrettyName;
    this.activities = activities;
  }

  @Override
  public String getEntry() {
    return new StringBuilder("social/space/").append(spacePrettyName).append("/").append(FILENAME).append(index).append(".metadata").toString();
  }

  @Override
  public void export(OutputStream outputStream) throws IOException {
    XStream xStream = new XStream();
    OutputStreamWriter writer = new OutputStreamWriter(outputStream, "UTF-8");
    xStream.omitField(ExoSocialActivity.class, "id");
    xStream.toXML(activities, writer);
    writer.flush();
  }
}
