package org.exoplatform.management.registry.operations;

import org.exoplatform.application.registry.ApplicationRegistryService;
import org.exoplatform.management.common.AbstractOperationHandler;
import org.exoplatform.management.registry.tasks.CategoryExportTask;
import org.gatein.management.api.PathAddress;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.ExportResourceModel;

/**
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class CategoryExportResource extends AbstractOperationHandler {
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

    resultHandler.completed(new ExportResourceModel(new CategoryExportTask(categoryName, applicationRegistryService)));
  }

}