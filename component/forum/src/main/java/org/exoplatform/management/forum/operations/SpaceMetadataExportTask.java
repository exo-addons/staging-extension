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
package org.exoplatform.management.forum.operations;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import org.exoplatform.social.core.space.model.Space;
import org.gatein.management.api.operation.model.ExportTask;

import com.thoughtworks.xstream.XStream;

/**
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class SpaceMetadataExportTask implements ExportTask {

  public static final String FILENAME = "space.metadata";

  private final String forumId;
  private final Space space;

  public SpaceMetadataExportTask(Space space, String forumId) {
    this.space = space;
    this.forumId = forumId;
  }

  @Override
  public String getEntry() {
    return getEntryPath(forumId);
  }

  public static String getEntryPath(String forumId) {
    return new StringBuilder("forum/space/").append(forumId).append("/").append(FILENAME).toString();
  }

  @Override
  public void export(OutputStream outputStream) throws IOException {
    SpaceMetaData metaData = new SpaceMetaData(space);

    XStream xStream = new XStream();
    xStream.alias("metadata", SpaceMetaData.class);
    OutputStreamWriter writer = new OutputStreamWriter(outputStream, "UTF-8");
    xStream.toXML(metaData, writer);
    writer.flush();
  }

}
