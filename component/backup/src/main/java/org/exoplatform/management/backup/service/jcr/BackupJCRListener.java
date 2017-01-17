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

import org.exoplatform.management.backup.operations.BackupExportResource;
import org.exoplatform.management.backup.service.BackupInProgressException;
import org.exoplatform.services.jcr.dataflow.ItemStateChangesLog;
import org.exoplatform.services.jcr.dataflow.persistent.ExtendedMandatoryItemsPersistenceListener;

/**
 * The listener interface for receiving backupJCR events.
 * The class that is interested in processing a backupJCR
 * event implements this interface, and the object created
 * with that class is registered with a component using the
 * component's <code>addBackupJCRListener</code> method. When
 * the backupJCR event occurs, that object's appropriate
 * method is invoked.
 *
 * @author <a href="mailto:boubaker.khanfir@exoplatform.com">Boubaker
 *         Khanfir</a>
 * @version $Revision$
 */
public class BackupJCRListener implements ExtendedMandatoryItemsPersistenceListener {

  /**
   * {@inheritDoc}
   */
  public void onSaveItems(ItemStateChangesLog itemStates) {
    if (BackupExportResource.backupInProgress && itemStates.getSize() > 0) {
      throw new BackupInProgressException("Backup is in progress, the Platform is in readonly mode. Your changes are ignored.");
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isTXAware() {
    return true;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onRollback() {}

  /**
   * {@inheritDoc}
   */
  @Override
  public void onCommit() {
    if (BackupExportResource.backupInProgress) {
      throw new BackupInProgressException("Backup is in progress, the Platform is in readonly mode. Your changes are ignored.");
    }
  }
}
