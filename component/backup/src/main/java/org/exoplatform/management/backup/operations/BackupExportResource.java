package org.exoplatform.management.backup.operations;

import java.io.File;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.management.backup.service.IDMBackup;
import org.exoplatform.management.backup.service.JCRBackup;
import org.exoplatform.management.common.AbstractOperationHandler;
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

  private JobSchedulerService jobSchedulerService = null;

  @Override
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws OperationException {
    try {
      OperationAttributes attributes = operationContext.getAttributes();
      String portalContainerName = operationContext.getAddress().resolvePathTemplate("portal");
      PortalContainer portalContainer = getPortalContainer(portalContainerName);

      String exportJCRString = attributes.getValue("export-jcr");
      String exportIDMString = attributes.getValue("export-idm");

      boolean exportJCR = StringUtils.isEmpty(exportJCRString) ? true : exportJCRString.trim().equalsIgnoreCase("true");
      boolean exportIDM = StringUtils.isEmpty(exportIDMString) ? true : exportIDMString.trim().equalsIgnoreCase("true");

      if (!exportIDM && !exportJCR) {
        throw new OperationException(OperationNames.EXPORT_RESOURCE, "You have to choose IDM, JCR or both datas to backup.");
      }

      increaseCurrentTransactionTimeOut(portalContainer);

      jobSchedulerService = (JobSchedulerService) portalContainer.getComponentInstanceOfType(JobSchedulerService.class);

      // Suspend Thread Schedulers
      jobSchedulerService.suspend();

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
      jobSchedulerService.resume();
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
    if (backupDir == null || !new File(backupDir).exists()) {
      throw new IllegalArgumentException("backup directory was not found: " + backupDir);
    }
    return new File(backupDir);
  }
}
