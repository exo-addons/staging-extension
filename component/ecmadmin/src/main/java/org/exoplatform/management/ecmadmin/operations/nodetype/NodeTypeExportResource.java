package org.exoplatform.management.ecmadmin.operations.nodetype;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.NodeTypeManager;

import org.exoplatform.management.common.AbstractOperationHandler;
import org.exoplatform.services.jcr.RepositoryService;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.operation.OperationAttributes;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.ExportResourceModel;
import org.gatein.management.api.operation.model.ExportTask;

/**
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class NodeTypeExportResource extends AbstractOperationHandler {

  private RepositoryService repositoryService;

  @Override
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws OperationException {
    OperationAttributes attributes = operationContext.getAttributes();
    List<String> filters = attributes.getValues("filter");
    if (repositoryService == null) {
      repositoryService = operationContext.getRuntimeContext().getRuntimeComponent(RepositoryService.class);
    }
    List<ExportTask> exportTasks = new ArrayList<ExportTask>();
    try {
      NodeTypeManager ntManager = repositoryService.getCurrentRepository().getNodeTypeManager();
      NodeTypeIterator nodeTypesIterator = ntManager.getAllNodeTypes();
      while (nodeTypesIterator.hasNext()) {
        NodeType nodeType = nodeTypesIterator.nextNodeType();
        if (filters.isEmpty() || filters.contains(nodeType.getName())) {
          exportTasks.add(new NodeTypeExportTask(nodeType, "nodetype"));
        }
      }
      exportTasks.add(new NamespacesExportTask(repositoryService.getCurrentRepository().getNamespaceRegistry(), "nodetype"));
    } catch (Exception exception) {
      throw new OperationException(OperationNames.EXPORT_RESOURCE, "Error while retrieving nodetypes", exception);
    }
    resultHandler.completed(new ExportResourceModel(exportTasks));
  }

}