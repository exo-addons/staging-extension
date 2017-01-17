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
package org.exoplatform.management.ecmadmin.operations.taxonomy;

import org.exoplatform.management.common.AbstractOperationHandler;
import org.exoplatform.services.jcr.RepositoryService;
import org.gatein.management.api.operation.model.ExportTask;

import java.io.IOException;
import java.io.OutputStream;

import javax.jcr.Session;

/**
 * The Class TaxonomyTreeExportTask.
 *
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class TaxonomyTreeExportTask implements ExportTask {

  /** The meta data. */
  private TaxonomyMetaData metaData = null;
  
  /** The path. */
  private String path = null;
  
  /** The repository service. */
  private RepositoryService repositoryService = null;

  /**
   * Instantiates a new taxonomy tree export task.
   *
   * @param repositoryService the repository service
   * @param metaData the meta data
   * @param path the path
   */
  public TaxonomyTreeExportTask(RepositoryService repositoryService, TaxonomyMetaData metaData, String path) {
    this.metaData = metaData;
    this.path = path;
    this.repositoryService = repositoryService;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getEntry() {
    return path + "/tree.xml";
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void export(OutputStream outputStream) throws IOException {
    try {
      Session session = AbstractOperationHandler.getSession(repositoryService, metaData.getTaxoTreeWorkspace());

      // Workaround: use docview instead of sysview
      session.exportDocumentView(metaData.getTaxoTreeHomePath(), outputStream, false, false);
    } catch (Exception exception) {
      exception.printStackTrace();
      throw new RuntimeException(exception);
    }
  }
}