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
package org.exoplatform.management.answer.operations;

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

  private final Space space;
  private final String faqId;

  public SpaceMetadataExportTask(Space space, String faqId) {
    this.space = space;
    this.faqId = faqId;
  }

  @Override
  public String getEntry() {
    return getEntryPath(faqId);
  }

  public static String getEntryPath(String faqId) {
    return new StringBuilder("answer/space/").append(faqId).append("/").append(FILENAME).toString();
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
