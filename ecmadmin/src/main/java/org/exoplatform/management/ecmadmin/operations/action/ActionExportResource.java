package org.exoplatform.management.ecmadmin.operations.action;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.jcr.nodetype.NodeType;

import org.exoplatform.management.ecmadmin.operations.nodetype.NodeTypeExportTask;
import org.exoplatform.services.cms.actions.ActionServiceContainer;
import org.exoplatform.services.jcr.RepositoryService;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.operation.OperationAttributes;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.OperationHandler;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.ExportResourceModel;
import org.gatein.management.api.operation.model.ExportTask;

/**
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class ActionExportResource implements OperationHandler {

  private ActionServiceContainer actionsServiceContainer;
  private RepositoryService repositoryService;

  @Override
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws OperationException {
    OperationAttributes attributes = operationContext.getAttributes();
    List<String> filters = attributes.getValues("filter");
    if (actionsServiceContainer == null) {
      actionsServiceContainer = operationContext.getRuntimeContext().getRuntimeComponent(ActionServiceContainer.class);
    }
    if (repositoryService == null) {
      repositoryService = operationContext.getRuntimeContext().getRuntimeComponent(RepositoryService.class);
    }
    List<ExportTask> exportTasks = new ArrayList<ExportTask>();
    try {
      Collection<NodeType> nodeTypes = actionsServiceContainer.getCreatedActionTypes(repositoryService.getCurrentRepository()
          .getConfiguration().getName());
      for (NodeType nodeType : nodeTypes) {
        if (filters.isEmpty() || filters.contains(nodeType.getName())) {
          exportTasks.add(new NodeTypeExportTask(nodeType, "action"));
        }
      }
    } catch (Exception exception) {
      throw new OperationException(OperationNames.EXPORT_RESOURCE, "Error while retrieving actions: ", exception);
    }
    resultHandler.completed(new ExportResourceModel(exportTasks));
  }

}