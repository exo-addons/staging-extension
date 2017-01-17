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
package org.exoplatform.management.ecmadmin.operations.view;

import org.apache.commons.io.IOUtils;
import org.exoplatform.ecm.webui.utils.Utils;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.model.ExportTask;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

/**
 * The Class ViewTemplateExportTask.
 *
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class ViewTemplateExportTask implements ExportTask {
  
  /** The template node. */
  private final Node templateNode;

  /**
   * Instantiates a new view template export task.
   *
   * @param templateNode the template node
   */
  public ViewTemplateExportTask(Node templateNode) {
    this.templateNode = templateNode;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getEntry() {
    try {
      return "ecmadmin/view/templates/" + this.templateNode.getName() + ".gtmpl";
    } catch (RepositoryException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void export(OutputStream outputStream) throws IOException {
    InputStream templateStream = null;
    try {
      Node contentNode = templateNode.getNode(Utils.JCR_CONTENT);
      templateStream = contentNode.getProperty("jcr:data").getStream();
      IOUtils.copy(templateStream, outputStream);
    } catch (Exception e) {
      throw new OperationException(OperationNames.EXPORT_RESOURCE, "Unable to export view template .", e);
    } finally {
      if (templateStream != null) {
        templateStream.close();
      }
    }
  }

}