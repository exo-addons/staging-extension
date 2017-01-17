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
package org.exoplatform.management.common.exportop;

import com.thoughtworks.xstream.XStream;

import org.exoplatform.management.common.SpaceMetaData;
import org.exoplatform.social.core.space.model.Space;
import org.gatein.management.api.operation.model.ExportTask;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

/**
 * The Class SpaceMetadataExportTask.
 *
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class SpaceMetadataExportTask implements ExportTask {

  /** The Constant FILENAME. */
  public static final String FILENAME = "space.metadata";

  /** The space. */
  protected final Space space;
  
  /** The entry name. */
  protected final String entryName;

  /**
   * Instantiates a new space metadata export task.
   *
   * @param space the space
   * @param prefix the prefix
   */
  public SpaceMetadataExportTask(Space space, String prefix) {
    this.space = space;
    this.entryName = (prefix.endsWith("/") ? prefix : (prefix + "/")) + FILENAME;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getEntry() {
    return entryName;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final void export(OutputStream outputStream) throws IOException {
    SpaceMetaData metaData = new SpaceMetaData(space);

    XStream xStream = new XStream();
    xStream.alias("metadata", SpaceMetaData.class);
    OutputStreamWriter writer = new OutputStreamWriter(outputStream, "UTF-8");
    xStream.toXML(metaData, writer);
    writer.flush();
  }
}
