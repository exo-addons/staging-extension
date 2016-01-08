package org.exoplatform.management.backup.service.idm;

import org.exoplatform.services.organization.MembershipType;
import org.exoplatform.services.organization.MembershipTypeEventListener;

public class BackupMembershipTypeListener extends MembershipTypeEventListener {

  @Override
  public void preSave(MembershipType membershipType, boolean isNew) throws Exception {
    IDMBackup.interceptIDMModificationOperation();
  }

  @Override
  public void preDelete(MembershipType membershipType) throws Exception {
    IDMBackup.interceptIDMModificationOperation();
  }
}
