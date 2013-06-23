package org.exoplatform.management.ecmadmin.operations.templates.applications;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;

import org.exoplatform.services.cms.views.ApplicationTemplateManagerService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
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

    ApplicationTemplateManagerService applicationTemplateManagerService = operationContext.getRuntimeContext().getRuntimeComponent(ApplicationTemplateManagerService.class);
    try {
      Node templatesHome = applicationTemplateManagerService.getApplicationTemplateHome(applicationName, SessionProvider.createSystemProvider());
      if (templatesHome != null) {
        if (applicationName.endsWith(".gtmpl")) {
          exportTasks.add(new ApplicationTemplateExportTask(applicationTemplateManagerService, templatesHome.getParent().getParent().getName(), templatesHome.getParent().getName(), templatesHome.getName()));
        } else {
          NodeIterator nodeIterator = templatesHome.getNodes();
          while (nodeIterator.hasNext()) {
            Node categoryNode = nodeIterator.nextNode();
            if (categoryNode.getName().endsWith(".gtmpl")) {
              Node templateNode = categoryNode;
              exportTasks.add(new ApplicationTemplateExportTask(applicationTemplateManagerService, applicationName, categoryNode.getName(), templateNode.getName()));
            } else {
              NodeIterator templatesIterator = categoryNode.getNodes();
              while (templatesIterator.hasNext()) {
                Node templateNode = templatesIterator.nextNode();
                exportTasks.add(new ApplicationTemplateExportTask(applicationTemplateManagerService, applicationName, categoryNode.getName(), templateNode.getName()));
              }
            }
          }
        }
      }

      resultHandler.completed(new ExportResourceModel(exportTasks));
    } catch (Exception e) {
      throw new OperationException(OperationNames.READ_RESOURCE, "Error while retrieving applications with templates", e);
    }
  }
}