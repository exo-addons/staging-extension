package org.exoplatform.management.backup.service;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.RepositoryException;

import org.exoplatform.commons.utils.ClassLoading;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.config.SimpleParameterEntry;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.config.WorkspaceInitializerEntry;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.core.WorkspaceContainerFacade;
import org.exoplatform.services.jcr.ext.backup.BackupChainLog;
import org.exoplatform.services.jcr.ext.backup.BackupConfig;
import org.exoplatform.services.jcr.ext.backup.BackupConfigurationException;
import org.exoplatform.services.jcr.ext.backup.BackupOperationException;
import org.exoplatform.services.jcr.ext.backup.JobEntryInfo;
import org.exoplatform.services.jcr.ext.backup.RepositoryBackupChainLog;
import org.exoplatform.services.jcr.ext.backup.RepositoryRestoreExeption;
import org.exoplatform.services.jcr.ext.backup.impl.rdbms.RdbmsBackupWorkspaceInitializer;
import org.exoplatform.services.jcr.ext.backup.impl.rdbms.RdbmsWorkspaceInitializer;
import org.exoplatform.services.jcr.impl.core.RepositoryImpl;
import org.exoplatform.services.jcr.impl.core.SessionRegistry;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * @author <a href="mailto:boubaker.khanfir@exoplatform.com">Boubaker
 *         Khanfir</a>
 * @version $Revision$
 */
public class JobRepositoryRestore extends Thread {
  protected static final Log LOG = ExoLogger.getLogger(JobRepositoryRestore.class);

  /**
   * REPOSITORY_RESTORE_STARTED. The state of start restore.
   */
  public static final int REPOSITORY_RESTORE_STARTED = 1;

  /**
   * REPOSITORY_RESTORE_SUCCESSFUL. The state of restore successful.
   */
  public static final int REPOSITORY_RESTORE_SUCCESSFUL = 2;

  /**
   * REPOSITORY_RESTORE_FAIL. The state of restore fail.
   */
  public static final int REPOSITORY_RESTORE_FAIL = 3;

  /**
   * The state of restore.
   */
  private int stateRestore;

  /**
   * The start time of restore.
   */
  private Calendar startTime;

  /**
   * The end time of restore.
   */
  private Calendar endTime;

  /**
   * The exception on restore.
   */
  private Throwable restoreException = null;

  protected RepositoryService repositoryService;

  protected RepositoryEntry repositoryEntry;

  protected Map<String, File> workspacesMapping;

  private File repositoryBackupChainLogFile;

  public JobRepositoryRestore(RepositoryService repoService, RepositoryEntry repositoryEntry, Map<String, File> workspacesMapping, File backupChainLog) {
    super("JobRepositoryRestore " + repositoryEntry.getName());
    this.repositoryService = repoService;
    this.repositoryEntry = repositoryEntry;
    this.workspacesMapping = workspacesMapping;
    this.repositoryBackupChainLogFile = backupChainLog;
  }

  /**
   * Restore repository. Provide information about start and finish process.
   * 
   * @throws RepositoryRestoreExeption
   *           if exception occurred during restore
   */
  public void restore() throws RepositoryRestoreExeption {
    try {
      stateRestore = REPOSITORY_RESTORE_STARTED;
      startTime = Calendar.getInstance();

      restoreRepository();

      stateRestore = REPOSITORY_RESTORE_SUCCESSFUL;
      endTime = Calendar.getInstance();
    } catch (Exception e) {
      stateRestore = REPOSITORY_RESTORE_FAIL;
      restoreException = e;
      throw new RepositoryRestoreExeption(e.getMessage(), e);
    }
  }

