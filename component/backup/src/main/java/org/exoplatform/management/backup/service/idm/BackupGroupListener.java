package org.exoplatform.management.backup.service.idm;

import org.exoplatform.management.backup.operations.BackupExportResource;
import org.exoplatform.management.backup.service.BackupInProgressException;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.GroupEventListener;

public class BackupGroupListener extends GroupEventListener {

  @Override
  public void preSave(Group group, boolean isNew) throws Exception {
    if (BackupExportResource.backupInProgress) {
      throw new BackupInProgressException("Backup is in progress, the Platform is in readonly mode. Your changes are ignored.");
    }
  }

  @Override
  public void preDelete(Group group) throws Exception {
    if (BackupExportResource.backupInProgress) {
      throw new BackupInProgressException("Backup is in progress, the Platform is in readonly mode. Your changes are ignored.");
    }
  }
}
