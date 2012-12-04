package org.exoplatform.management.packaging;

import org.exoplatform.management.packaging.operations.PackagingReadResource;
import org.exoplatform.management.packaging.operations.ExtensionExportResource;
import org.exoplatform.management.packaging.operations.ExtensionReadResource;
import org.gatein.management.api.ComponentRegistration;
import org.gatein.management.api.ManagedDescription;
import org.gatein.management.api.ManagedResource;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.spi.ExtensionContext;
import org.gatein.management.spi.ManagementExtension;

/**
 * @author <a href="mailto:thomas.delhomenie@exoplatform.com">Thomas Delhoménie</a>
 * @version $Revision$
 */
public class PackagingManagementExtension implements ManagementExtension
{
   @Override
   public void initialize(ExtensionContext context)
   {
	   	ComponentRegistration registration = context.registerManagedComponent("package");

	   	ManagedResource.Registration packageExtension = registration.registerManagedResource(description("platform extension"));

       packageExtension.registerOperationHandler(OperationNames.READ_RESOURCE, new PackagingReadResource(), description("Hello World :-)"));

        ManagedResource.Registration extension = packageExtension.registerSubResource("extension", description("responsible for generating the export of the extension"));
       extension.registerOperationHandler(OperationNames.READ_RESOURCE, new ExtensionReadResource(), description("extension"));
       extension.registerOperationHandler(OperationNames.EXPORT_RESOURCE, new ExtensionExportResource(), description("export of the extension"));
   }

   @Override
   public void destroy()
   {
   }

   private static ManagedDescription description(final String description)
   {
      return new ManagedDescription()
      {
         @Override
         public String getDescription()
         {
            return description;
         }
      };
   }
}
