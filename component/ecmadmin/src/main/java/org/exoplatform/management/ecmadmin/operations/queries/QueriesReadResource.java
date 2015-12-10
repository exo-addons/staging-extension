package org.exoplatform.management.ecmadmin.operations.queries;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.query.Query;

import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.management.common.AbstractOperationHandler;
import org.exoplatform.services.cms.queries.QueryService;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.ReadResourceModel;

/**
 * @author <a href="mailto:thomas.delhomenie@exoplatform.com">Thomas
 *         Delhoménie</a>
 * @version $Revision$
 */
public class QueriesReadResource extends AbstractOperationHandler {
  @Override
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws OperationException {
    QueryService queryService = operationContext.getRuntimeContext().getRuntimeComponent(QueryService.class);
    OrganizationService organizationService = operationContext.getRuntimeContext().getRuntimeComponent(OrganizationService.class);

    Set<String> allQueries = new HashSet<String>();

    try {
      // shared queries
      List<Node> sharedQueries = queryService.getSharedQueries(WCMCoreUtils.getSystemSessionProvider());
      for (Node sharedQuery : sharedQueries) {
        allQueries.add(sharedQuery.getName());
      }

      // users queries
      ListAccess<User> usersListAccess = organizationService.getUserHandler().findAllUsers();
      User[] users = usersListAccess.load(0, usersListAccess.getSize());
      for (User user : users) {
        List<Query> userQueries = queryService.getQueries(user.getUserName(), WCMCoreUtils.getSystemSessionProvider());
        for (Query userQuery : userQueries) {
          String queryPath = userQuery.getStoredQueryPath();
          String queryName = queryPath.substring(queryPath.lastIndexOf("/") + 1);

          allQueries.add(user.getUserName() + "/" + queryName);
        }
      }
    } catch (Exception e) {
      throw new OperationException(OperationNames.READ_RESOURCE, "Error while retrieving ECMS queries", e);
    }

    resultHandler.completed(new ReadResourceModel("Available queries", allQueries));
  }
}