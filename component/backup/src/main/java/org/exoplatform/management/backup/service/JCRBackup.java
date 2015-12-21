package org.exoplatform.management.backup.service;

import java.io.File;
import java.util.Iterator;
import java.util.List;

import javax.jcr.RepositoryException;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.PropertiesParam;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.config.ValueStorageEntry;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.backup.BackupConfig;
import org.exoplatform.services.jcr.ext.backup.BackupConfigurationException;
import org.exoplatform.services.jcr.ext.backup.BackupManager;
import org.exoplatform.services.jcr.ext.backup.BackupOperationException;
import org.exoplatform.services.jcr.ext.backup.RepositoryBackupChain;
import org.exoplatform.services.jcr.ext.backup.RepositoryBackupConfig;
import org.exoplatform.services.jcr.ext.backup.impl.BackupManagerImpl;
import org.exoplatform.services.jcr.ext.registry.RegistryService;

/**
 * @author <a href="mailto:boubaker.khanfir@exoplatform.com">Boubaker
 *         Khanfir</a>
 * @version $Revision$
 */
public class JCRBackup {

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

    // FIXME: Workaround JCR-2405
    fixWorkspacesConfiguration(repository);

    // Prepare BackupConfig
    BackupConfig backupConfig = new BackupConfig();
    backupConfig.setBackupType(BackupManager.FULL_BACKUP_ONLY);
    backupConfig.setBackupDir(backupDirFile);
    backupConfig.setRepository(repository.getConfiguration().getName());

    try {
      repository.setState(ManageableRepository.SUSPENDED);

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
      repository.setState(ManageableRepository.ONLINE);
    }
  }

  // FIXME JCR-2405
  public static void fixWorkspacesConfiguration(final ManageableRepository repository) throws RepositoryConfigurationException {
    List<WorkspaceEntry> workspaceEntries = repository.getConfiguration().getWorkspaceEntries();
    for (WorkspaceEntry workspaceEntry : workspaceEntries) {
      List<ValueStorageEntry> storageEntries = workspaceEntry.getContainer().getValueStorages();
      if (storageEntries != null) {
        Iterator<ValueStorageEntry> storageIterator = storageEntries.iterator();
        while (storageIterator.hasNext()) {
          ValueStorageEntry valueStorageEntry = (ValueStorageEntry) storageIterator.next();
          String enabledStorage = valueStorageEntry.getParameterValue("enabled");
          if (enabledStorage != null && "false".equalsIgnoreCase(enabledStorage)) {
            storageIterator.remove();
          }
        }
      }
    }
  }

}