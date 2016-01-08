package org.exoplatform.management.backup.service.idm;

import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.GroupEventListener;

public class BackupGroupListener extends GroupEventListener {

  @Override
  public void preSave(Group group, boolean isNew) throws Exception {
    IDMBackup.interceptIDMModificationOperation();
  }

  @Override
  public void preDelete(Group group) throws Exception {
    IDMBackup.interceptIDMModificationOperation();
  }
}
