/*
 * Copyright (C) 2003-2017 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.exoplatform.management.backup.operations;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.management.backup.service.BackupInProgressException;
import org.exoplatform.management.backup.service.idm.IDMBackup;
import org.exoplatform.management.backup.service.jcr.JCRBackup;
import org.exoplatform.management.backup.service.webui.BackupExceptionHandlerLifeCycle;
import org.exoplatform.management.common.AbstractOperationHandler;
import org.exoplatform.management.service.impl.StagingMessageREST;
import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.scheduler.JobSchedulerService;
import org.exoplatform.web.application.Application;
import org.exoplatform.web.application.ApplicationLifecycle;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.operation.OperationAttributes;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.NoResultModel;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The Class BackupExportResource.
 *
 * @author <a href="mailto:boubaker.khanfir@exoplatform.com">Boubaker
 *         Khanfir</a>
 * @version $Revision$
 */
public class BackupExportResource extends AbstractOperationHandler {

  /** The Constant log. */
  private static final Log log = ExoLogger.getLogger(BackupExportResource.class);
  
  /** The Constant backupDirPattern. */
  private static final Pattern backupDirPattern = Pattern.compile("^directory:(.*)$");

  /** The Constant WRITE_STRATEGY_SUSPEND. */
  public static final String WRITE_STRATEGY_SUSPEND = "suspend";
  
  /** The Constant WRITE_STRATEGY_EXCEPTION. */
  public static final String WRITE_STRATEGY_EXCEPTION = "exception";
  
  /** The Constant WRITE_STRATEGY_NOTHING. */
  public static final String WRITE_STRATEGY_NOTHING = "nothing";

  /** The Constant DISPLAY_MESSAGE_FOR_ALL. */
  public static final String DISPLAY_MESSAGE_FOR_ALL = "all";
  
  /** The Constant DISPLAY_MESSAGE_FOR_ADMIN. */
  public static final String DISPLAY_MESSAGE_FOR_ADMIN = "admin";

  /** The Constant DATE_FORMAT. */
  public static final DateFormat DATE_FORMAT = new SimpleDateFormat("yy-MMM-dd-HH-mm-ss");

  /** The backup in progress. */
  public static boolean backupInProgress = false;
  
  /** The write strategy. */
  public static String writeStrategy = null;
  
  /** The display message for. */
  public static String displayMessageFor = null;
  
  /** The message. */
  public static String message = null;

  /** The job scheduler service. */
  private JobSchedulerService jobSchedulerService = null;

  /**
   * {@inheritDoc}
   */
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
      stagingMessageREST.setPosition("top-center");

      File backupDirFile = getBackupDirectoryFile(attributes, true);

      if (!exportIDM && !exportJCR) {
        throw new OperationException(OperationNames.EXPORT_RESOURCE, "You have to choose IDM, JCR or both datas to backup.");
      }

      if (BackupExportResource.WRITE_STRATEGY_EXCEPTION.equals(BackupExportResource.writeStrategy)) {
        handleWriteOperations();
      }

      String backupParentFolder = backupDirFile.getAbsolutePath() + File.separator + "Backup-" + DATE_FORMAT.format(Calendar.getInstance().getTime());
      backupDirFile = new File(backupParentFolder);
      if (!backupDirFile.mkdir()) {
        throw new IllegalStateException("Cannot create directory: " + backupParentFolder + ". " + (backupDirFile.exists() ? " It already exists." : ""));
      }

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
      throw new OperationException(OperationNames.EXPORT_RESOURCE, e.getMessage(), e);
    } finally {
      backupInProgress = false;
      stagingMessageREST.readParametersFromProperties();
      jobSchedulerService.resume();
      restoreDefaultTransactionTimeOut(portalContainer);
      if (BackupExportResource.WRITE_STRATEGY_EXCEPTION.equals(BackupExportResource.writeStrategy)) {
        deleteWriteOperationHandler();
      }
    }
  }

  /**
   * Handle write operations.
   */
  private void handleWriteOperations() {
    deleteWriteOperationHandler();
    PortalRequestContext portalRequestContext = PortalRequestContext.getCurrentInstance();
    Application application = portalRequestContext.getApplication();
    application.getApplicationLifecycle().add(new BackupExceptionHandlerLifeCycle());
  }

  /**
   * Delete write operation handler.
   */
  private void deleteWriteOperationHandler() {
    PortalRequestContext portalRequestContext = PortalRequestContext.getCurrentInstance();
    Application application = portalRequestContext.getApplication();
    @SuppressWarnings("rawtypes")
    Iterator<ApplicationLifecycle> applicationLifecycleIterator = application.getApplicationLifecycle().iterator();
    while (applicationLifecycleIterator.hasNext()) {
      ApplicationLifecycle<?> applicationLifecycle = (ApplicationLifecycle<?>) applicationLifecycleIterator.next();
      if (applicationLifecycle instanceof BackupExceptionHandlerLifeCycle) {
        applicationLifecycleIterator.remove();
      }
    }
  }

  /**
   * Gets the backup directory file.
   *
   * @param attributes the attributes
   * @param create the create
   * @return the backup directory file
   */
  public static File getBackupDirectoryFile(OperationAttributes attributes, boolean create) {
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

    if (!backupDirFile.exists() && create) {
      backupDirFile.mkdirs();
    }

    return backupDirFile;
  }

}
