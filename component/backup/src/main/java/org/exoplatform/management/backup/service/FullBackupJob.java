package org.exoplatform.management.backup.service;

import java.io.File;
import java.util.List;

import org.exoplatform.services.jcr.impl.backup.Backupable;
import org.exoplatform.services.jcr.impl.backup.ResumeException;
import org.exoplatform.services.jcr.impl.core.query.SearchManager;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * @author <a href="mailto:boubaker.khanfir@exoplatform.com">Boubaker
 *         Khanfir</a>
 * @version $Revision$
 */
public class FullBackupJob extends org.exoplatform.services.jcr.ext.backup.impl.rdbms.FullBackupJob {
  protected static final Log LOG = ExoLogger.getLogger(FullBackupJob.class);

  /**
   * {@inheritDoc}
   */
  public void run() {
    notifyListeners();
    try {
      // Reopen indexes to make platform in read-ony mode
      SearchManager searchManager = (SearchManager) repository.getWorkspaceContainer(workspaceName).getComponent(SearchManager.class);
      if (searchManager.isSuspended()) {
        try {
          LOG.info("Resume SearchManager for workspace: " + workspaceName);
          searchManager.resume();
        } catch (ResumeException e) {
          LOG.error(e);
        }
      }

      List<Backupable> backupableComponents = repository.getWorkspaceContainer(workspaceName).getComponentInstancesOfType(Backupable.class);
      // backup all components
      for (Backupable component : backupableComponents) {
        component.backup(new File(getStorageURL().getFile()));
      }
    } catch (Throwable e) {
      LOG.error("Full backup failed " + getStorageURL().getPath(), e);
      notifyError("Full backup failed", e);
    } finally {
      state = FINISHED;
    }
    notifyListeners();
  }
}