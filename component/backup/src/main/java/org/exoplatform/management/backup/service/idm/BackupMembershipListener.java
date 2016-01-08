package org.exoplatform.management.backup.service.idm;

import org.exoplatform.services.organization.Membership;
import org.exoplatform.services.organization.MembershipEventListener;

public class BackupMembershipListener extends MembershipEventListener {

  @Override
  public void preSave(Membership membership, boolean isNew) throws Exception {
    IDMBackup.interceptIDMModificationOperation();
  }

  @Override
  public void preDelete(Membership membership) throws Exception {
    IDMBackup.interceptIDMModificationOperation();
  }
}
