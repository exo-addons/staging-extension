package org.exoplatform.management.organization.role;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.exoplatform.management.common.AbstractOperationHandler;
import org.exoplatform.management.organization.OrganizationModelExportTask;
import org.exoplatform.services.organization.MembershipType;
import org.exoplatform.services.organization.OrganizationService;
import org.gatein.management.api.PathAddress;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.ExportResourceModel;
import org.gatein.management.api.operation.model.ExportTask;

/**
 * @author <a href="mailto:boubaker.khanfir@exoplatform.com">Boubaker
 *         Khanfir</a>
 * @version $Revision$
 */
public class RoleExportResource extends AbstractOperationHandler {

  private OrganizationService organizationService = null;

  @Override
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws OperationException {
    List<ExportTask> exportTasks = new ArrayList<ExportTask>();
    try {
      if (organizationService == null) {
        organizationService = operationContext.getRuntimeContext().getRuntimeComponent(OrganizationService.class);
      }

      PathAddress address = operationContext.getAddress();
      String roleName = address.resolvePathTemplate("role-name");

      // If only one role selected
      if (roleName != null && !roleName.trim().isEmpty()) {
        MembershipType membershipType = organizationService.getMembershipTypeHandler().findMembershipType(roleName);
        if (membershipType == null) {
          throw new OperationException(OperationNames.EXPORT_RESOURCE, "membershipType with name '" + roleName + "' doesn't exist.");
        }
        exportRole(membershipType, exportTasks);
      } else {
        // If all roles will be exported
        Collection<MembershipType> roles = organizationService.getMembershipTypeHandler().findMembershipTypes();
        for (MembershipType role : roles) {
          exportRole(role, exportTasks);
        }
      }
      resultHandler.completed(new ExportResourceModel(exportTasks));
    } catch (Exception e) {
      throw new OperationException(OperationNames.EXPORT_RESOURCE, "Unable to export membershipType : " + e.getMessage());
    }
  }

  private void exportRole(MembershipType membershipType, List<ExportTask> exportTasks) throws Exception {
    exportTasks.add(new OrganizationModelExportTask(membershipType));
  }
}
