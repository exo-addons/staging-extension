package org.exoplatform.management.ecmadmin.operations.queries;

import java.util.Set;

import org.exoplatform.services.cms.queries.QueryService;
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
public class QueriesReadResource implements OperationHandler {
  
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws OperationException {
    QueryService queryService = operationContext.getRuntimeContext().getRuntimeComponent(QueryService.class);

    Set<String> allConfiguredQueries = queryService.getAllConfiguredQueries();

    resultHandler.completed(new ReadResourceModel("Available queries.", allConfiguredQueries));
  }
}