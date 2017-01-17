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
package org.exoplatform.management.ecmadmin.exporttask;

import org.apache.commons.io.IOUtils;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.model.ExportTask;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.jcr.Node;

/**
 * The Class NodeFileExportTask.
 *
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class NodeFileExportTask implements ExportTask {
  
  /** The node. */
  private final Node node;
  
  /** The export path. */
  private final String exportPath;

  /**
   * Instantiates a new node file export task.
   *
   * @param node the node
   * @param exportPath the export path
   */
  public NodeFileExportTask(Node node, String exportPath) {
    this.node = node;
    this.exportPath = exportPath;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getEntry() {
    return exportPath;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void export(OutputStream outputStream) throws IOException {
    InputStream nodeFileIS = null;
    try {
      nodeFileIS = node.getNode("jcr:content").getProperty("jcr:data").getStream();
      IOUtils.copy(nodeFileIS, outputStream);
    } catch (Exception e) {
      throw new OperationException(OperationNames.EXPORT_RESOURCE, "Unable to export file of node " + exportPath, e);
    } finally {
      if (nodeFileIS != null) {
        nodeFileIS.close();
      }
    }
  }

}