  protected void restoreRepository() throws Exception {
    List<WorkspaceEntry> originalWorkspaceEntrys = repositoryEntry.getWorkspaceEntries();

    // Getting system workspace entry
    WorkspaceEntry systemWorkspaceEntry = null;

    for (WorkspaceEntry wsEntry : originalWorkspaceEntrys) {
      if (wsEntry.getName().equals(repositoryEntry.getSystemWorkspaceName())) {
        systemWorkspaceEntry = wsEntry;
        break;
      }
    }

    if (systemWorkspaceEntry == null) {
      throw new RepositoryRestoreExeption("Can not restore workspace \"" + repositoryEntry.getSystemWorkspaceName() + " in repository \"" + repositoryEntry.getName() + "\"."
          + " The related configuration cannot be found.");
    }
    WorkspaceInitializerEntry wieOriginal = systemWorkspaceEntry.getInitializer();

    // getting backup chail log to system workspace.
    BackupChainLog systemBackupChainLog = new BackupChainLog(workspacesMapping.get(systemWorkspaceEntry.getName()));

    WorkspaceInitializerEntry wiEntry = getWorkspaceInitializerEntry(systemBackupChainLog);

    // set initializer
    systemWorkspaceEntry.setInitializer(wiEntry);

    ArrayList<WorkspaceEntry> newEntries = new ArrayList<WorkspaceEntry>();
    newEntries.add(systemWorkspaceEntry);

    repositoryEntry.setWorkspaceEntries(newEntries);

    String currennWorkspaceName = repositoryEntry.getSystemWorkspaceName();

    boolean restored = true;
    try {
      LOG.info("Trying to create the repository '" + repositoryEntry.getName() + "'");
      repositoryService.createRepository(repositoryEntry);

      // set original initializer to created workspace.
      RepositoryImpl defRep = (RepositoryImpl) repositoryService.getRepository(repositoryEntry.getName());
      WorkspaceContainerFacade wcf = defRep.getWorkspaceContainer(systemWorkspaceEntry.getName());
      WorkspaceEntry createdWorkspaceEntry = (WorkspaceEntry) wcf.getComponent(WorkspaceEntry.class);
      createdWorkspaceEntry.setInitializer(wieOriginal);

      for (WorkspaceEntry wsEntry : originalWorkspaceEntrys) {
        if (!(wsEntry.getName().equals(repositoryEntry.getSystemWorkspaceName()))) {
          currennWorkspaceName = wsEntry.getName();
          LOG.info("Trying to restore the workspace " + wsEntry.getName());
          restoreOverInitializer(new BackupChainLog(workspacesMapping.get(wsEntry.getName())), repositoryEntry.getName(), wsEntry);
        }
      }
    } catch (Exception e) {
      restored = false;
      LOG.error("Can not restore workspace \"" + currennWorkspaceName + " in repository \"" + repositoryEntry.getName() + "\".", e);
      throw new RepositoryRestoreExeption("Can not restore workspace \"" + currennWorkspaceName + " in repository \"" + repositoryEntry.getName() + "\".", e);
    } finally {
      if (!restored) {
        try {
          removeRepository(repositoryService, repositoryEntry.getName());
        } catch (Exception exp) {
          LOG.error("The partly restored repository \"" + repositoryEntry.getName() + "\" can not be removed.", exp);
        }
      }
    }
  }

  protected void removeRepository(RepositoryService repositoryService, String repositoryName) throws RepositoryException, RepositoryConfigurationException {
    ManageableRepository mr = null;
    try {
      mr = repositoryService.getRepository(repositoryName);
    } catch (RepositoryException e) {
      // Nothing to catch, Repository was not found
    }
    if (mr != null) {
      closeAllSession(mr);
      repositoryService.removeRepository(repositoryName);
    }
  }

  private WorkspaceInitializerEntry getWorkspaceInitializerEntry(BackupChainLog systemBackupChainLog) throws BackupOperationException, ClassNotFoundException {
    String fullBackupPath = systemBackupChainLog.getJobEntryInfos().get(0).getURL().getPath();
    String fullbackupType = null;
    try {
      if ((ClassLoading.forName(systemBackupChainLog.getFullBackupType(), this).equals(FullBackupJob.class))) {
        fullbackupType = systemBackupChainLog.getFullBackupType();
      } else {
        throw new BackupOperationException("Class  \"" + systemBackupChainLog.getFullBackupType() + "\" is not support as full backup.");
      }
    } catch (ClassNotFoundException e) {
      throw new BackupOperationException("Class \"" + systemBackupChainLog.getFullBackupType() + "\" is not found.", e);
    }
    WorkspaceInitializerEntry wiEntry = new WorkspaceInitializerEntry();
    if ((ClassLoading.forName(fullbackupType, this).equals(FullBackupJob.class))) {
      // set the initializer RdbmsBackupWorkspaceInitializer
      wiEntry.setType(RdbmsBackupWorkspaceInitializer.class.getCanonicalName());

      List<SimpleParameterEntry> wieParams = new ArrayList<SimpleParameterEntry>();
      wieParams.add(new SimpleParameterEntry(RdbmsBackupWorkspaceInitializer.RESTORE_PATH_PARAMETER, (new File(fullBackupPath).getParent())));

      wiEntry.setParameters(wieParams);
    } else {
      throw new BackupOperationException("Class  \"" + systemBackupChainLog.getFullBackupType() + "\" is not support as full backup.");
    }
    return wiEntry;
  }

