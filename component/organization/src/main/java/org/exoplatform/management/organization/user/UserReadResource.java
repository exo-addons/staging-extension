package org.exoplatform.management.organization.user;

import java.util.HashSet;
import java.util.Set;

import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.gatein.common.logging.Logger;
import org.gatein.common.logging.LoggerFactory;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.OperationHandler;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.ReadResourceModel;

/**
 * @author <a href="mailto:boubaker.khanfir@exoplatform.com">Boubaker
 *         Khanfir</a>
 * @version $Revision$
 */
public class UserReadResource implements OperationHandler {
  final private static Logger log = LoggerFactory.getLogger(UserReadResource.class);

  @Override
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws OperationException {
    Set<String> userNames = new HashSet<String>();

    OrganizationService organizationService = operationContext.getRuntimeContext().getRuntimeComponent(OrganizationService.class);
    try {
      ListAccess<User> allUsers = organizationService.getUserHandler().findAllUsers();
      int size = allUsers.getSize();
      int pageSize = 10;
      int i = 0;
      while (i < size) {
        int length = (size - i >= pageSize) ? pageSize : size - i;
        User[] users = allUsers.load(0, length);
        for (User user : users) {
          userNames.add(user.getUserName());
        }
        i += pageSize;
      }
      resultHandler.completed(new ReadResourceModel("List of all users.", userNames));
    } catch (Exception e) {
      log.error("Error occured while reading list of users.", e);
    }
  }
}