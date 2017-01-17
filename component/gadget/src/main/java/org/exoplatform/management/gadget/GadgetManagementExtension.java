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
package org.exoplatform.management.gadget;

import org.exoplatform.management.common.AbstractManagementExtension;
import org.exoplatform.management.gadget.operations.GadgetExportResource;
import org.exoplatform.management.gadget.operations.GadgetImportResource;
import org.exoplatform.management.gadget.operations.GadgetReadResource;
import org.gatein.management.api.ComponentRegistration;
import org.gatein.management.api.ManagedResource;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.spi.ExtensionContext;

/**
 * The Class GadgetManagementExtension.
 *
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class GadgetManagementExtension extends AbstractManagementExtension {
  
  /**
   * {@inheritDoc}
   */
  @Override
  public void initialize(ExtensionContext context) {
    ComponentRegistration registration = context.registerManagedComponent("gadget");

    ManagedResource.Registration gadgets = registration.registerManagedResource(description("Gadget Managed Resource, responsible for handling management operations Gadgets."));
    gadgets.registerOperationHandler(OperationNames.READ_RESOURCE, new GadgetReadResource(), description("Empty read resources."));
    gadgets.registerOperationHandler(OperationNames.IMPORT_RESOURCE, new GadgetImportResource(), description("Import gadget data"));

    // /gadget/<gadget_name>
    ManagedResource.Registration gadget = gadgets.registerSubResource("{gadget-name: .*}", description("Management resource responsible for handling management operations on a specific gadget."));
    gadget.registerOperationHandler(OperationNames.READ_RESOURCE, new EmptyReadResource(), description("Empty."));
    gadget.registerOperationHandler(OperationNames.EXPORT_RESOURCE, new GadgetExportResource(), description("Export gadget data"));
  }

}
