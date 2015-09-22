package org.exoplatform.management.backup.operations;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.jcr.Node;

import org.exoplatform.commons.utils.PrivilegedSystemHelper;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.PropertiesParam;
import org.exoplatform.management.common.exportop.AbstractJCRExportOperationHandler;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.config.ValueStorageEntry;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.core.WorkspaceContainerFacade;
import org.exoplatform.services.jcr.ext.backup.BackupConfig;
import org.exoplatform.services.jcr.ext.backup.BackupManager;
import org.exoplatform.services.jcr.ext.backup.RepositoryBackupChain;
import org.exoplatform.services.jcr.ext.backup.RepositoryBackupConfig;
import org.exoplatform.services.jcr.ext.backup.impl.BackupManagerImpl;
import org.exoplatform.services.jcr.ext.registry.RegistryService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.operation.OperationAttributes;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.ExportTask;
import org.gatein.management.api.operation.model.NoResultModel;

/**
 * @author <a href="mailto:boubaker.khanfir@exoplatform.com">Boubaker
 *         Khanfir</a>
 * @version $Revision$
 */
public class BackupExportResource extends AbstractJCRExportOperationHandler {

  private static final Log log = ExoLogger.getLogger(BackupExportResource.class);
  private static final Pattern backupDirPattern = Pattern.compile("^directory:(.*)$");

  private RegistryService registryService = null;
  private BackupManager backupManager = null;

  @Override
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws OperationException {
    try {
      OperationAttributes attributes = operationContext.getAttributes();

      repositoryService = operationContext.getRuntimeContext().getRuntimeComponent(RepositoryService.class);
      registryService = operationContext.getRuntimeContext().getRuntimeComponent(RegistryService.class);
      if (backupManager == null) {
        backupManager = operationContext.getRuntimeContext().getRuntimeComponent(BackupManager.class);
      }

      increaseCurrentTransactionTimeOut(operationContext);

      List<String> filters = attributes.getValues("filter");

      final String repositoryName = operationContext.getAddress().resolvePathTemplate("repository");
      final String workspaceName = operationContext.getAddress().resolvePathTemplate("workspace");

      String backupDir = null;
      if (filters != null && !filters.isEmpty()) {
        for (String filter : filters) {
          Matcher dirMatcher = backupDirPattern.matcher(filter);
          if (dirMatcher.matches()) {
            backupDir = dirMatcher.group(1);
            continue;
          }
        }
      }

      if (backupDir == null) {
        backupDir = PrivilegedSystemHelper.getProperty("java.io.tmpdir");
      }

      final ManageableRepository repository = repositoryName == null ? repositoryService.getCurrentRepository() : repositoryService.getRepository(repositoryName);
      if (repository == null) {
        throw new IllegalArgumentException("No repository was found" + (repositoryName == null ? "" : (" with name :" + repositoryName)));
      }

      final WorkspaceContainerFacade workspaceContainer;
      if (workspaceName != null) {
        workspaceContainer = repository.getWorkspaceContainer(workspaceName);
        if (workspaceContainer == null) {
          throw new IllegalArgumentException("No workspace was found with name :" + workspaceName);
        }
      } else {
        workspaceContainer = null;
      }

      // Initialize backup service
      if (backupManager == null || !FullBackupJob.class.getName().equals(backupManager.getFullBackupType())) {
        InitParams initParams = new InitParams();
        PropertiesParam param = new PropertiesParam();
        param.setName(BackupManagerImpl.BACKUP_PROPERTIES);
        param.setProperty(BackupManagerImpl.BACKUP_DIR, backupDir);
        param.setProperty(BackupManagerImpl.FULL_BACKUP_TYPE, FullBackupJob.class.getName());
        initParams.addParameter(param);
        backupManager = new BackupManagerImpl(null, initParams, repositoryService, registryService);
        ((BackupManagerImpl) backupManager).start();

        // FIXME: Workaround JCR-2405
        fixWorkspacesConfiguration(repository);
      }

      // Prepare BackupConfig
      BackupConfig backupConfig = new BackupConfig();
      backupConfig.setBackupType(BackupManager.FULL_BACKUP_ONLY);
      backupConfig.setBackupDir(new File(backupDir));
      backupConfig.setRepository(repositoryName);

      final RepositoryBackupChain currentBackupChain;
      if (workspaceName == null) {
        // Full JCR repository backup
        log.info("Start full backup of repository to directory: " + backupDir);
        currentBackupChain = backupManager.startBackup((RepositoryBackupConfig) backupConfig);
      } else {
        // JCR workspace backup
        log.info("Start backup of workspace " + workspaceName + " to directory: " + backupDir);
        backupConfig.setWorkspace(workspaceName);
        currentBackupChain = backupManager.startBackup(backupConfig);
      }

      while (!currentBackupChain.isFinished()) {
        try {
          // Block Thread until backup is finished
          Thread.sleep(1000);
        } catch (InterruptedException e) {
          log.error("Error while resuming the SearchIndex services on workspaces", e);
          return;
        }
      }

      // Nothing to return here
      resultHandler.completed(NoResultModel.INSTANCE);
    } catch (Exception e) {
      throw new OperationException(OperationNames.EXPORT_RESOURCE, "Unable to retrieve the list of the contents sites : " + e.getMessage(), e);
    }
  }

  private void fixWorkspacesConfiguration(final ManageableRepository repository) throws RepositoryConfigurationException {
    ArrayList<WorkspaceEntry> workspaceEntries = repository.getConfiguration().getWorkspaceEntries();
    for (WorkspaceEntry workspaceEntry : workspaceEntries) {
      ArrayList<ValueStorageEntry> storageEntries = workspaceEntry.getContainer().getValueStorages();
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

  @Override
  protected void addJCRNodeExportTask(Node childNode, List<ExportTask> subNodesExportTask, boolean recursive, String... params) throws Exception {}
}
