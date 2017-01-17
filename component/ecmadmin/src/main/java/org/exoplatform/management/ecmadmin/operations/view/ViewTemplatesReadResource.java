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
package org.exoplatform.management.ecmadmin.operations.view;

import org.exoplatform.management.common.AbstractOperationHandler;
import org.exoplatform.services.cms.BasePath;
import org.exoplatform.services.cms.views.ManageViewService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.gatein.common.logging.Logger;
import org.gatein.common.logging.LoggerFactory;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.ReadResourceModel;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.jcr.Node;

/**
 * The Class ViewTemplatesReadResource.
 *
 * @author <a href="mailto:boubaker.khanfir@exoplatform.com">Boubaker
 *         Khanfir</a>
 * @version $Revision$
 */
public class ViewTemplatesReadResource extends AbstractOperationHandler {
  
  /** The Constant log. */
  final private static Logger log = LoggerFactory.getLogger(ViewTemplatesReadResource.class);

  /**
   * {@inheritDoc}
   */
  @Override
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws OperationException {
    ManageViewService manageViewService = operationContext.getRuntimeContext().getRuntimeComponent(ManageViewService.class);

    try {
      List<Node> nodes = manageViewService.getAllTemplates(BasePath.ECM_EXPLORER_TEMPLATES, SessionProvider.createSystemProvider());
      Set<String> viewConfigurations = new HashSet<String>();
      for (Node node : nodes) {
        viewConfigurations.add(node.getName());
      }

      resultHandler.completed(new ReadResourceModel("ECMS Explorer View Templates.", viewConfigurations));
    } catch (Exception e) {
      log.error("Error occured while retrieving Sites Explorer View Templates", e);
    }
  }
}