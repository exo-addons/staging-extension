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

import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.management.backup.service.idm.IDMRestore;
import org.exoplatform.management.backup.service.jcr.JCRRestore;
import org.exoplatform.management.common.AbstractOperationHandler;
import org.exoplatform.services.cache.CacheService;
import org.exoplatform.services.cache.ExoCache;
import org.exoplatform.services.cms.folksonomy.impl.NewFolksonomyServiceImpl;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.idm.PicketLinkIDMCacheService;
import org.exoplatform.services.scheduler.JobSchedulerService;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.operation.OperationAttributes;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.NoResultModel;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * The Class BackupImportResource.
 *
 * @author <a href="mailto:boubaker.khanfir@exoplatform.com">Boubaker
 *         Khanfir</a>
 * @version $Revision$
 */
public class BackupImportResource extends AbstractOperationHandler {

  /** The Constant log. */
  private static final Log log = ExoLogger.getLogger(BackupImportResource.class);

  /** The restore in progress. */
  public static boolean restoreInProgress = false;

  /**
   * {@inheritDoc}
   */
  @Override
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws OperationException {
    Map<Object, Throwable> endedServicesLifecycle = null;

    OperationAttributes attributes = operationContext.getAttributes();

    File backupDirFile = BackupExportResource.getBackupDirectoryFile(attributes, false);
    if (!backupDirFile.exists() || !backupDirFile.isDirectory()) {
      throw new OperationException(OperationNames.IMPORT_RESOURCE, "Backup folder doesn't exists or is not a folder: " + backupDirFile.getAbsolutePath());
    }

    String portalContainerName = operationContext.getAddress().resolvePathTemplate("portal");
    PortalContainer portalContainer = getPortalContainer(portalContainerName);

    JobSchedulerService jobSchedulerService = (JobSchedulerService) portalContainer.getComponentInstanceOfType(JobSchedulerService.class);
    PicketLinkIDMCacheService idmCacheService = (PicketLinkIDMCacheService) portalContainer.getComponentInstanceOfType(PicketLinkIDMCacheService.class);
    CacheService cacheService = (CacheService) portalContainer.getComponentInstanceOfType(CacheService.class);

    // Suspend Thread Schedulers
    log.info("Suspend Jobs Scheduler Service");
    jobSchedulerService.suspend();

    increaseCurrentTransactionTimeOut(portalContainer);
    restoreInProgress = true;
    try {

      // Close transactions of current Thread
      endedServicesLifecycle = RequestLifeCycle.end();

      IDMRestore.verifyDSConnections(portalContainer);

      // Clear all caches based on eXo CacheService
      log.info("Clear Services caches");
      clearCaches(cacheService, idmCacheService);

      // Restore IDM db
      log.info("Restore IDM Data");
      boolean idmRestored = IDMRestore.restore(portalContainer, backupDirFile);

      // Restore JCR data
      log.info("Restore JCR Data");
      List<File> logFiles = JCRRestore.restore(portalContainer, backupDirFile);

      if (!idmRestored) {
        if (logFiles == null || logFiles.size() == 0) {
          throw new IllegalStateException("Backup files was not found in: " + backupDirFile.getAbsolutePath());
        } else if (logFiles.size() > 1) {
          throw new IllegalStateException("Multiple backup directories was found in: " + backupDirFile.getAbsolutePath());
        }
      }

      // Clear all caches based on eXo CacheService
      log.info("Clear Services caches");
      clearCaches(cacheService, idmCacheService);

      // Resume cron jobs only if Restore operation is successfully processed
      log.info("Resume Jobs Scheduler Service");
      jobSchedulerService.resume();

      log.info("Restore operation finished successfully. Recoved from {}", backupDirFile);

      // Nothing to return here
      resultHandler.completed(NoResultModel.INSTANCE);
    } catch (Exception e) {
      throw new OperationException(OperationNames.IMPORT_RESOURCE, e.getMessage(), e);
    } finally {
      restoreInProgress = false;

      restoreDefaultTransactionTimeOut(portalContainer);

      // Reopen transactions for current Thread
      if (endedServicesLifecycle != null && !endedServicesLifecycle.isEmpty()) {
        RequestLifeCycle.begin(portalContainer);
      }
    }
  }

  /**
   * Clear caches.
   *
   * @param cacheService the cache service
   * @param idmCacheService the idm cache service
   */
  public void clearCaches(CacheService cacheService, PicketLinkIDMCacheService idmCacheService) {
    for (Object o : cacheService.getAllCacheInstances()) {
      try {
        // FIXME PLF-6526
        // Workaround : Avoid clear Folksonomy cache, it's not computed again
        if (!NewFolksonomyServiceImpl.class.getName().equals(((ExoCache<?, ?>) o).getName())) {
          ((ExoCache<?, ?>) o).clearCache();
        }
      } catch (Exception e) {
        log.warn("An exception occurred", e);
      }
    }
    if (idmCacheService != null) {
      idmCacheService.invalidateAll();
    }
  }
}
