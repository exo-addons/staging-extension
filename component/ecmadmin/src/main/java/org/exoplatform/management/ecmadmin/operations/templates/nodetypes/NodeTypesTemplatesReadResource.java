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
package org.exoplatform.management.ecmadmin.operations.templates.nodetypes;

import org.exoplatform.management.common.AbstractOperationHandler;
import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.ReadResourceModel;

import java.util.HashSet;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;

/**
 * The Class NodeTypesTemplatesReadResource.
 *
 * @author <a href="mailto:thomas.delhomenie@exoplatform.com">Thomas
 *         Delhoménie</a>
 * @version $Revision$
 */
public class NodeTypesTemplatesReadResource extends AbstractOperationHandler {
  
  /**
   * {@inheritDoc}
   */
  @Override
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws OperationException {
    Set<String> nodeTypesTemplates = new HashSet<String>();

    TemplateService templateService = operationContext.getRuntimeContext().getRuntimeComponent(TemplateService.class);

    try {
      Node templatesHome = templateService.getTemplatesHome(WCMCoreUtils.getSystemSessionProvider());
      if (templatesHome != null) {
        NodeIterator templatesNodes = templatesHome.getNodes();
        while (templatesNodes.hasNext()) {
          Node node = templatesNodes.nextNode();
          nodeTypesTemplates.add(node.getName());
        }
      } else {
        throw new Exception("Unable to retrieve templates root node");
      }
    } catch (Exception e) {
      throw new OperationException(OperationNames.READ_RESOURCE, "Error while retrieving nodetype templates", e);
    }

    resultHandler.completed(new ReadResourceModel("Available nodetype templates", nodeTypesTemplates));
  }
}