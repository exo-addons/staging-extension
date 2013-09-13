package org.exoplatform.management.ecmadmin.operations.action;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.jcr.nodetype.NodeType;

import org.exoplatform.services.cms.actions.ActionServiceContainer;
import org.exoplatform.services.jcr.RepositoryService;
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
public class ActionReadResource implements OperationHandler {
  private ActionServiceContainer actionsServiceContainer;
  private RepositoryService repositoryService;

  @Override
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws OperationException {
    if (actionsServiceContainer == null) {
      actionsServiceContainer = operationContext.getRuntimeContext().getRuntimeComponent(ActionServiceContainer.class);
    }
    if (repositoryService == null) {
      repositoryService = operationContext.getRuntimeContext().getRuntimeComponent(RepositoryService.class);
    }
    Set<String> actionNames = new HashSet<String>();
    try {
      Collection<NodeType> nodeTypes = actionsServiceContainer.getCreatedActionTypes(repositoryService.getCurrentRepository()
          .getConfiguration().getName());
      for (NodeType nodeType : nodeTypes) {
        actionNames.add(nodeType.getName());
      }
      resultHandler.completed(new ReadResourceModel("Available actions.", actionNames));
    } catch (Exception e) {
      throw new OperationException(OperationNames.READ_RESOURCE, "Error while retrieving actions.", e);
    }
  }
}