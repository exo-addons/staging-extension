package org.exoplatform.management.organization.user;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.jcr.Node;

import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.management.common.AbstractOperationHandler;
import org.exoplatform.management.organization.OrganizationModelExportTask;
import org.exoplatform.management.organization.OrganizationModelJCRContentExportTask;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserProfile;
import org.gatein.management.api.PathAddress;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.operation.OperationAttributes;
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
public class UserExportResource extends AbstractOperationHandler {
  private OrganizationService organizationService = null;
  private RepositoryService repositoryService = null;
  private NodeHierarchyCreator hierarchyCreator = null;

  @Override
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws OperationException {
    List<ExportTask> exportTasks = new ArrayList<ExportTask>();
    try {
      if (organizationService == null) {
        organizationService = operationContext.getRuntimeContext().getRuntimeComponent(OrganizationService.class);
      }
      if (repositoryService == null) {
        repositoryService = operationContext.getRuntimeContext().getRuntimeComponent(RepositoryService.class);
      }
      if (hierarchyCreator == null) {
        hierarchyCreator = operationContext.getRuntimeContext().getRuntimeComponent(NodeHierarchyCreator.class);
      }

      PathAddress address = operationContext.getAddress();
      String userName = address.resolvePathTemplate("user-name");

      OperationAttributes attributes = operationContext.getAttributes();
      List<String> filters = attributes.getValues("filter");

      // "with-membership" attribute. Defaults to false.
      boolean withContent = filters.contains("with-jcr-content:true");
      // "with-membership" attribute. Defaults to true.
      boolean withMemberships = !filters.contains("with-membership:false");

      // "new-password" attribute. Defaults to null (means : don't change passwords).
      String newPassword = null;
      for(String filter : attributes.getValues("filter")) {
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
        exportUser(user, withContent, withMemberships, exportTasks);
      } else {
        ListAccess<User> allUsers = organizationService.getUserHandler().findAllUsers();
        int size = allUsers.getSize();
        int pageSize = 10;
        int i = 0;
        while (i < size) {
          int length = (size - i >= pageSize) ? pageSize : size - i;
          User[] users = allUsers.load(0, length);
          for (User user : users) {
            user.setPassword(newPassword);
            exportUser(user, withContent, withMemberships, exportTasks);
          }
          i += pageSize;
        }
      }

      resultHandler.completed(new ExportResourceModel(exportTasks));
    } catch (Exception e) {
      throw new OperationException(OperationNames.EXPORT_RESOURCE, "Unable to export User : " + e.getMessage());
    }
  }

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
      exportTasks.add(new OrganizationModelJCRContentExportTask(repositoryService, userNode, user));
    }
  }
}
