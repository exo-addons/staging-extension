package org.exoplatform.management.idm.operations;

import java.util.ArrayList;
import java.util.List;

import org.exoplatform.management.idm.tasks.IDMExportTask;
import org.exoplatform.platform.organization.injector.DataInjectorService;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.OperationHandler;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.ExportResourceModel;
import org.gatein.management.api.operation.model.ExportTask;

/**
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class IDMExportResource implements OperationHandler {

  private DataInjectorService dataInjectorService;

  
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws OperationException {
    if (dataInjectorService == null) {
      dataInjectorService = operationContext.getRuntimeContext().getRuntimeComponent(DataInjectorService.class);
      if (dataInjectorService == null) {
        throw new OperationException(OperationNames.EXPORT_RESOURCE, "DataInjectorService doesn't exist.");
      }
    }

    try {
      List<ExportTask> exportTasks = new ArrayList<ExportTask>();
      exportTasks.add(new IDMExportTask(dataInjectorService));
      resultHandler.completed(new ExportResourceModel(exportTasks));
    } catch (Exception e) {
      throw new OperationException(operationContext.getOperationName(), "Error while retrieving node types templates", e);
    }
  }

}