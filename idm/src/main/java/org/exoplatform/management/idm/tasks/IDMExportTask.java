package org.exoplatform.management.idm.tasks;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.compress.utils.IOUtils;
import org.exoplatform.platform.organization.injector.DataInjectorService;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.model.ExportTask;

/**
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class IDMExportTask implements ExportTask {
  private final DataInjectorService dataInjectorService;

  public IDMExportTask(DataInjectorService dataInjectorService) {
    this.dataInjectorService = dataInjectorService;
  }

  
  public String getEntry() {
    return "idm.zip";
  }

  
  public void export(OutputStream outputStream) throws IOException {
    File zosFile = File.createTempFile("idm-users", ".zip");
    ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zosFile));
    try {
      dataInjectorService.writeUsers(zos);
      dataInjectorService.writeProfiles(zos);
      dataInjectorService.writeOrganizationModelData(zos);
    } catch (Exception exception) {
      throw new OperationException(OperationNames.EXPORT_RESOURCE, "Error exporting users: " + exception.getMessage(), exception);
    }
    zos.close();

    FileInputStream fileInputStream = new FileInputStream(zosFile);
    IOUtils.copy(fileInputStream, outputStream);
  }

}
