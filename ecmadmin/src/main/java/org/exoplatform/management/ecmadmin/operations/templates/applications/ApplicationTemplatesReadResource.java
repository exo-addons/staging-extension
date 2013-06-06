package org.exoplatform.management.ecmadmin.operations.templates.applications;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.exoplatform.services.cms.views.ApplicationTemplateManagerService;
import org.gatein.management.api.PathAddress;
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
public class ApplicationTemplatesReadResource implements OperationHandler {
  @Override
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws OperationException {
    String operationName = operationContext.getOperationName();
    PathAddress address = operationContext.getAddress();

    String applicationName = address.resolvePathTemplate("application-name");
    if (applicationName == null) {
      throw new OperationException(operationName, "No application name specified.");
    }

    Set<String> templates = null;
    ApplicationTemplateManagerService templateManagerService = operationContext.getRuntimeContext().getRuntimeComponent(
        ApplicationTemplateManagerService.class);
    try {
      Set<String> nonSortedTemplates = templateManagerService.getConfiguredAppTemplateMap(applicationName);

      if(nonSortedTemplates != null) {
        // convert to List in order to sort it
        List<String> templatesList = new ArrayList<String>(nonSortedTemplates);
        Collections.sort(templatesList);
        templates = new TreeSet<String>(templatesList);
      } else {
        templates = new TreeSet<String>();
      }
      
    } catch (Exception e) {
      throw new OperationException("Read template applications", "Error while retrieving applications with templates", e);
    }

    resultHandler.completed(new ReadResourceModel("Available application templates", templates));
  }
}