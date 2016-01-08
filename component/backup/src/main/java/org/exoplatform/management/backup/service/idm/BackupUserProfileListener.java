package org.exoplatform.management.backup.service.idm;

import org.exoplatform.management.backup.operations.BackupExportResource;
import org.exoplatform.management.backup.service.BackupInProgressException;
import org.exoplatform.services.organization.UserProfile;
import org.exoplatform.services.organization.UserProfileEventListener;

public class BackupUserProfileListener extends UserProfileEventListener {

  @Override
  public void preSave(UserProfile user, boolean isNew) throws Exception {
    if (BackupExportResource.backupInProgress) {
      throw new BackupInProgressException("Backup is in progress, the Platform is in readonly mode. Your changes are ignored.");
    }
  }

  @Override
  public void preDelete(UserProfile user) throws Exception {
    if (BackupExportResource.backupInProgress) {
      throw new BackupInProgressException("Backup is in progress, the Platform is in readonly mode. Your changes are ignored.");
    }
  }
}
