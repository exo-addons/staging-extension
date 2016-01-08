package org.exoplatform.management.backup.service.idm;

import org.exoplatform.management.backup.operations.BackupExportResource;
import org.exoplatform.management.backup.service.BackupInProgressException;
import org.exoplatform.services.organization.MembershipType;
import org.exoplatform.services.organization.MembershipTypeEventListener;

public class BackupMembershipTypeListener extends MembershipTypeEventListener {

  @Override
  public void preSave(MembershipType membershipType, boolean isNew) throws Exception {
    if (BackupExportResource.backupInProgress) {
      throw new BackupInProgressException("Backup is in progress, the Platform is in readonly mode. Your changes are ignored.");
    }
  }

  @Override
  public void preDelete(MembershipType membershipType) throws Exception {
    if (BackupExportResource.backupInProgress) {
      throw new BackupInProgressException("Backup is in progress, the Platform is in readonly mode. Your changes are ignored.");
    }
  }
}
