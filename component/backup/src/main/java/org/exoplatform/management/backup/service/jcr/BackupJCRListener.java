package org.exoplatform.management.backup.service.jcr;

import org.exoplatform.management.backup.operations.BackupExportResource;
import org.exoplatform.management.backup.service.BackupInProgressException;
import org.exoplatform.services.jcr.dataflow.ItemStateChangesLog;
import org.exoplatform.services.jcr.dataflow.persistent.ExtendedMandatoryItemsPersistenceListener;

/**
 * @author <a href="mailto:boubaker.khanfir@exoplatform.com">Boubaker
 *         Khanfir</a>
 * @version $Revision$
 */
public class BackupJCRListener implements ExtendedMandatoryItemsPersistenceListener {

  public void onSaveItems(ItemStateChangesLog itemStates) {
    if (BackupExportResource.backupInProgress && itemStates.getSize() > 0) {
      throw new BackupInProgressException("Backup is in progress, the Platform is in readonly mode. Your changes are ignored.");
    }
  }

  @Override
  public boolean isTXAware() {
    return true;
  }

  @Override
  public void onRollback() {}

  @Override
  public void onCommit() {
    if (BackupExportResource.backupInProgress) {
      throw new BackupInProgressException("Backup is in progress, the Platform is in readonly mode. Your changes are ignored.");
    }
  }
}
