package org.exoplatform.management.backup.service.idm;

import org.exoplatform.management.backup.operations.BackupExportResource;
import org.exoplatform.management.backup.service.BackupInProgressException;
import org.exoplatform.services.organization.Membership;
import org.exoplatform.services.organization.MembershipEventListener;

public class BackupMembershipListener extends MembershipEventListener {

  @Override
  public void preSave(Membership membership, boolean isNew) throws Exception {
    if (BackupExportResource.backupInProgress) {
      throw new BackupInProgressException("Backup is in progress, the Platform is in readonly mode. Your changes are ignored.");
    }
  }

  @Override
  public void preDelete(Membership membership) throws Exception {
    if (BackupExportResource.backupInProgress) {
      throw new BackupInProgressException("Backup is in progress, the Platform is in readonly mode. Your changes are ignored.");
    }
  }
}
