package org.exoplatform.management.idm.operations;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.compress.utils.IOUtils;
import org.exoplatform.platform.organization.injector.DataInjectorService;
import org.gatein.common.logging.Logger;
import org.gatein.common.logging.LoggerFactory;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.operation.OperationAttachment;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.OperationHandler;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.NoResultModel;

/**
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class IDMImportResource implements OperationHandler {

  final private static Logger log = LoggerFactory.getLogger(IDMImportResource.class);

  private DataInjectorService dataInjectorService;

  
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws OperationException {
    if (dataInjectorService == null) {
      dataInjectorService = operationContext.getRuntimeContext().getRuntimeComponent(DataInjectorService.class);
      if (dataInjectorService == null) {
        throw new OperationException(OperationNames.IMPORT_RESOURCE, "DataInjectorService doesn't exist.");
      }
    }
    OperationAttachment attachment = operationContext.getAttachment(false);

    InputStream attachmentInputStream = attachment.getStream();
    if (attachmentInputStream == null) {
      throw new OperationException(OperationNames.IMPORT_RESOURCE, "No data stream available for IDM import.");
    }

    // extract data from zip
    try {
      final ZipInputStream zis = new ZipInputStream(attachmentInputStream);
      ZipEntry entry;
      while ((entry = zis.getNextEntry()) != null) {
        // & Skip entries not in zip format
        if (!entry.getName().endsWith(".zip")) {
          continue;
        }

        File zosFile = File.createTempFile("idm-users", ".zip");
        FileOutputStream fos = new FileOutputStream(zosFile);
        IOUtils.copy(zis, fos);
        fos.close();

        dataInjectorService.readDataPlugins(zosFile.getAbsolutePath());
        dataInjectorService.readUsersData(zosFile.getAbsolutePath());
        dataInjectorService.readUserProfilesData(zosFile.getAbsolutePath());
        dataInjectorService.doImport(true);
        zis.closeEntry();
      }
      zis.close();

      log.info("Organization model data successfully imported.");
    } catch (Exception e) {
      throw new OperationException(OperationNames.IMPORT_RESOURCE, "Error while importing Organization Data.", e);
    }
    resultHandler.completed(NoResultModel.INSTANCE);
  }

}