/*
 * Copyright (C) 2003-2017 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
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
 * The Class RoleExportResource.
 *
 * @author <a href="mailto:boubaker.khanfir@exoplatform.com">Boubaker
 *         Khanfir</a>
 * @version $Revision$
 */
public class RoleExportResource extends AbstractOperationHandler {

  /** The organization service. */
  private OrganizationService organizationService = null;

  /**
   * {@inheritDoc}
   */
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

  /**
   * Export role.
   *
   * @param membershipType the membership type
   * @param exportTasks the export tasks
   * @throws Exception the exception
   */
  private void exportRole(MembershipType membershipType, List<ExportTask> exportTasks) throws Exception {
    exportTasks.add(new OrganizationModelExportTask(membershipType));
  }
}
