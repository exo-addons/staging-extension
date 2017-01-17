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

import org.exoplatform.container.xml.InitParams;
import org.gatein.management.api.operation.model.ExportTask;

import java.io.IOException;
import java.io.OutputStream;

/**
 * The Class ViewConfigurationExportTask.
 *
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class ViewConfigurationExportTask implements ExportTask {
  
  /** The init params. */
  private final InitParams initParams;
  
  /** The name. */
  private final String name;

  /**
   * Instantiates a new view configuration export task.
   *
   * @param initParams the init params
   * @param name the name
   */
  public ViewConfigurationExportTask(InitParams initParams, String name) {
    this.initParams = initParams;
    this.name = name;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getEntry() {
    return "ecmadmin/view/configuration/" + name;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void export(OutputStream outputStream) throws IOException {
    try {
      String content = new String(Utils.toXML(initParams));
      content = content.replaceAll("<field name=\"([A-z])*\"/>", "");
      outputStream.write(content.getBytes());
    } catch (Exception exception) {
      throw new RuntimeException(exception);
    }
  }
}