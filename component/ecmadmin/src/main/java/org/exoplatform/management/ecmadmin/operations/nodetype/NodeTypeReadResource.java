/*
 * Copyright (C) 2003-2017 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.exoplatform.management.ecmadmin.operations.nodetype;

import org.exoplatform.management.common.AbstractOperationHandler;
import org.exoplatform.services.jcr.RepositoryService;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.ReadResourceModel;

import java.util.HashSet;
import java.util.Set;

import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.NodeTypeManager;

/**
 * The Class NodeTypeReadResource.
 *
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class NodeTypeReadResource extends AbstractOperationHandler {
  
  /** The repository service. */
  private RepositoryService repositoryService;

  /**
   * {@inheritDoc}
   */
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