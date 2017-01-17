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
package org.exoplatform.management.ecmadmin.operations.templates.nodetypes;

import com.thoughtworks.xstream.XStream;

import org.exoplatform.management.ecmadmin.operations.templates.NodeTemplate;
import org.gatein.management.api.operation.model.ExportTask;

import java.io.IOException;
import java.io.OutputStream;

/**
 * The Class NodeTypeTemplatesMetaDataExportTask.
 *
 * @author <a href="mailto:thomas.delhomenie@exoplatform.com">Thomas
 *         Delhoménie</a>
 * @version $Revision$
 */
public class NodeTypeTemplatesMetaDataExportTask implements ExportTask {

  /** The meta data. */
  private NodeTypeTemplatesMetaData metaData = null;
  
  /** The path. */
  private String path = null;

  /**
   * Instantiates a new node type templates meta data export task.
   *
   * @param metaData the meta data
   * @param path the path
   */
  public NodeTypeTemplatesMetaDataExportTask(NodeTypeTemplatesMetaData metaData, String path) {
    this.metaData = metaData;
    this.path = path;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getEntry() {
    return path + "/metadata.xml";
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void export(OutputStream outputStream) throws IOException {
    XStream xStream = new XStream();
    xStream.alias("metadata", NodeTypeTemplatesMetaData.class);
    xStream.alias("template", NodeTemplate.class);
    String xmlContent = xStream.toXML(metaData);
    outputStream.write(xmlContent.getBytes());
  }
}