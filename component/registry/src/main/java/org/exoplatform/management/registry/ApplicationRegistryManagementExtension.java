package org.exoplatform.management.registry;

import java.util.HashSet;

import org.exoplatform.management.common.AbstractManagementExtension;
import org.exoplatform.management.common.AbstractOperationHandler;
import org.exoplatform.management.registry.operations.ApplicationExportResource;
import org.exoplatform.management.registry.operations.CategoryExportResource;
import org.exoplatform.management.registry.operations.CategoryReadResource;
import org.exoplatform.management.registry.operations.RegistryImportResource;
import org.exoplatform.management.registry.operations.RegistryReadResource;
import org.gatein.management.api.ComponentRegistration;
import org.gatein.management.api.ManagedDescription;
import org.gatein.management.api.ManagedResource;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.exceptions.ResourceNotFoundException;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.ReadResourceModel;
import org.gatein.management.spi.ExtensionContext;

/**
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class ApplicationRegistryManagementExtension extends AbstractManagementExtension {
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
