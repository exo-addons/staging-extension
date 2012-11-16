package org.exoplatform.management.ecmadmin.operations.drive;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.exoplatform.services.cms.drives.DriveData;
import org.exoplatform.services.cms.drives.ManageDriveService;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.OperationHandler;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.ReadResourceModel;

/**
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class DriveReadResource implements OperationHandler {
  private ManageDriveService driveService;

  @Override
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws OperationException {
    if (driveService == null) {
      driveService = operationContext.getRuntimeContext().getRuntimeComponent(ManageDriveService.class);
    }
    try {
      Set<String> driveNames = new HashSet<String>();
      List<DriveData> driveDataList = driveService.getAllDrives();
      for (DriveData driveData : driveDataList) {
        driveNames.add(driveData.getName());
      }
      resultHandler.completed(new ReadResourceModel("Available drives.", driveNames));
    } catch (Exception e) {
      throw new OperationException(OperationNames.READ_RESOURCE, "Error while retrieving ECMS drives.", e);
    }
  }
}