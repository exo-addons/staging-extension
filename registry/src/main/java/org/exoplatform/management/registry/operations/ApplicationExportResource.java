package org.exoplatform.management.registry.operations;

import org.exoplatform.application.registry.ApplicationRegistryService;
import org.exoplatform.management.registry.tasks.ApplicationExportTask;
import org.gatein.management.api.PathAddress;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.OperationHandler;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.ExportResourceModel;

/**
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class ApplicationExportResource implements OperationHandler {
  private ApplicationRegistryService applicationRegistryService;

  @Override
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws OperationException {
    if (applicationRegistryService == null) {
      applicationRegistryService = operationContext.getRuntimeContext().getRuntimeComponent(ApplicationRegistryService.class);
      if (applicationRegistryService == null) {
        throw new OperationException(OperationNames.EXPORT_RESOURCE, "Cannot get ApplicationRegistryService instance.");
      }
    }

    PathAddress address = operationContext.getAddress();
    String categoryName = address.resolvePathTemplate("category-name");
    String applicationName = address.resolvePathTemplate("application-name");

    resultHandler.completed(new ExportResourceModel(new ApplicationExportTask(applicationName, categoryName, applicationRegistryService)));
  }

}