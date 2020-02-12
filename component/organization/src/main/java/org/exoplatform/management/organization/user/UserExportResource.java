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
package org.exoplatform.management.organization.user;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.jcr.Node;

import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.management.common.exportop.AbstractJCRExportOperationHandler;
import org.exoplatform.management.common.exportop.ActivitiesExportTask;
import org.exoplatform.management.common.exportop.JCRNodeExportTask;
import org.exoplatform.management.organization.OrganizationManagementExtension;
import org.exoplatform.management.organization.OrganizationModelExportTask;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserProfile;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.manager.ActivityManager;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.storage.api.IdentityStorage;
import org.gatein.management.api.PathAddress;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.operation.OperationAttributes;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.ExportResourceModel;
import org.gatein.management.api.operation.model.ExportTask;

/**
 * The Class UserExportResource.
 *
 * @author <a href="mailto:boubaker.khanfir@exoplatform.com">Boubaker
 *         Khanfir</a>
 * @version $Revision$
 */
public class UserExportResource extends AbstractJCRExportOperationHandler {
  
  /** The organization service. */
  private OrganizationService organizationService = null;
  
  /** The hierarchy creator. */
  private NodeHierarchyCreator hierarchyCreator = null;

  /**
   * {@inheritDoc}
   */
  @Override
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws OperationException {
    List<ExportTask> exportTasks = new ArrayList<ExportTask>();

    increaseCurrentTransactionTimeOut(operationContext);
    try {
      if (organizationService == null) {
        organizationService = operationContext.getRuntimeContext().getRuntimeComponent(OrganizationService.class);
        repositoryService = operationContext.getRuntimeContext().getRuntimeComponent(RepositoryService.class);
        hierarchyCreator = operationContext.getRuntimeContext().getRuntimeComponent(NodeHierarchyCreator.class);
      }
      if(identityStorage == null){
        identityStorage = operationContext.getRuntimeContext().getRuntimeComponent(IdentityStorage.class);
      }
      if(activityManager == null){
        activityManager = operationContext.getRuntimeContext().getRuntimeComponent(ActivityManager.class);
      }
      if(identityManager == null) {
        identityManager = operationContext.getRuntimeContext().getRuntimeComponent(IdentityManager.class);
      }

      PathAddress address = operationContext.getAddress();
      String userName = address.resolvePathTemplate("user-name");

      OperationAttributes attributes = operationContext.getAttributes();
      List<String> filters = attributes.getValues("filter");

      // "with-membership" attribute. Defaults to false.
      boolean withContent = filters.contains("with-jcr-content:true");
      // "with-membership" attribute. Defaults to true.
      boolean withMemberships = !filters.contains("with-membership:false");
      // "with-activities" attribute. Defaults to true.
      boolean withActivities = !filters.contains("with-activities:false");

      // "new-password" attribute. Defaults to null (means : don't change
      // passwords).
      String newPassword = null;
      for (String filter : attributes.getValues("filter")) {
        if (filter.startsWith("new-password:")) {
          newPassword = filter.substring("new-password:".length());
          break;
        }
      }

      if (userName != null && !userName.trim().isEmpty()) {
        User user = organizationService.getUserHandler().findUserByName(userName);
        if (user == null) {
          throw new OperationException(OperationNames.EXPORT_RESOURCE, "User with name '" + userName + "' doesn't exist");
        }
        user.setPassword(newPassword);
        exportUser(user, withContent, withMemberships, withActivities, exportTasks);
      } else {
        ListAccess<User> allUsers = organizationService.getUserHandler().findAllUsers();
        int size = allUsers.getSize();
        int pageSize = 10;
        int i = 0;
        while (i < size) {
          int length = (size - i >= pageSize) ? pageSize : size - i;
          User[] users = allUsers.load(i, length);
          for (User user : users) {
            user.setPassword(newPassword);
            exportUser(user, withContent, withMemberships, withActivities, exportTasks);
          }
          i += pageSize;
        }
      }

      resultHandler.completed(new ExportResourceModel(exportTasks));
    } catch (Exception e) {
      throw new OperationException(OperationNames.EXPORT_RESOURCE, "Unable to export User : " + e.getMessage());
    } finally {
      restoreDefaultTransactionTimeOut(operationContext);
    }
  }

  /**
   * Export user.
   *
   * @param user the user
   * @param withContent the with content
   * @param withMemberships the with memberships
   * @param withActivities
   * @param exportTasks the export tasks
   * @throws Exception the exception
   */
  private void exportUser(User user, boolean withContent, boolean withMemberships, boolean withActivities, List<ExportTask> exportTasks) throws Exception {
    if(withActivities) {
      String prefix = OrganizationManagementExtension.PATH_ORGANIZATION + "/" + OrganizationManagementExtension.PATH_ORGANIZATION_USER + "/" + user.getUserName() + "/";
      exportActivities(exportTasks, user.getUserName(), prefix);
    }
    exportUser(user, withContent, withMemberships, exportTasks);
  }
  /**
   * Export user.
   *
   * @param user the user
   * @param withContent the with content
   * @param withMemberships the with memberships
   * @param exportTasks the export tasks
   * @throws Exception the exception
   */
  private void exportUser(User user, boolean withContent, boolean withMemberships, List<ExportTask> exportTasks) throws Exception {
    UserProfile profile = organizationService.getUserProfileHandler().findUserProfileByName(user.getUserName());
    exportTasks.add(new OrganizationModelExportTask(user));
    if (profile != null) {
      exportTasks.add(new OrganizationModelExportTask(profile));
    }
    if (withMemberships) {
      Collection<?> memberships = organizationService.getMembershipHandler().findMembershipsByUser(user.getUserName());
      for (Object membership : memberships) {
        exportTasks.add(new OrganizationModelExportTask(membership));
      }
    }
    if (withContent) {
      SessionProvider provider = SessionProvider.createSystemProvider();
      Node userNode = hierarchyCreator.getUserNode(provider, user.getUserName());
      String defaultWorkspace = userNode.getSession().getWorkspace().getName();
      exportNode(userNode, exportTasks, new String[] { defaultWorkspace, user.getUserName() });
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void addJCRNodeExportTask(Node childNode, List<ExportTask> subNodesExportTask, boolean recursive, String... params) throws Exception {
    String prefixInZiipFile = OrganizationManagementExtension.PATH_ORGANIZATION + "/" + OrganizationManagementExtension.PATH_ORGANIZATION_USER + "/" + params[1];
    subNodesExportTask.add(new JCRNodeExportTask(repositoryService, params[0], childNode.getPath(), prefixInZiipFile, recursive, true));
  }
}
