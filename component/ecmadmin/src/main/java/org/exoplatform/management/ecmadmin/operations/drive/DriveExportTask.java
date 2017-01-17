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
package org.exoplatform.management.ecmadmin.operations.drive;

import org.exoplatform.container.xml.Configuration;
import org.gatein.management.api.operation.model.ExportTask;

import java.io.IOException;
import java.io.OutputStream;

/**
 * The Class DriveExportTask.
 *
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class DriveExportTask implements ExportTask {
  
  /** The Constant CONFIGURATION_FILE_XSD. */
  private static final String CONFIGURATION_FILE_XSD = "<configuration " + "\r\n   xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
      + "\r\n   xsi:schemaLocation=\"http://www.exoplatform.org/xml/ns/kernel_1_2.xsd http://www.exoplatform.org/xml/ns/kernel_1_2.xsd\""
      + "\r\n   xmlns=\"http://www.exoplatform.org/xml/ns/kernel_1_2.xsd\">";

  /** The basepath. */
  private String basepath = null;
  
  /** The configuration. */
  private Configuration configuration = null;

  /**
   * Instantiates a new drive export task.
   *
   * @param configuration the configuration
   * @param basepath the basepath
   */
  public DriveExportTask(Configuration configuration, String basepath) {
    this.basepath = basepath;
    this.configuration = configuration;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getEntry() {
    return basepath + "/drives-configuration.xml";
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void export(OutputStream outputStream) throws IOException {
    try {
      String content = configuration.toXML();
      content = content.replace("<configuration>", CONFIGURATION_FILE_XSD);
      content = content.replaceAll("<field name=\"([A-z])*\"/>", "");
      outputStream.write(content.getBytes());
    } catch (Exception exception) {
      throw new RuntimeException(exception);
    }
  }

}