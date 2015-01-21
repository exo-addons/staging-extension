package org.exoplatform.management.ecmadmin.operations.view;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ObjectParameter;
import org.exoplatform.management.common.AbstractOperationHandler;
import org.exoplatform.services.cms.BasePath;
import org.exoplatform.services.cms.views.ManageViewService;
import org.exoplatform.services.cms.views.TemplateConfig;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.operation.OperationAttributes;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.ExportResourceModel;
import org.gatein.management.api.operation.model.ExportTask;

/**
 * @author <a href="mailto:boubaker.khanfir@exoplatform.com">Boubaker
 *         Khanfir</a>
 * @version $Revision$
 */
public class ViewTemplatesExportResource extends AbstractOperationHandler {

  @Override
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws OperationException {
    try {
      ManageViewService manageViewService = operationContext.getRuntimeContext().getRuntimeComponent(ManageViewService.class);

      OperationAttributes attributes = operationContext.getAttributes();
      List<String> filters = attributes.getValues("filter");

      List<ExportTask> exportTasks = new ArrayList<ExportTask>();

      List<Node> selectedTemplates = null;
      List<Node> templates = manageViewService.getAllTemplates(BasePath.ECM_EXPLORER_TEMPLATES, SessionProvider.createSystemProvider());
      if (filters != null && !filters.isEmpty()) {
        selectedTemplates = new ArrayList<Node>();
        for (Node templateNode : templates) {
          if (filters.contains(templateNode.getName())) {
            selectedTemplates.add(templateNode);
          }
        }
      } else {
        selectedTemplates = templates;
      }

      InitParams initParams = new InitParams();
      for (Node templateNode : selectedTemplates) {
        TemplateConfig templateConfig = new TemplateConfig();
        templateConfig.setName(templateNode.getName());
        templateConfig.setTemplateType("ecmExplorerTemplate");
        templateConfig.setWarPath("/ecm-explorer/" + templateNode.getName() + ".gtmpl");

        ObjectParameter objectParam = new ObjectParameter();
        objectParam.setName(templateConfig.getName());
        objectParam.setObject(templateConfig);
        initParams.addParam(objectParam);
        exportTasks.add(new ViewTemplateExportTask(templateNode));
      }
      exportTasks.add(new ViewConfigurationExportTask(initParams, "views-templates-configuration.xml"));

      resultHandler.completed(new ExportResourceModel(exportTasks));
    } catch (Exception e) {
      throw new OperationException(OperationNames.EXPORT_RESOURCE, "Unable to export View Configurations.", e);
    }
  }
}
