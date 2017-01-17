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
package org.exoplatform.management.content.operations.site.contents;

import com.thoughtworks.xstream.XStream;

import org.exoplatform.management.content.operations.site.SiteUtil;
import org.gatein.management.api.operation.model.ExportTask;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

/**
 * The Class SiteMetaDataExportTask.
 *
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class SiteMetaDataExportTask implements ExportTask {

  /** The Constant FILENAME. */
  public static final String FILENAME = "metadata.xml";

  /** The meta data. */
  private SiteMetaData metaData = null;

  /**
   * Instantiates a new site meta data export task.
   *
   * @param metaData the meta data
   */
  public SiteMetaDataExportTask(SiteMetaData metaData) {
    this.metaData = metaData;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getEntry() {
    return SiteUtil.getSiteContentsBasePath(metaData.getOptions().get(SiteMetaData.SITE_NAME)) + "/" + FILENAME;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void export(OutputStream outputStream) throws IOException {
    XStream xStream = new XStream();
    xStream.alias("metadata", SiteMetaData.class);
    OutputStreamWriter writer = new OutputStreamWriter(outputStream, "UTF-8");
    xStream.toXML(metaData, writer);
    writer.flush();
  }

  /**
   * Gets the meta data.
   *
   * @return the meta data
   */
  public SiteMetaData getMetaData() {
    return metaData;
  }
}