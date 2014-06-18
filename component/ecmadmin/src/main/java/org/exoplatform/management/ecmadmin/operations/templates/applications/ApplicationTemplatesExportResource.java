package org.exoplatform.management.ecmadmin.operations.templates.applications;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

  private Pattern templateEntryPattern = Pattern.compile("(.*)/(.*)/(.*)\\.gtmpl");

  @Override
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws OperationException {
    String operationName = operationContext.getOperationName();
    PathAddress address = operationContext.getAddress();

    String applicationName = address.resolvePathTemplate("application-name");
    if (applicationName == null) {
      throw new OperationException(operationName, "No application name specified.");
    }

    Matcher matcher = templateEntryPattern.matcher(applicationName);
    String categoryName = null;
    String templateName = null;
    if (matcher.find()) {
      applicationName = matcher.group(1);
      categoryName = matcher.group(2);
      templateName = matcher.group(3) + ".gtmpl";
    }

    List<ExportTask> exportTasks = new ArrayList<ExportTask>();

    ApplicationTemplateManagerService applicationTemplateManagerService = operationContext.getRuntimeContext().getRuntimeComponent(ApplicationTemplateManagerService.class);
    try {
      Node templatesHome = applicationTemplateManagerService.getApplicationTemplateHome(applicationName, SessionProvider.createSystemProvider());
      if (templatesHome != null) {
        ApplicationTemplatesMetadata metadata = new ApplicationTemplatesMetadata();
        if (templateName != null) {
          exportTasks.add(new ApplicationTemplateExportTask(applicationTemplateManagerService, applicationName, categoryName, templateName, metadata));
        } else {

          NodeIterator nodeIterator = null;
          if (categoryName == null) {
            nodeIterator = templatesHome.getNodes();
          } else {
            nodeIterator = templatesHome.getNode(categoryName).getNodes();
          }
          while (nodeIterator.hasNext()) {
            Node categoryNode = nodeIterator.nextNode();
            if (categoryNode.getName().endsWith(".gtmpl")) {
              Node templateNode = categoryNode;
              categoryNode = templateNode.getParent();
              exportTasks.add(new ApplicationTemplateExportTask(applicationTemplateManagerService, applicationName, categoryNode.getName(), templateNode.getName(), metadata));
            } else {
              NodeIterator templatesIterator = categoryNode.getNodes();
              while (templatesIterator.hasNext()) {
                Node templateNode = templatesIterator.nextNode();
                exportTasks.add(new ApplicationTemplateExportTask(applicationTemplateManagerService, applicationName, categoryNode.getName(), templateNode.getName(), metadata));
              }
            }
          }
        }
        exportTasks.add(new ApplicationTemplatesMetaDataExportTask(metadata, applicationName));
      }

      resultHandler.completed(new ExportResourceModel(exportTasks));
    } catch (Exception e) {
      throw new OperationException(OperationNames.READ_RESOURCE, "Error while retrieving applications with templates", e);
    }
  }
}