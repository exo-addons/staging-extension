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
package org.exoplatform.management.ecmadmin.operations.queries;

import org.exoplatform.container.xml.Configuration;
import org.gatein.management.api.operation.model.ExportTask;

import java.io.IOException;
import java.io.OutputStream;

/**
 * The Class QueriesExportTask.
 *
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class QueriesExportTask implements ExportTask {
  
  /** The Constant CONFIGURATION_FILE_XSD. */
  public static final String CONFIGURATION_FILE_XSD = "<configuration " + "\r\n   xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
      + "\r\n   xsi:schemaLocation=\"http://www.exoplatform.org/xml/ns/kernel_1_2.xsd http://www.exoplatform.org/xml/ns/kernel_1_2.xsd\""
      + "\r\n   xmlns=\"http://www.exoplatform.org/xml/ns/kernel_1_2.xsd\">";
  
  /** The configuration. */
  private final Configuration configuration;
  
  /** The user id. */
  private final String userId;

  /**
   * Instantiates a new queries export task.
   *
   * @param configuration the configuration
   * @param userId the user id
   */
  public QueriesExportTask(Configuration configuration, String userId) {
    this.configuration = configuration;
    this.userId = userId;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getEntry() {
    if (userId != null) {
      return "ecmadmin/queries/users/" + this.userId + "-queries-configuration.xml";
    } else {
      return "ecmadmin/queries/shared-queries-configuration.xml";
    }
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