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

import org.gatein.management.api.operation.model.ExportTask;

import java.io.IOException;
import java.io.OutputStream;

/**
 * The Class ExportTaskWrapper.
 */
public class ExportTaskWrapper implements ExportTask {

  /** The base path. */
  private String basePath;
  
  /** The export task. */
  private ExportTask exportTask;

  /**
   * Instantiates a new export task wrapper.
   *
   * @param exportTask the export task
   * @param basePath the base path
   */
  public ExportTaskWrapper(ExportTask exportTask, String basePath) {
    this.exportTask = exportTask;
    this.basePath = basePath;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getEntry() {
    return basePath + exportTask.getEntry();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void export(OutputStream outputStream) throws IOException {
    exportTask.export(outputStream);
  }
}
