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
package org.exoplatform.management.organization;

import java.util.HashSet;
import java.util.Set;

import org.exoplatform.management.common.AbstractManagementExtension;
import org.exoplatform.management.common.AbstractOperationHandler;
import org.exoplatform.management.organization.group.GroupExportResource;
import org.exoplatform.management.organization.group.GroupImportResource;
import org.exoplatform.management.organization.group.GroupReadResource;
import org.exoplatform.management.organization.role.RoleExportResource;
import org.exoplatform.management.organization.role.RoleImportResource;
import org.exoplatform.management.organization.role.RoleReadResource;
import org.exoplatform.management.organization.user.UserExportResource;
import org.exoplatform.management.organization.user.UserImportResource;
import org.exoplatform.management.organization.user.UserReadResource;
import org.gatein.management.api.ComponentRegistration;
import org.gatein.management.api.ManagedResource;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.exceptions.ResourceNotFoundException;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.ReadResourceModel;
import org.gatein.management.spi.ExtensionContext;

/**
 * The Class OrganizationManagementExtension.
 *
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class OrganizationManagementExtension extends AbstractManagementExtension {

  /** The Constant PATH_ORGANIZATION. */
  public final static String PATH_ORGANIZATION = "organization";
  
  /** The Constant PATH_ORGANIZATION_USER. */
  public final static String PATH_ORGANIZATION_USER = "user";
  
  /** The Constant PATH_ORGANIZATION_GROUP. */
  public final static String PATH_ORGANIZATION_GROUP = "group";
  
  /** The Constant PATH_ORGANIZATION_ROLE. */
  public final static String PATH_ORGANIZATION_ROLE = "role";
  
  /** The Constant GROUPS_PATH. */
  public static final String GROUPS_PATH = "groupsPath";

  /**
   * {@inheritDoc}
   */
  @Override
  public void initialize(ExtensionContext context) {
    ComponentRegistration organizationRegistration = context.registerManagedComponent(PATH_ORGANIZATION);

    ManagedResource.Registration organization = organizationRegistration.registerManagedResource(description("ECMS (Enterprise Content Management Suite) administration resources."));
    organization.registerOperationHandler(OperationNames.READ_RESOURCE, new OrganizationReadResource(), description("Lists available ECMS administration data"));

    // /organization/user
    ManagedResource.Registration users = organization.registerSubResource(PATH_ORGANIZATION_USER, description("Platform Users."));
    users.registerOperationHandler(OperationNames.READ_RESOURCE, new UserReadResource(), description("Lists available users."));
    users.registerOperationHandler(OperationNames.EXPORT_RESOURCE, new UserExportResource(), description("Exports all users."));
    users.registerOperationHandler(OperationNames.IMPORT_RESOURCE, new UserImportResource(), description("Import users."));

    // /organization/user/<user-name>
    ManagedResource.Registration user = users.registerSubResource("{user-name: .*}", description("User {configuration-name}."));
    user.registerOperationHandler(OperationNames.READ_RESOURCE, new EmptyReadResource(), description("Nothing to read."));
    user.registerOperationHandler(OperationNames.EXPORT_RESOURCE, new UserExportResource(), description("Exports selected views."));

    // /organization/group
    ManagedResource.Registration groups = organization.registerSubResource(PATH_ORGANIZATION_GROUP, description("Groups."));
    groups.registerOperationHandler(OperationNames.READ_RESOURCE, new GroupReadResource(), description("Lists available Groups."));
    groups.registerOperationHandler(OperationNames.EXPORT_RESOURCE, new GroupExportResource(), description("Exports all Groups"));
    groups.registerOperationHandler(OperationNames.IMPORT_RESOURCE, new GroupImportResource(), description("Import Groups."));

    // /organization/group/<group-name>
    ManagedResource.Registration group = groups.registerSubResource("{group-name: .*}", description("Group {group-name}."));
    group.registerOperationHandler(OperationNames.READ_RESOURCE, new EmptyReadResource(), description("Nothing to read."));
    group.registerOperationHandler(OperationNames.EXPORT_RESOURCE, new GroupExportResource(), description("Exports selected Groups"));

    // /organization/role
    ManagedResource.Registration roles = organization.registerSubResource(PATH_ORGANIZATION_ROLE, description("Organization Roles."));
    roles.registerOperationHandler(OperationNames.READ_RESOURCE, new RoleReadResource(), description("Lists available Organization Roles."));
    roles.registerOperationHandler(OperationNames.EXPORT_RESOURCE, new RoleExportResource(), description("Exports all Roles."));
    roles.registerOperationHandler(OperationNames.IMPORT_RESOURCE, new RoleImportResource(), description("Import Organization Roles."));

    // /organization/role/<role-name>
    ManagedResource.Registration role = roles.registerSubResource("{role-name: .*}", description("Organization Role {role-name}."));
    role.registerOperationHandler(OperationNames.READ_RESOURCE, new EmptyReadResource(), description("Nothing to read."));
    role.registerOperationHandler(OperationNames.EXPORT_RESOURCE, new RoleExportResource(), description("Exports selected Organization Role."));
  }

  /**
   * The Class OrganizationReadResource.
   */
  public static final class OrganizationReadResource extends AbstractOperationHandler {
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void execute(OperationContext operationContext, ResultHandler resultHandler) throws ResourceNotFoundException, OperationException {
      Set<String> subResources = new HashSet<String>();
      subResources.add(PATH_ORGANIZATION_USER);
      subResources.add(PATH_ORGANIZATION_GROUP);
      subResources.add(PATH_ORGANIZATION_ROLE);
      resultHandler.completed(new ReadResourceModel("Empty", subResources));
    }
  }

}
