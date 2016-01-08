package org.exoplatform.management.backup.service.idm;

import org.exoplatform.management.backup.operations.BackupExportResource;
import org.exoplatform.management.backup.service.BackupInProgressException;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserEventListener;

public class BackupUserListener extends UserEventListener {

  @Override
  public void preSave(User user, boolean isNew) throws Exception {
    if (BackupExportResource.backupInProgress) {
      throw new BackupInProgressException("Backup is in progress, the Platform is in readonly mode. Your changes are ignored.");
    }
  }

  @Override
  public void preDelete(User user) throws Exception {
    if (BackupExportResource.backupInProgress) {
      throw new BackupInProgressException("Backup is in progress, the Platform is in readonly mode. Your changes are ignored.");
    }
  }
}
