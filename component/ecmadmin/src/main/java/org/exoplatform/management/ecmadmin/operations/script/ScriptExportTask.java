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
package org.exoplatform.management.ecmadmin.operations.script;

import org.gatein.management.api.operation.model.ExportTask;

import java.io.IOException;
import java.io.OutputStream;

/**
 * The Class ScriptExportTask.
 *
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class ScriptExportTask implements ExportTask {

  /** The script path. */
  private String scriptPath;
  
  /** The script data. */
  private String scriptData;

  /**
   * Instantiates a new script export task.
   *
   * @param scriptPath the script path
   * @param scriptData the script data
   */
  public ScriptExportTask(String scriptPath, String scriptData) {
    this.scriptPath = scriptPath;
    this.scriptData = scriptData;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getEntry() {
    return "ecmadmin/script/" + scriptPath;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void export(OutputStream outputStream) throws IOException {
    try {
      outputStream.write(scriptData.getBytes("UTF-8"));
    } catch (Exception exception) {
      throw new RuntimeException(exception);
    }
  }

}