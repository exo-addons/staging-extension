package org.exoplatform.management.packaging.operations;

import java.util.HashSet;
import java.util.Set;

import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.OperationHandler;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.ReadResourceModel;

/**
 * @author <a href="mailto:thomas.delhomenie@exoplatform.com">Thomas Delhoménie</a>
 * @version $Revision$
 */
public class PackagingReadResource implements OperationHandler
{
   
   public void execute(OperationContext operationContext, ResultHandler resultHandler) throws OperationException
   {
	   try {
		   Set<String> display = new HashSet<String>();
		   display.add("extension");
		   resultHandler.completed(new ReadResourceModel("packaging", display));
	   } catch(Exception e) {
		   throw new OperationException(OperationNames.READ_RESOURCE, "Unable to retrieve packaging : " + e.getMessage());
	   }
   }
}
