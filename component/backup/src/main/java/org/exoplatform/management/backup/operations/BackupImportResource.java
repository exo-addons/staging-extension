package org.exoplatform.management.backup.operations;

import java.io.File;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.management.backup.service.IDMRestore;
import org.exoplatform.management.backup.service.JCRRestore;
import org.exoplatform.management.common.AbstractOperationHandler;
import org.exoplatform.services.cache.CacheService;
import org.exoplatform.services.cache.ExoCache;
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
public class BackupImportResource extends AbstractOperationHandler {

  private static final Log log = ExoLogger.getLogger(BackupImportResource.class);

  @Override
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws OperationException {
    try {
      OperationAttributes attributes = operationContext.getAttributes();
      String portalContainerName = operationContext.getAddress().resolvePathTemplate("portal");
      PortalContainer portalContainer = getPortalContainer(portalContainerName);

      JobSchedulerService jobSchedulerService = (JobSchedulerService) portalContainer.getComponentInstanceOfType(JobSchedulerService.class);
      CacheService cacheService = (CacheService) portalContainer.getComponentInstanceOfType(CacheService.class);

      increaseCurrentTransactionTimeOut(portalContainer);

      // Suspend Thread Schedulers
      jobSchedulerService.suspend();

      File backupDirFile = BackupExportResource.getBackupDirectoryFile(attributes);

      // Restore JCR data
      log.info("Restore JCR Data");
      JCRRestore.restore(portalContainer, backupDirFile);

      // Restore IDM db
      log.info("Start IDM restore.");
      IDMRestore.restore(portalContainer, backupDirFile);

      // Clear all caches based on eXo CacheService
      clearCaches(cacheService);

      // Resume cron jobs only if Restore operation is suscessfully proceeded
      jobSchedulerService.resume();

      // Nothing to return here
      resultHandler.completed(NoResultModel.INSTANCE);
    } catch (Exception e) {
      throw new OperationException(OperationNames.EXPORT_RESOURCE, "Unable to restore Data : " + e.getMessage(), e);
    }
  }

  public void clearCaches(CacheService cacheService) {
    for (Object o : cacheService.getAllCacheInstances()) {
      try {
        ((ExoCache<?, ?>) o).clearCache();
      } catch (Exception wtf) {
        if (log.isTraceEnabled()) {
          log.trace("An exception occurred: " + wtf.getMessage());
        }
      }
    }
  }

}
