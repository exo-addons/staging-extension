package org.exoplatform.management.organization.group;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.OrganizationService;
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
public class GroupReadResource implements OperationHandler {
  final private static Logger log = LoggerFactory.getLogger(GroupReadResource.class);

  @Override
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws OperationException {
    Set<String> groupIds = new HashSet<String>();

    OrganizationService organizationService = operationContext.getRuntimeContext().getRuntimeComponent(OrganizationService.class);
    try {
      Collection<?> groups = organizationService.getGroupHandler().getAllGroups();
      for (Object object : groups) {
        groupIds.add(((Group) object).getId());
      }
      resultHandler.completed(new ReadResourceModel("List of all groups.", groupIds));
    } catch (Exception e) {
      log.error("Error occured while reading list of groups.", e);
    }
  }
}