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
package org.exoplatform.management.registry;

import org.exoplatform.management.common.AbstractManagementExtension;
import org.exoplatform.management.registry.operations.ApplicationExportResource;
import org.exoplatform.management.registry.operations.CategoryExportResource;
import org.exoplatform.management.registry.operations.CategoryReadResource;
import org.exoplatform.management.registry.operations.RegistryImportResource;
import org.exoplatform.management.registry.operations.RegistryReadResource;
import org.gatein.management.api.ComponentRegistration;
import org.gatein.management.api.ManagedResource;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.spi.ExtensionContext;

/**
 * The Class ApplicationRegistryManagementExtension.
 *
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class ApplicationRegistryManagementExtension extends AbstractManagementExtension {
  
  /**
   * {@inheritDoc}
   */
  @Override
  public void initialize(ExtensionContext context) {
    ComponentRegistration registration = context.registerManagedComponent("registry");

    ManagedResource.Registration registry = registration.registerManagedResource(description("Application Registry Managed Resource"));
    registry.registerOperationHandler(OperationNames.READ_RESOURCE, new RegistryReadResource(), description("Application Registry Managed Resource."));
    registry.registerOperationHandler(OperationNames.IMPORT_RESOURCE, new RegistryImportResource(), description("Import Application Registry data."));

    // /registry/<category_name>
    ManagedResource.Registration applicationCategory = registry.registerSubResource("{category-name: .*}", description("Application Registry Category"));
    applicationCategory.registerOperationHandler(OperationNames.READ_RESOURCE, new CategoryReadResource(), description("Empty."));
    applicationCategory.registerOperationHandler(OperationNames.EXPORT_RESOURCE, new CategoryExportResource(), description("Export Category declaration in Application Registry."));

    // /registry/<category_name>/<application_name>
    ManagedResource.Registration application = applicationCategory.registerSubResource("{application-name: .*}", description("Management resource responsible for handling management operations on a specific gadget."));
    application.registerOperationHandler(OperationNames.READ_RESOURCE, new EmptyReadResource(), description("Empty list."));
    application.registerOperationHandler(OperationNames.EXPORT_RESOURCE, new ApplicationExportResource(), description("Export Application declaration in Application Registry."));
  }

}
