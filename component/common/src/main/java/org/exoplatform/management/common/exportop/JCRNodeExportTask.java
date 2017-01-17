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

import org.exoplatform.management.common.importop.AbstractJCRImportOperationHandler;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.model.ExportTask;

import java.io.IOException;
import java.io.OutputStream;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * The Class JCRNodeExportTask.
 *
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class JCRNodeExportTask implements ExportTask {
  
  /** The Constant JCR_DATA_SEPARATOR. */
  public static final String JCR_DATA_SEPARATOR = "JCR_EXP_DATA";

  /** The Constant log. */
  private static final Log log = ExoLogger.getLogger(JCRNodeExportTask.class);

  /** The export binary. */
  private boolean exportBinary = true;

  /** The repository service. */
  private final RepositoryService repositoryService;
  
  /** The workspace. */
  private final String workspace;
  
  /** The absolute path. */
  private final String absolutePath;
  
  /** The entry path. */
  private final String entryPath;
  
  /** The recurse. */
  private final boolean recurse;

  /**
   * Instantiates a new JCR node export task.
   *
   * @param repositoryService the repository service
   * @param workspace the workspace
   * @param absolutePath the absolute path
   * @param entryPath the entry path
   * @param recurse the recurse
   * @param isPrefix the is prefix
   */
  public JCRNodeExportTask(RepositoryService repositoryService, String workspace, String absolutePath, String entryPath, boolean recurse, boolean isPrefix) {
    this.repositoryService = repositoryService;
    this.workspace = workspace;
    if (isPrefix) {
      this.entryPath = entryPath + (entryPath.endsWith("/") ? "" : "/") + JCR_DATA_SEPARATOR + absolutePath + ".xml";
    } else {
      this.entryPath = entryPath;
    }
    this.absolutePath = absolutePath;
    this.recurse = recurse;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getEntry() {
    return entryPath;
  }

  /**
   * Sets the export binary.
   *
   * @param exportBinary the new export binary
   */
  public void setExportBinary(boolean exportBinary) {
    this.exportBinary = exportBinary;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void export(OutputStream outputStream) throws IOException {
    Session session = null;
    try {
      log.info("Export: " + workspace + ":" + absolutePath);

      session = AbstractJCRImportOperationHandler.getSession(repositoryService, workspace);
      session.exportDocumentView(absolutePath, outputStream, !exportBinary, !recurse);
    } catch (RepositoryException exception) {
      throw new OperationException(OperationNames.EXPORT_RESOURCE, "Unable to export content from : " + absolutePath, exception);
    } finally {
      if (session != null) {
        session.logout();
      }
    }
  }
}
