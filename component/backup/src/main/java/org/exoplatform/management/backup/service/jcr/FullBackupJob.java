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
package org.exoplatform.management.backup.service.jcr;

import org.exoplatform.services.jcr.impl.backup.Backupable;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.File;
import java.util.List;

/**
 * The Class FullBackupJob.
 *
 * @author <a href="mailto:boubaker.khanfir@exoplatform.com">Boubaker
 *         Khanfir</a>
 * @version $Revision$
 */
public class FullBackupJob extends org.exoplatform.services.jcr.ext.backup.impl.rdbms.FullBackupJob {
  
  /** The Constant LOG. */
  protected static final Log LOG = ExoLogger.getLogger(FullBackupJob.class);

  /**
   * {@inheritDoc}
   */
  public void run() {
    notifyListeners();
    try {
      List<Backupable> backupableComponents = repository.getWorkspaceContainer(workspaceName).getComponentInstancesOfType(Backupable.class);
      // backup all components
      for (Backupable component : backupableComponents) {
        component.backup(new File(getStorageURL().getFile()));
      }
    } catch (Throwable e) {
      LOG.error("Full backup failed " + getStorageURL().getPath(), e);
      notifyError("Full backup failed", e);
    } finally {
      state = FINISHED;
    }
    notifyListeners();
  }
}