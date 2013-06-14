package org.exoplatform.management.registry.operations;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.exoplatform.application.registry.Application;
import org.exoplatform.application.registry.ApplicationCategory;
import org.exoplatform.application.registry.ApplicationRegistryService;
import org.exoplatform.portal.config.model.ApplicationType;
import org.gatein.management.api.PathAddress;
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
public class CategoryReadResource implements OperationHandler {
  private ApplicationRegistryService applicationRegistryService;

  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws OperationException {
    if (applicationRegistryService == null) {
      applicationRegistryService = operationContext.getRuntimeContext().getRuntimeComponent(ApplicationRegistryService.class);
      if (applicationRegistryService == null) {
        throw new OperationException(OperationNames.EXPORT_RESOURCE, "Cannot get ApplicationRegistryService instance.");
      }
    }
    String operationName = operationContext.getOperationName();
    PathAddress address = operationContext.getAddress();

    String categoryName = address.resolvePathTemplate("category-name");
    if (categoryName == null) {
      throw new OperationException(operationName, "Application Registry Resource: No category name specified.");
    }

    Set<String> result = new HashSet<String>();
    try {
      ApplicationCategory category = applicationRegistryService.getApplicationCategory(categoryName);
      if (category == null) {
        throw new OperationException(operationName, "Application Registry Resource: Category name " + categoryName + " was not found.");
      }
      List<Application> applications = applicationRegistryService.getApplications(category, ApplicationType.GADGET, ApplicationType.PORTLET, ApplicationType.WSRP_PORTLET);
      for (Application application : applications) {
        result.add(application.getDisplayName());
      }
    } catch (Exception e) {
      throw new OperationException(OperationNames.READ_RESOURCE, "Error while retrieving applications from registry.", e);
    }
    resultHandler.completed(new ReadResourceModel("Selected Category applications.", result));
  }
}
