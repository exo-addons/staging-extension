package org.exoplatform.management.ecmadmin.operations.templates.applications;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.exoplatform.services.cms.views.ApplicationTemplateManagerService;
import org.gatein.management.api.PathAddress;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.OperationHandler;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.ExportResourceModel;
import org.gatein.management.api.operation.model.ExportTask;

/**
 * @author <a href="mailto:thomas.delhomenie@exoplatform.com">Thomas
 *         Delhoménie</a>
 * @version $Revision$
 */
public class ApplicationTemplatesExportResource implements OperationHandler {
  @Override
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws OperationException {
    String operationName = operationContext.getOperationName();
    PathAddress address = operationContext.getAddress();

    String applicationName = address.resolvePathTemplate("application-name");
    if (applicationName == null) {
      throw new OperationException(operationName, "No application name specified.");
    }

    List<ExportTask> exportTasks = new ArrayList<ExportTask>();

    Set<String> templates = null;
    ApplicationTemplateManagerService applicationTemplateManagerService = operationContext.getRuntimeContext()
        .getRuntimeComponent(ApplicationTemplateManagerService.class);
    try {
      templates = applicationTemplateManagerService.getConfiguredAppTemplateMap(applicationName);
      for (String template : templates) {
        exportTasks.add(new ApplicationTemplateExportTask(applicationTemplateManagerService, applicationName, template));
      }

      resultHandler.completed(new ExportResourceModel(exportTasks));
    } catch (Exception e) {
      throw new OperationException(OperationNames.READ_RESOURCE, "Error while retrieving applications with templates", e);
    }
  }
}