package org.exoplatform.management.backup.operations;

import java.io.File;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.management.backup.service.BackupInProgressException;
import org.exoplatform.management.backup.service.IDMBackup;
import org.exoplatform.management.backup.service.JCRBackup;
import org.exoplatform.management.common.AbstractOperationHandler;
import org.exoplatform.management.service.impl.StagingMessageREST;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.scheduler.JobSchedulerService;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.operation.OperationAttributes;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.NoResultModel;

/**
 * @author <a href="mailto:boubaker.khanfir@exoplatform.com">Boubaker
 *         Khanfir</a>
 * @version $Revision$
 */
public class BackupExportResource extends AbstractOperationHandler {

  private static final Log log = ExoLogger.getLogger(BackupExportResource.class);
  private static final Pattern backupDirPattern = Pattern.compile("^directory:(.*)$");

  public static final String WRITE_STRATEGY_SUSPEND = "suspend";
  public static final String WRITE_STRATEGY_EXCEPTION = "exception";

  public static final String DISPLAY_MESSAGE_FOR_ALL = "all";
  public static final String DISPLAY_MESSAGE_FOR_ADMIN = "admin";

  public static boolean backupInProgress = false;
  public static String writeStrategy = null;
  public static String displayMessageFor = null;
  public static String message = null;

  private JobSchedulerService jobSchedulerService = null;

  @Override
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws OperationException {
    if (backupInProgress) {
      throw new BackupInProgressException();
    }
    String portalContainerName = operationContext.getAddress().resolvePathTemplate("portal");
    PortalContainer portalContainer = getPortalContainer(portalContainerName);
    StagingMessageREST stagingMessageREST = (StagingMessageREST) portalContainer.getComponentInstanceOfType(StagingMessageREST.class);

    increaseCurrentTransactionTimeOut(portalContainer);

    // Suspend Thread Schedulers
    jobSchedulerService = (JobSchedulerService) portalContainer.getComponentInstanceOfType(JobSchedulerService.class);
    jobSchedulerService.suspend();

    OperationAttributes attributes = operationContext.getAttributes();

    backupInProgress = true;
    writeStrategy = null;
    displayMessageFor = null;
    message = null;
    try {
      String exportJCRString = attributes.getValue("export-jcr");
      String exportIDMString = attributes.getValue("export-idm");

      boolean exportJCR = StringUtils.isEmpty(exportJCRString) ? true : exportJCRString.trim().equalsIgnoreCase("true");
      boolean exportIDM = StringUtils.isEmpty(exportIDMString) ? true : exportIDMString.trim().equalsIgnoreCase("true");

      writeStrategy = attributes.getValue("writeStrategy");

      displayMessageFor = attributes.getValue("displayMessageFor");
      message = attributes.getValue("message");

      stagingMessageREST.setDisplayForAll(DISPLAY_MESSAGE_FOR_ALL.equals(displayMessageFor));
      stagingMessageREST.setMessage(message);

      if (!exportIDM && !exportJCR) {
        throw new OperationException(OperationNames.EXPORT_RESOURCE, "You have to choose IDM, JCR or both datas to backup.");
      }

      File backupDirFile = getBackupDirectoryFile(attributes);

      if (exportJCR) {
        // Backup JCR
        log.info("Start full backup of repository to directory: " + backupDirFile);
        JCRBackup.backup(portalContainer, backupDirFile);
      }

      if (exportIDM) {
        // Backup IDM
        log.info("Start full backup of IDM to directory: " + backupDirFile);
        IDMBackup.backup(portalContainer, backupDirFile);
      }

      log.info("Backup operation finished successfully. Files under {} ", backupDirFile);

      // Nothing to return here
      resultHandler.completed(NoResultModel.INSTANCE);
    } catch (Exception e) {
      throw new OperationException(OperationNames.EXPORT_RESOURCE, "Unable to backup Data : " + e.getMessage(), e);
    } finally {
      backupInProgress = false;
      stagingMessageREST.readParametersFromProperties();
      jobSchedulerService.resume();
      restoreDefaultTransactionTimeOut(portalContainer);
    }
  }

  public static File getBackupDirectoryFile(OperationAttributes attributes) {
    String backupDir = attributes.getValue("directory");
    if (backupDir == null) {
      List<String> filters = attributes.getValues("filter");
      if (filters != null && !filters.isEmpty()) {
        for (String filter : filters) {
          Matcher dirMatcher = backupDirPattern.matcher(filter);
          if (dirMatcher.matches()) {
            backupDir = dirMatcher.group(1);
            continue;
          }
        }
      }
    }
    if (backupDir == null) {
      throw new IllegalArgumentException("backup directory was not found: " + backupDir);
    }

    File backupDirFile = new File(backupDir);

    if (!backupDirFile.exists()) {
      backupDirFile.mkdirs();
    }

    return backupDirFile;
  }
}
