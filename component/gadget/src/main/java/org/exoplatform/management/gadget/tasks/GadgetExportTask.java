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
package org.exoplatform.management.gadget.tasks;

import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.model.ExportTask;

import java.io.IOException;
import java.io.OutputStream;

import javax.jcr.Session;

/**
 * The Class GadgetExportTask.
 *
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class GadgetExportTask implements ExportTask {
  
  /** The workspace name. */
  private String workspaceName;
  
  /** The gadget JCR path. */
  private String gadgetJCRPath;
  
  /** The gadget name. */
  private String gadgetName;
  
  /** The manageable repository. */
  private ManageableRepository manageableRepository;

  /**
   * Instantiates a new gadget export task.
   *
   * @param gadgetName the gadget name
   * @param manageableRepository the manageable repository
   * @param workspaceName the workspace name
   * @param jcrPath the jcr path
   */
  public GadgetExportTask(String gadgetName, ManageableRepository manageableRepository, String workspaceName, String jcrPath) {
    this.workspaceName = workspaceName;
    this.gadgetName = gadgetName;
    this.gadgetJCRPath = jcrPath + "app:" + gadgetName;
    this.manageableRepository = manageableRepository;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getEntry() {
    return "gadget/" + gadgetName + ".xml";
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void export(OutputStream outputStream) throws IOException {
    SessionProvider sessionProvider = SessionProvider.createSystemProvider();
    try {
      Session session = sessionProvider.getSession(workspaceName, manageableRepository);
      session.exportDocumentView(gadgetJCRPath, outputStream, false, false);
      outputStream.flush();
    } catch (Exception exception) {
      throw new OperationException(OperationNames.EXPORT_RESOURCE, "Error while exporting gadget data", exception);
    }
  }

}
