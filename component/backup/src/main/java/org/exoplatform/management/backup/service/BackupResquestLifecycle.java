package org.exoplatform.management.backup.service;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.component.ComponentRequestLifecycle;
import org.exoplatform.management.backup.operations.BackupImportResource;

public class BackupResquestLifecycle implements ComponentRequestLifecycle {

  @Override
  public void startRequest(ExoContainer container) {
    while (BackupImportResource.restoreInProgress) {
      try {
        Thread.sleep(3000);
      } catch (InterruptedException e) {}
    }
  }

  @Override
  public void endRequest(ExoContainer container) {}

}
