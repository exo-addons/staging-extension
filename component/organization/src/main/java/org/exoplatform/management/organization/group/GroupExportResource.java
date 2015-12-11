package org.exoplatform.management.organization.group;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Session;

import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.management.common.exportop.AbstractJCRExportOperationHandler;
import org.exoplatform.management.common.exportop.JCRNodeExportTask;
import org.exoplatform.management.organization.OrganizationManagementExtension;
import org.exoplatform.management.organization.OrganizationModelExportTask;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
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
public class GroupExportResource extends AbstractJCRExportOperationHandler {

  private OrganizationService organizationService = null;
  private NodeHierarchyCreator hierarchyCreator = null;
  private DataDistributionManager dataDistributionManager = null;
  private DataDistributionType dataDistributionType = null;
  private String groupsPath = null;

  @Override
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws OperationException {
    List<ExportTask> exportTasks = new ArrayList<ExportTask>();
    increaseCurrentTransactionTimeOut(operationContext);
    try {
      if (organizationService == null) {
        organizationService = operationContext.getRuntimeContext().getRuntimeComponent(OrganizationService.class);
        repositoryService = operationContext.getRuntimeContext().getRuntimeComponent(RepositoryService.class);
        hierarchyCreator = operationContext.getRuntimeContext().getRuntimeComponent(NodeHierarchyCreator.class);
        dataDistributionManager = operationContext.getRuntimeContext().getRuntimeComponent(DataDistributionManager.class);
        dataDistributionType = dataDistributionManager.getDataDistributionType(DataDistributionMode.NONE);
        groupsPath = hierarchyCreator.getJcrPath(OrganizationManagementExtension.GROUPS_PATH);
      }

      PathAddress address = operationContext.getAddress();
      String groupId = address.resolvePathTemplate("group-name");

      OperationAttributes attributes = operationContext.getAttributes();
      List<String> filters = attributes.getValues("filter");

      boolean withContent = filters.contains("with-jcr-content:true");
      boolean withMemberships = filters.contains("with-membership:true");

      String defaultWorkspace = null;
      if (withContent) {
        ManageableRepository manageableRepository = repositoryService.getCurrentRepository();
        defaultWorkspace = manageableRepository.getConfiguration().getDefaultWorkspaceName();
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
        exportGroup(group, withContent, withMemberships, defaultWorkspace, exportTasks);
      } else {
        // If all groups will be exported
        Collection<?> groups = organizationService.getGroupHandler().getAllGroups();
        for (Object object : groups) {
          exportGroup(((Group) object), withContent, withMemberships, defaultWorkspace, exportTasks);
        }
      }
      resultHandler.completed(new ExportResourceModel(exportTasks));
    } catch (Exception e) {
      throw new OperationException(OperationNames.EXPORT_RESOURCE, "Unable to export Group : ", e);
    } finally {
      restoreDefaultTransactionTimeOut(operationContext);
    }
  }

  private void exportGroup(Group group, boolean withContent, boolean withMemberships, String defaultWorkspace, List<ExportTask> exportTasks) throws Exception {
    exportTasks.add(new OrganizationModelExportTask(group));
    if (withContent) {
      Session session = getSession(defaultWorkspace);
      Node groupsHome = (Node) session.getItem(groupsPath);
      Node groupNode = dataDistributionType.getOrCreateDataNode(groupsHome, group.getId());
      exportNode(groupNode, exportTasks, new String[] { defaultWorkspace, group.getId() });
    }
    if (withMemberships) {
      ListAccess<Membership> memberships = organizationService.getMembershipHandler().findAllMembershipsByGroup(group);
      Membership[] membershipArray = memberships.load(0, memberships.getSize());
      for (Membership membership : membershipArray) {
        exportTasks.add(new OrganizationModelExportTask(membership));
      }
    }
  }

  @Override
  protected void addJCRNodeExportTask(Node childNode, List<ExportTask> subNodesExportTask, boolean recursive, String... params) throws Exception {
    String prefixInZiipFile = OrganizationManagementExtension.PATH_ORGANIZATION + "/" + OrganizationManagementExtension.PATH_ORGANIZATION_GROUP + "/" + params[1];
    subNodesExportTask.add(new JCRNodeExportTask(repositoryService, params[0], childNode.getPath(), prefixInZiipFile, recursive, true));
  }
}