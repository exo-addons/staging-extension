package org.exoplatform.management.registry.operations;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.exoplatform.application.registry.ApplicationCategory;
import org.exoplatform.application.registry.ApplicationRegistryService;
import org.exoplatform.management.common.AbstractOperationHandler;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.ReadResourceModel;

/**
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class RegistryReadResource extends AbstractOperationHandler {
  private ApplicationRegistryService applicationRegistryService;

  @Override
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws OperationException {
    if (applicationRegistryService == null) {
      applicationRegistryService = operationContext.getRuntimeContext().getRuntimeComponent(ApplicationRegistryService.class);
      if (applicationRegistryService == null) {
        throw new OperationException(OperationNames.EXPORT_RESOURCE, "Cannot get ApplicationRegistryService instance.");
      }
    }
    Set<String> result = new HashSet<String>();
    try {
      List<ApplicationCategory> categories = applicationRegistryService.getApplicationCategories();
      for (ApplicationCategory category : categories) {
        result.add(category.getName());
      }
    } catch (Exception e) {
      throw new OperationException(OperationNames.READ_RESOURCE, "Error while retrieving application registry categories.", e);
    }
    resultHandler.completed(new ReadResourceModel("Application registry categories.", result));
  }
}