  private void closeAllSession(ManageableRepository mr) throws NoSuchWorkspaceException {
    for (String wsName : mr.getWorkspaceNames()) {
      if (!mr.canRemoveWorkspace(wsName)) {
        WorkspaceContainerFacade wc = mr.getWorkspaceContainer(wsName);
        SessionRegistry sessionRegistry = (SessionRegistry) wc.getComponent(SessionRegistry.class);
        sessionRegistry.closeSessions(wsName);
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void run() {
    try {
      restore();
    } catch (Throwable t) // NOSONAR
    {
      LOG.error("The restore was fail", t);
    }
  }

  /**
   * getRestoreException.
   * 
   * @return Throwable return the exception of repository restore.
   */
  public Throwable getRestoreException() {
    return restoreException;
  }

  /**
   * getStateRestore.
   * 
   * @return int return state of restore.
   */
  public int getStateRestore() {
    return stateRestore;
  }

  /**
   * getBeginTime.
   * 
   * @return Calendar return the start time of restore
   */
  public Calendar getStartTime() {
    return startTime;
  }

  /**
   * getEndTime.
   * 
   * @return Calendar return the end time of restore
   */
  public Calendar getEndTime() {
    return endTime;
  }

  /**
   * getRepositoryName.
   *
   * @return String the name of destination repository
   */
  public String getRepositoryName() {
    return repositoryEntry.getName();
  }

  /**
   * GetRepositoryBackupChainLog.
   * 
   * @return repositoryBackupChainLog
   */
  public RepositoryBackupChainLog getRepositoryBackupChainLog() throws BackupOperationException {
    return new RepositoryBackupChainLog(repositoryBackupChainLogFile);
  }

  /**
   * getRepositoryEntry.
   * 
   * @return repositoryBackupChainLog
   */
  public RepositoryEntry getRepositoryEntry() {
    return repositoryEntry;
  }

  protected void restoreOverInitializer(BackupChainLog log, String repositoryName, WorkspaceEntry workspaceEntry) throws BackupOperationException, RepositoryException,
      RepositoryConfigurationException, BackupConfigurationException {
    List<JobEntryInfo> list = log.getJobEntryInfos();
    BackupConfig config = log.getBackupConfig();

    String reposytoryName = (repositoryName == null ? config.getRepository() : repositoryName);
    String workspaceName = workspaceEntry.getName();

    String fullbackupType = null;

    try {
      if ((ClassLoading.forName(log.getFullBackupType(), this).equals(FullBackupJob.class))) {
        fullbackupType = log.getFullBackupType();
      } else if ((ClassLoading.forName(log.getFullBackupType(), this).equals(org.exoplatform.services.jcr.ext.backup.impl.rdbms.FullBackupJob.class))) {
        fullbackupType = log.getFullBackupType();
      } else {
        throw new BackupOperationException("Class  \"" + log.getFullBackupType() + "\" is not support as full backup.");
      }
    } catch (ClassNotFoundException e) {
      throw new BackupOperationException("Class \"" + log.getFullBackupType() + "\" is not found.", e);
    }

    // ws should not exists.
    if (!workspaceAlreadyExist(reposytoryName, workspaceName)) {
      for (int i = 0; i < list.size(); i++) {
        try {
          fullRestoreOverInitializer(list.get(i).getURL().getPath(), reposytoryName, workspaceEntry, fullbackupType);
        } catch (Exception e) {
          throw new BackupOperationException("Restore error", e);
        }
      }
    } else {
      throw new BackupConfigurationException("Workspace \"" + workspaceName + "\" should not exists.");
    }
  }

  private void fullRestoreOverInitializer(String pathBackupFile, String repositoryName, WorkspaceEntry workspaceEntry, String fBackupType) throws Exception {
    WorkspaceInitializerEntry wieOriginal = workspaceEntry.getInitializer();
    RepositoryImpl defRep = (RepositoryImpl) repositoryService.getRepository(repositoryName);
    WorkspaceInitializerEntry wiEntry = new WorkspaceInitializerEntry();
    if ((ClassLoading.forName(fBackupType, this).equals(FullBackupJob.class))) {
      // set the initializer RdbmsWorkspaceInitializer
      wiEntry.setType(CustomWorkspaceInitializer.class.getCanonicalName());

      List<SimpleParameterEntry> wieParams = new ArrayList<SimpleParameterEntry>();
      wieParams.add(new SimpleParameterEntry(RdbmsWorkspaceInitializer.RESTORE_PATH_PARAMETER, new File(pathBackupFile).getParent()));

      wiEntry.setParameters(wieParams);
    }
    String wsName = workspaceEntry.getName();
    if (defRep.isWorkspaceInitialized(wsName)) {
      if (defRep.canRemoveWorkspace(wsName)) {
        defRep.removeWorkspace(wsName);
      } else {
        LOG.error("Cannot initialize workspace {}", wsName);
      }
    }
    workspaceEntry.setInitializer(wiEntry);
    // Restore Workspace
    defRep.configWorkspace(workspaceEntry);
    CustomWorkspaceInitializer.setRestoreInProgress(true); // Force initialized
                                                           // flag on
                                                           // WorkspaceInitializer
                                                           // to be false
    defRep.createWorkspace(wsName);
    CustomWorkspaceInitializer.setRestoreInProgress(false); // Resume
                                                            // initialized flag
                                                            // to its original
                                                            // value
    // set original workspace initializer
    WorkspaceContainerFacade wcf = defRep.getWorkspaceContainer(wsName);
    WorkspaceEntry createdWorkspaceEntry = (WorkspaceEntry) wcf.getComponent(WorkspaceEntry.class);
    createdWorkspaceEntry.setInitializer(wieOriginal);
  }

  private boolean workspaceAlreadyExist(String repository, String workspace) throws RepositoryException, RepositoryConfigurationException {
    String[] ws = repositoryService.getRepository(repository).getWorkspaceNames();
    for (int i = 0; i < ws.length; i++) {
      if (ws[i].equals(workspace)) {
        return true;
      }
    }
    return false;
  }
}