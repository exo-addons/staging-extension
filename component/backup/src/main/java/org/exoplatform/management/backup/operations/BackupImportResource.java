package org.exoplatform.management.backup.operations;

import java.io.File;
import java.util.Map;

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

/**
 * @author <a href="mailto:boubaker.khanfir@exoplatform.com">Boubaker
 *         Khanfir</a>
 * @version $Revision$
 */
public class BackupImportResource extends AbstractOperationHandler {

  private static final Log log = ExoLogger.getLogger(BackupImportResource.class);

  public static boolean restoreInProgress = false;

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
      IDMRestore.restore(portalContainer, backupDirFile);

      // Restore JCR data
      log.info("Restore JCR Data");
      JCRRestore.restore(portalContainer, backupDirFile);

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
