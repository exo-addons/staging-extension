package org.exoplatform.management.ecmadmin.operations.templates.applications;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.exoplatform.services.cms.views.ApplicationTemplateManagerService;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.OperationHandler;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.ReadResourceModel;

/**
 * @author <a href="mailto:thomas.delhomenie@exoplatform.com">Thomas
 *         Delhoménie</a>
 * @version $Revision$
 */
public class ApplicationsTemplatesReadResource implements OperationHandler {
  
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws OperationException {
    Set<String> applications = new HashSet<String>();
    ApplicationTemplateManagerService templateManagerService = operationContext.getRuntimeContext().getRuntimeComponent(
        ApplicationTemplateManagerService.class);
    try {
      List<String> applicationNames = templateManagerService.getAllManagedPortletName("repository");
      for (String applicationName : applicationNames) {
        applications.add(applicationName);
      }
    } catch (Exception e) {
      throw new OperationException("Read template applications", "Error while retrieving applications with templates");
    }

    resultHandler.completed(new ReadResourceModel("Available applications", applications));
  }
}