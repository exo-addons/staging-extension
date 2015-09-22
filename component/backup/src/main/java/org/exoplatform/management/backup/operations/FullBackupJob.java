package org.exoplatform.management.backup.operations;

import java.io.File;
import java.util.List;

import javax.jcr.RepositoryException;

import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.impl.backup.BackupException;
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
public class FullBackupJob extends org.exoplatform.services.jcr.ext.backup.impl.rdbms.FullBackupJob
{
  protected static final Log LOG = ExoLogger.getLogger(FullBackupJob.class);

  /**
   * {@inheritDoc}
   */
  public void run()
  {
    notifyListeners();
    try {
      // Wait finishing all current storage operations
      repository.getWorkspaceContainer(workspaceName).setState(ManageableRepository.SUSPENDED);

      // Reopen indexes to make platform in read-ony mode
      SearchManager searchManager = (SearchManager) repository.getWorkspaceContainer(workspaceName).getComponent(SearchManager.class);
      if (searchManager.isSuspended()) {
        try {
          LOG.info("Resume component: " + searchManager.getClass() + " for workspace: " + workspaceName);
          searchManager.resume();
        } catch (ResumeException e) {
          LOG.error(e);
        }
      }

      @SuppressWarnings("unchecked")
      List<Backupable> backupableComponents = repository.getWorkspaceContainer(workspaceName).getComponentInstancesOfType(Backupable.class);
      // backup all components
      for (Backupable component : backupableComponents) {
        component.backup(new File(getStorageURL().getFile()));
      }
    } catch (BackupException e) {
      LOG.error("Full backup failed " + getStorageURL().getPath(), e);
      notifyError("Full backup failed", e);
    } catch (RepositoryException e) {
      LOG.error("Full backup failed " + getStorageURL().getPath(), e);
      notifyError("Full backup failed", e);
    } finally {
      try {
        repository.getWorkspaceContainer(workspaceName).setState(ManageableRepository.ONLINE);
      } catch (RepositoryException e) {
        LOG.error("Full backup failed " + getStorageURL().getPath(), e);
        notifyError("Full backup failed", e);
      }
    }
    state = FINISHED;
    notifyListeners();
  }
}