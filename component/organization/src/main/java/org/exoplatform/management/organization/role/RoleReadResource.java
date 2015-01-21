package org.exoplatform.management.organization.role;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.exoplatform.management.common.AbstractOperationHandler;
import org.exoplatform.services.organization.MembershipType;
import org.exoplatform.services.organization.OrganizationService;
import org.gatein.common.logging.Logger;
import org.gatein.common.logging.LoggerFactory;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.ReadResourceModel;

/**
 * @author <a href="mailto:boubaker.khanfir@exoplatform.com">Boubaker
 *         Khanfir</a>
 * @version $Revision$
 */
public class RoleReadResource extends AbstractOperationHandler {
  final private static Logger log = LoggerFactory.getLogger(RoleReadResource.class);

  @Override
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws OperationException {
    Set<String> memberships = new HashSet<String>();

    OrganizationService organizationService = operationContext.getRuntimeContext().getRuntimeComponent(OrganizationService.class);
    try {
      Collection<?> membershipTypes = organizationService.getMembershipTypeHandler().findMembershipTypes();
      for (Object object : membershipTypes) {
        memberships.add(((MembershipType) object).getName());
      }
      resultHandler.completed(new ReadResourceModel("List of all roles.", memberships));
    } catch (Exception e) {
      log.error("Error occured while reading list of roles.", e);
    }
  }
}