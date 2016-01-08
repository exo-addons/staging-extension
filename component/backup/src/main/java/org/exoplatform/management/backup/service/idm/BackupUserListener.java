package org.exoplatform.management.backup.service.idm;

import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserEventListener;

public class BackupUserListener extends UserEventListener {

  @Override
  public void preSave(User user, boolean isNew) throws Exception {
    IDMBackup.interceptIDMModificationOperation();
  }

  @Override
  public void preDelete(User user) throws Exception {
    IDMBackup.interceptIDMModificationOperation();
  }
}
