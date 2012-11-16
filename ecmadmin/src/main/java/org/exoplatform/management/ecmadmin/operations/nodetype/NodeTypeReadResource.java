package org.exoplatform.management.ecmadmin.operations.nodetype;

import java.util.HashSet;
import java.util.Set;

import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.NodeTypeManager;

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
public class NodeTypeReadResource implements OperationHandler {
  private RepositoryService repositoryService;

  @Override
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws OperationException {
    if (repositoryService == null) {
      repositoryService = operationContext.getRuntimeContext().getRuntimeComponent(RepositoryService.class);
    }
    Set<String> nodeTypeNames = new HashSet<String>();
    try {
      NodeTypeManager ntManager = repositoryService.getCurrentRepository().getNodeTypeManager();
      NodeTypeIterator nodeTypesIterator = ntManager.getAllNodeTypes();
      while (nodeTypesIterator.hasNext()) {
        NodeType nodeType = nodeTypesIterator.nextNodeType();
        nodeTypeNames.add(nodeType.getName());
      }
      resultHandler.completed(new ReadResourceModel("Available nodetypes.", nodeTypeNames));
    } catch (Exception e) {
      throw new OperationException(OperationNames.READ_RESOURCE, "Error while retrieving nodetypes.", e);
    }
  }
}