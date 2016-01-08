package org.exoplatform.management.backup.service.jcr;

import java.io.File;
import java.util.List;

import javax.jcr.RepositoryException;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.PropertiesParam;
import org.exoplatform.management.backup.operations.BackupExportResource;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.core.WorkspaceContainerFacade;
import org.exoplatform.services.jcr.ext.backup.BackupConfig;
import org.exoplatform.services.jcr.ext.backup.BackupConfigurationException;
import org.exoplatform.services.jcr.ext.backup.BackupManager;
import org.exoplatform.services.jcr.ext.backup.BackupOperationException;
import org.exoplatform.services.jcr.ext.backup.RepositoryBackupChain;
import org.exoplatform.services.jcr.ext.backup.RepositoryBackupConfig;
import org.exoplatform.services.jcr.ext.backup.impl.BackupManagerImpl;
import org.exoplatform.services.jcr.ext.registry.RegistryService;
import org.exoplatform.services.jcr.impl.backup.ResumeException;
import org.exoplatform.services.jcr.impl.backup.Suspendable;
import org.exoplatform.services.jcr.impl.core.query.SearchManager;
import org.exoplatform.services.jcr.impl.dataflow.persistent.WorkspacePersistentDataManager;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * @author <a href="mailto:boubaker.khanfir@exoplatform.com">Boubaker
 *         Khanfir</a>
 * @version $Revision$
 */
public class JCRBackup {

  private static final Log LOG = ExoLogger.getLogger(JCRBackup.class);

  private static final BackupJCRListener BACKUP_JCR_LISTENER = new BackupJCRListener();

  public static void backup(PortalContainer portalContainer, File backupDirFile) throws RepositoryException, RepositoryConfigurationException, BackupOperationException, BackupConfigurationException {
    RepositoryService repositoryService = (RepositoryService) portalContainer.getComponentInstanceOfType(RepositoryService.class);
    RegistryService registryService = (RegistryService) portalContainer.getComponentInstanceOfType(RegistryService.class);

    final ManageableRepository repository = repositoryService.getDefaultRepository();
    if (repository == null) {
      throw new IllegalArgumentException("No repository was found for PortalContainer '" + portalContainer.getName() + "'");
    }

    // Initialize backup service
    InitParams initParams = new InitParams();
    PropertiesParam param = new PropertiesParam();
    param.setName(BackupManagerImpl.BACKUP_PROPERTIES);
    param.setProperty(BackupManagerImpl.BACKUP_DIR, backupDirFile.getAbsolutePath());
    param.setProperty(BackupManagerImpl.FULL_BACKUP_TYPE, FullBackupJob.class.getName());
    initParams.addParameter(param);
    BackupManager backupManager = new BackupManagerImpl(null, initParams, repositoryService, registryService);
    ((BackupManagerImpl) backupManager).start();

    // Prepare BackupConfig
    BackupConfig backupConfig = new BackupConfig();
    backupConfig.setBackupType(BackupManager.FULL_BACKUP_ONLY);
    backupConfig.setBackupDir(backupDirFile);
    backupConfig.setRepository(repository.getConfiguration().getName());

    String[] wsNames = repository.getWorkspaceNames();
    try {
      if (BackupExportResource.WRITE_STRATEGY_NOTHING.equals(BackupExportResource.writeStrategy)) {
        // Nothing to do
      } else if (BackupExportResource.WRITE_STRATEGY_SUSPEND.equals(BackupExportResource.writeStrategy)) {
        LOG.info("Suspend repository: " + repository.getConfiguration().getName());
        repository.setState(ManageableRepository.SUSPENDED);
        // Reopen indexes to make platform in read-ony mode
        for (String workspaceName : wsNames) {
          WorkspaceContainerFacade workspaceContainer = repository.getWorkspaceContainer(workspaceName);
          List<Suspendable> suspendables = workspaceContainer.getComponentInstancesOfType(Suspendable.class);
          for (Suspendable suspendable : suspendables) {
            if (suspendable instanceof SearchManager) {
              resumeSearchManager(suspendable, workspaceName);
            }
          }
        }
      } else if (BackupExportResource.WRITE_STRATEGY_EXCEPTION.equals(BackupExportResource.writeStrategy)) {
        for (String workspaceName : wsNames) {
          WorkspaceContainerFacade workspaceContainer = repository.getWorkspaceContainer(workspaceName);
          WorkspacePersistentDataManager dataManager = (WorkspacePersistentDataManager) workspaceContainer.getComponent(WorkspacePersistentDataManager.class);
          dataManager.addItemPersistenceListener(BACKUP_JCR_LISTENER);
        }
      }

      // Full JCR repository backup
      RepositoryBackupChain currentBackupChain = backupManager.startBackup((RepositoryBackupConfig) backupConfig);

      while (!currentBackupChain.isFinished()) {
        try {
          // Block Thread until backup is finished
          Thread.sleep(1000);
        } catch (InterruptedException e) {
          throw new IllegalStateException("Error while resuming the SearchIndex services on workspaces", e);
        }
      }
    } finally {
      if (BackupExportResource.WRITE_STRATEGY_NOTHING.equals(BackupExportResource.writeStrategy)) {
        // Nothing to do
      } else if (BackupExportResource.WRITE_STRATEGY_SUSPEND.equals(BackupExportResource.writeStrategy)) {
        repository.setState(ManageableRepository.ONLINE);
      } else if (BackupExportResource.WRITE_STRATEGY_EXCEPTION.equals(BackupExportResource.writeStrategy)) {
        // remove backup listener
        for (String workspaceName : wsNames) {
          WorkspaceContainerFacade workspaceContainer = repository.getWorkspaceContainer(workspaceName);
          if (workspaceContainer != null) {
            WorkspacePersistentDataManager dataManager = (WorkspacePersistentDataManager) workspaceContainer.getComponent(WorkspacePersistentDataManager.class);
            dataManager.removeItemPersistenceListener(BACKUP_JCR_LISTENER);
          }
        }
      }
    }
  }

  private static void resumeSearchManager(Suspendable searchManager, String workspaceName) {
    if (searchManager.isSuspended()) {
      try {
        LOG.info("Resume SearchManager for workspace: " + workspaceName);
        searchManager.resume();
      } catch (ResumeException e) {
        LOG.error(e);
      }
    }
  }

}
