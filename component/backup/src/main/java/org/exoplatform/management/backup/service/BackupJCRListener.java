package org.exoplatform.management.backup.service;

import org.exoplatform.management.backup.operations.BackupExportResource;
import org.exoplatform.services.jcr.dataflow.ItemStateChangesLog;
import org.exoplatform.services.jcr.dataflow.persistent.ExtendedMandatoryItemsPersistenceListener;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * @author <a href="mailto:boubaker.khanfir@exoplatform.com">Boubaker
 *         Khanfir</a>
 * @version $Revision$
 */
public class BackupJCRListener implements ExtendedMandatoryItemsPersistenceListener {

  protected final Log LOG = ExoLogger.getLogger(BackupJCRListener.class);

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
