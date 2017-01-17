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
package org.exoplatform.management.gadget.operations;

import org.exoplatform.application.gadget.Gadget;
import org.exoplatform.application.gadget.GadgetRegistryService;
import org.exoplatform.management.common.AbstractOperationHandler;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.ReadResourceModel;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The Class GadgetReadResource.
 *
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class GadgetReadResource extends AbstractOperationHandler {
  
  /** The gadget registry service. */
  private GadgetRegistryService gadgetRegistryService;

  /**
   * {@inheritDoc}
   */
  @Override
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws OperationException {
    if (gadgetRegistryService == null) {
      gadgetRegistryService = operationContext.getRuntimeContext().getRuntimeComponent(GadgetRegistryService.class);
      if (gadgetRegistryService == null) {
        throw new OperationException(OperationNames.EXPORT_RESOURCE, "Cannot get GadgetRegistryService instance");
      }
    }
    Set<String> result = new HashSet<String>();
    try {
      List<Gadget> gadgets = gadgetRegistryService.getAllGadgets();
      for (Gadget gadget : gadgets) {
        result.add(gadget.getName());
      }
    } catch (Exception e) {
      throw new OperationException(OperationNames.READ_RESOURCE, "Error while retrieving gadget list", e);
    }

    resultHandler.completed(new ReadResourceModel("Available gadgets", result));
  }
}
