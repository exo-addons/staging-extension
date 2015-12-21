package org.exoplatform.management.backup.service;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import javax.jcr.RepositoryException;

import org.apache.commons.collections.map.HashedMap;
import org.apache.commons.io.FileUtils;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.management.backup.operations.BackupImportResource;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.backup.BackupChainLog;
import org.exoplatform.services.jcr.ext.backup.RepositoryBackupChainLog;
import org.exoplatform.services.jcr.impl.backup.BackupException;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

public class JCRRestore {
  private static final Log log = ExoLogger.getLogger(BackupImportResource.class);

  public static void restore(PortalContainer portalContainer, File backupDirFile) throws Exception {
    RepositoryService repositoryService = (RepositoryService) portalContainer.getComponentInstanceOfType(RepositoryService.class);

    File logFile = getLogFile(backupDirFile);
    if (logFile == null) {
      log.info("JCR backup files was not found. JCR restore ignored");
      return;
    }

    String repositoryName = null;
    try {

      RepositoryBackupChainLog backupChainLog = new RepositoryBackupChainLog(logFile);
      repositoryName = backupChainLog.getOriginalRepositoryEntry().getName();

      // Suspend repository
      ManageableRepository repository = suspendRepository(repositoryService, repositoryName);

      // Remove repository if existing
      if (repository != null) {
        log.info("Remove repository '{}'", repositoryName);
        removeRepository(repositoryService, repositoryName);
      }

      // Restore repository from backup directory
      log.info("Restore repository '{}'", repositoryName);
      restoreRepository(repositoryService, backupChainLog);
      log.info("Repository '{}' restore completed.", repositoryName);

    } finally {
      resumeRepository(repositoryService, repositoryName);
    }
  }

  private static File getLogFile(File backupDirFile) throws IOException {
    File[] files = backupDirFile.listFiles();
    for (File file : files) {
      if (!file.isDirectory() && file.getName().endsWith(".xml") && file.getName().startsWith("repository-backup-") && FileUtils.readFileToString(file).contains("<repository-backup-chain-log")) {
        return file;
      }
    }
    return null;
  }

  private static void removeRepository(RepositoryService repositoryService, String repositoryName) throws RepositoryException {
    repositoryService.removeRepository(repositoryName, true);
  }

  private static ManageableRepository suspendRepository(RepositoryService repositoryService, String repositoryName) throws RepositoryConfigurationException {
    ManageableRepository repository = null;
    try {
      repository = repositoryService.getRepository(repositoryName);
      repository.setState(ManageableRepository.OFFLINE);
    } catch (RepositoryException e) {
      // Nothing to do, the repository wasn't found
    }
    return repository;
  }

  private static void resumeRepository(RepositoryService repositoryService, String repositoryName) {
    if (repositoryName != null) {
      try {
        ManageableRepository repository = null;
        try {
          repository = repositoryService.getRepository(repositoryName);
        } catch (Exception e) {
          // Noting to do, Repository was not found
        }
        if (repository != null && repository.getState() != ManageableRepository.ONLINE) {
          repository.setState(ManageableRepository.ONLINE);
        }
      } catch (Exception e) {
        log.error("Error while resuming repository", e);
      }
    }
  }

  @SuppressWarnings("unchecked")
  private static void restoreRepository(RepositoryService repositoryService, RepositoryBackupChainLog rblog) throws Exception {
    RepositoryEntry repositoryEntry = rblog.getOriginalRepositoryEntry();

    // Checking repository exists.
    try {
      ManageableRepository repository = repositoryService.getRepository(repositoryEntry.getName());
      if(repository != null) {
        throw new BackupException("Repository \"" + repositoryEntry.getName() + "\" already exists.");
      }
    } catch (RepositoryException e) {
      // OK. Repository with "repositoryEntry.getName" is not exists.
    }

    Map<String, File> workspacesMapping = new HashedMap();
    Map<String, BackupChainLog> backups = new HashedMap();

    for (String path : rblog.getWorkspaceBackupsInfo()) {
      BackupChainLog bLog = new BackupChainLog(new File(path));
      backups.put(bLog.getBackupConfig().getWorkspace(), bLog);
    }

    for (WorkspaceEntry wsEntry : repositoryEntry.getWorkspaceEntries()) {
      workspacesMapping.put(wsEntry.getName(), new File(backups.get(wsEntry.getName()).getLogFilePath()));
    }

    JobRepositoryRestore jobRepositoryRestore = new JobRepositoryRestore(repositoryService, repositoryEntry, workspacesMapping, new File(rblog.getLogFilePath()));
    jobRepositoryRestore.restore();
  }

}
