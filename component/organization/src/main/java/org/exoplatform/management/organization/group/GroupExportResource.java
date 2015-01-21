package org.exoplatform.management.organization.group;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Session;

import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.management.common.AbstractOperationHandler;
import org.exoplatform.management.organization.OrganizationModelExportTask;
import org.exoplatform.management.organization.OrganizationModelJCRContentExportTask;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.distribution.DataDistributionManager;
import org.exoplatform.services.jcr.ext.distribution.DataDistributionMode;
import org.exoplatform.services.jcr.ext.distribution.DataDistributionType;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.Membership;
import org.exoplatform.services.organization.OrganizationService;
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
public class GroupExportResource extends AbstractOperationHandler {

  private OrganizationService organizationService = null;
  private RepositoryService repositoryService = null;
  private NodeHierarchyCreator hierarchyCreator = null;
  private DataDistributionManager dataDistributionManager = null;
  private DataDistributionType dataDistributionType = null;
  private String groupsPath = null;

  @Override
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws OperationException {
    List<ExportTask> exportTasks = new ArrayList<ExportTask>();
    try {
      if (organizationService == null) {
        organizationService = operationContext.getRuntimeContext().getRuntimeComponent(OrganizationService.class);
        repositoryService = operationContext.getRuntimeContext().getRuntimeComponent(RepositoryService.class);
        hierarchyCreator = operationContext.getRuntimeContext().getRuntimeComponent(NodeHierarchyCreator.class);
        dataDistributionManager = operationContext.getRuntimeContext().getRuntimeComponent(DataDistributionManager.class);
        dataDistributionType = dataDistributionManager.getDataDistributionType(DataDistributionMode.NONE);
        groupsPath = hierarchyCreator.getJcrPath(OrganizationModelJCRContentExportTask.GROUPS_PATH);
      }

      PathAddress address = operationContext.getAddress();
      String groupId = address.resolvePathTemplate("group-name");

      OperationAttributes attributes = operationContext.getAttributes();
      List<String> filters = attributes.getValues("filter");

      boolean withContent = filters.contains("with-jcr-content:true");
      boolean withMemberships = filters.contains("with-membership:true");

      String systemWorkspace = null;
      SessionProvider sessionProvider = null;
      ManageableRepository manageableRepository = null;
      if (withContent) {
        manageableRepository = repositoryService.getCurrentRepository();
        systemWorkspace = manageableRepository.getConfiguration().getDefaultWorkspaceName();
        sessionProvider = SessionProvider.createSystemProvider();
      }

      // If only one group selected
      if (groupId != null && !groupId.trim().isEmpty()) {
        if (!groupId.startsWith("/")) {
          groupId = "/" + groupId;
        }
        Group group = organizationService.getGroupHandler().findGroupById(groupId);
        if (group == null) {
          throw new OperationException(OperationNames.EXPORT_RESOURCE, "Group with name '" + groupId + "' doesn't exist.");
        }
        exportGroup(group, withContent, withMemberships, sessionProvider, manageableRepository, systemWorkspace, exportTasks);
      } else {
        // If all groups will be exported
        Collection<?> groups = organizationService.getGroupHandler().getAllGroups();
        for (Object object : groups) {
          exportGroup(((Group) object), withContent, withMemberships, sessionProvider, manageableRepository, systemWorkspace, exportTasks);
        }
      }
      resultHandler.completed(new ExportResourceModel(exportTasks));
    } catch (Exception e) {
      throw new OperationException(OperationNames.EXPORT_RESOURCE, "Unable to export Group : ", e);
    }
  }

  private void exportGroup(Group group, boolean withContent, boolean withMemberships, SessionProvider sessionProvider, ManageableRepository manageableRepository, String systemWorkspace,
      List<ExportTask> exportTasks) throws Exception {
    exportTasks.add(new OrganizationModelExportTask(group));
    if (withContent) {
      Session session = sessionProvider.getSession(systemWorkspace, manageableRepository);
      Node groupsHome = (Node) session.getItem(groupsPath);
      Node groupNode = dataDistributionType.getOrCreateDataNode(groupsHome, group.getId());
      exportTasks.add(new OrganizationModelJCRContentExportTask(repositoryService, groupNode, group));
    }
    if (withMemberships) {
      ListAccess<Membership> memberships = organizationService.getMembershipHandler().findAllMembershipsByGroup(group);
      Membership[] membershipArray = memberships.load(0, memberships.getSize());
      for (Membership membership : membershipArray) {
        exportTasks.add(new OrganizationModelExportTask(membership));
      }
    }
  }
}
