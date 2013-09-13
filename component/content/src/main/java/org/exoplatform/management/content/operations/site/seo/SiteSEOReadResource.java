package org.exoplatform.management.content.operations.site.seo;

import java.util.HashSet;

import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.OperationHandler;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.ReadResourceModel;

/**
 * @author <a href="mailto:thomas.delhomenie@exoplatform.com">Thomas
 *         Delhoménie</a>
 * @version $Revision$
 */
public class SiteSEOReadResource implements OperationHandler {
  @Override
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws OperationException {
    resultHandler.completed(new ReadResourceModel("Available sites.", new HashSet<String>()));
  }
}