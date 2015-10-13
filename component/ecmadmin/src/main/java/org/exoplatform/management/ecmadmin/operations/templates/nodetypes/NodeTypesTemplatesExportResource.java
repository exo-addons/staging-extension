package org.exoplatform.management.ecmadmin.operations.templates.nodetypes;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;

import org.exoplatform.management.common.AbstractOperationHandler;
import org.exoplatform.management.ecmadmin.exporttask.NodeFileExportTask;
import org.exoplatform.management.ecmadmin.operations.templates.NodeTemplate;
import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.operation.OperationAttributes;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.ExportResourceModel;
import org.gatein.management.api.operation.model.ExportTask;

/**
 * @author <a href="mailto:thomas.delhomenie@exoplatform.com">Thomas
 *         Delhoménie</a>
 * @version $Revision$
 */
public class NodeTypesTemplatesExportResource extends AbstractOperationHandler {

  private static final String EXPORT_BASE_PATH = "ecmadmin/templates/nodetypes";

  private NodeTypeTemplatesMetaData metadata;
  private TemplateService templateService;

  @Override
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws OperationException {

    String operationName = operationContext.getOperationName();
    OperationAttributes attributes = operationContext.getAttributes();
    List<String> filters = attributes.getValues("filter");

    List<ExportTask> exportTasks = new ArrayList<ExportTask>();

    templateService = operationContext.getRuntimeContext().getRuntimeComponent(TemplateService.class);

    try {
      Node templatesHome = templateService.getTemplatesHome(WCMCoreUtils.getSystemSessionProvider());
      if (templatesHome != null) {
        String templatesHomePath = templatesHome.getPath();
        NodeIterator templatesNodes = templatesHome.getNodes();
        while (templatesNodes.hasNext()) {
          Node node = templatesNodes.nextNode();
          if (filters != null && !filters.isEmpty() && !filters.contains(node.getName())) {
            continue;
          }
          metadata = new NodeTypeTemplatesMetaData();
          metadata.setLabel(templateService.getTemplateLabel(node.getName()));
          metadata.setDocumentTemplate(node.getProperty(TemplateService.DOCUMENT_TEMPLATE_PROP).getBoolean());
          metadata.setNodeTypeName(node.getName());

          // View templates
          if (node.hasNode(TemplateService.VIEWS)) {
            Node viewNode = node.getNode(TemplateService.VIEWS);
            exportTemplates(viewNode, TemplateService.VIEWS, templatesHomePath, exportTasks);
          }

          // Dialog templates
          if (node.hasNode(TemplateService.DIALOGS)) {
            Node dialogNode = node.getNode(TemplateService.DIALOGS);
            exportTemplates(dialogNode, TemplateService.DIALOGS, templatesHomePath, exportTasks);
          }

          // Skin templates
          if (node.hasNode(TemplateService.SKINS)) {
            Node skinNode = node.getNode(TemplateService.SKINS);
            exportTemplates(skinNode, TemplateService.SKINS, templatesHomePath, exportTasks);
          }

          exportTasks.add(new NodeTypeTemplatesMetaDataExportTask(metadata, EXPORT_BASE_PATH + "/" + node.getName()));
        }
      } else {
        throw new Exception("Unable to retrieve templates root node");
      }
    } catch (Exception e) {
      throw new OperationException(operationName, "Error while retrieving nodetype templates", e);
    }

    resultHandler.completed(new ExportResourceModel(exportTasks));
  }

  /**
   * Add an export task for each templates of this type
   * 
   * @param exportTasks
   * @param templatesHomePath
   * @param node
   * @param viewNode
   * @throws RepositoryException
   */
  private void exportTemplates(Node parentNode, String type, String templatesHomePath, List<ExportTask> exportTasks)
      throws Exception {
    if (parentNode != null) {
      NodeIterator viewTemplateNodes = parentNode.getNodes();
      while (viewTemplateNodes.hasNext()) {
        Node viewTemplateNode = viewTemplateNodes.nextNode();
        String path = viewTemplateNode.getPath().substring(templatesHomePath.length());
        exportTasks.add(new NodeFileExportTask(viewTemplateNode, EXPORT_BASE_PATH + path + ".gtmpl"));

        metadata.addTemplate(type, new NodeTemplate(path + ".gtmpl", templateService.getTemplateRoles(viewTemplateNode)));
      }
    }
  }
}