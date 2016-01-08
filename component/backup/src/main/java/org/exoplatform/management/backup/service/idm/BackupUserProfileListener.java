package org.exoplatform.management.backup.service.idm;

import org.exoplatform.services.organization.UserProfile;
import org.exoplatform.services.organization.UserProfileEventListener;

public class BackupUserProfileListener extends UserProfileEventListener {

  @Override
  public void preSave(UserProfile user, boolean isNew) throws Exception {
    IDMBackup.interceptIDMModificationOperation();
  }

  @Override
  public void preDelete(UserProfile user) throws Exception {
    IDMBackup.interceptIDMModificationOperation();
  }
}
