package org.exoplatform.management.idm;

import java.util.HashSet;

import org.exoplatform.management.idm.operations.IDMExportResource;
import org.exoplatform.management.idm.operations.IDMImportResource;
import org.gatein.management.api.ComponentRegistration;
import org.gatein.management.api.ManagedDescription;
import org.gatein.management.api.ManagedResource;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.exceptions.ResourceNotFoundException;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.OperationHandler;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.ReadResourceModel;
import org.gatein.management.spi.ExtensionContext;
import org.gatein.management.spi.ManagementExtension;

/**
 * @author <a href="mailto:thomas.delhomenie@exoplatform.com">Thomas
 *         Delhoménie</a>
 * @version $Revision$
 */
public class IDMManagementExtension implements ManagementExtension {
  @Override
  public void initialize(ExtensionContext context) {
    ComponentRegistration registration = context.registerManagedComponent("idm");

    ManagedResource.Registration idm = registration
        .registerManagedResource(description("IDM (Model Object for Portal) Managed Resource, responsible for handling management operations on users, groups, membership types and memberships."));
    idm.registerOperationHandler(OperationNames.READ_RESOURCE, new EmptyReadResource(), description("Empty."));

    ManagedResource.Registration operations = idm.registerSubResource("operations",
        description("Workaround : Export Resource can't be added to parent operation handler."));

    operations.registerOperationHandler(OperationNames.READ_RESOURCE, new EmptyReadResource(), description("Empty."));
    operations.registerOperationHandler(OperationNames.EXPORT_RESOURCE, new IDMExportResource(),
        description("Export IDM resources."), false);
    operations.registerOperationHandler(OperationNames.IMPORT_RESOURCE, new IDMImportResource(),
        description("Import organization data"));

  }

  @Override
  public void destroy() {}

  private static ManagedDescription description(final String description) {
    return new ManagedDescription() {
      @Override
      public String getDescription() {
        return description;
      }
    };
  }

  public static class EmptyReadResource implements OperationHandler {
    @Override
    public void execute(OperationContext operationContext, ResultHandler resultHandler) throws ResourceNotFoundException,
        OperationException {
      resultHandler.completed(new ReadResourceModel("Empty", new HashSet<String>()));
    }
  }

}
