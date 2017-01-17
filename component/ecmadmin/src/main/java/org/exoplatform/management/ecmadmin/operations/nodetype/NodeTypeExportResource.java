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
import org.gatein.management.api.operation.OperationAttributes;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.ExportResourceModel;
import org.gatein.management.api.operation.model.ExportTask;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.NodeTypeManager;

/**
 * The Class NodeTypeExportResource.
 *
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class NodeTypeExportResource extends AbstractOperationHandler {

  /** The repository service. */
  private RepositoryService repositoryService;

  /**
   * {@inheritDoc}
   */
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