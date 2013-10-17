package org.exoplatform.management.organization.group;

import com.thoughtworks.xstream.XStream;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.util.IOUtils;
import org.exoplatform.management.organization.OrganizationManagementExtension;
import org.exoplatform.management.organization.OrganizationModelJCRContentExportTask;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.distribution.DataDistributionManager;
import org.exoplatform.services.jcr.ext.distribution.DataDistributionMode;
import org.exoplatform.services.jcr.ext.distribution.DataDistributionType;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.*;
import org.exoplatform.services.organization.impl.UserImpl;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.operation.*;
import org.gatein.management.api.operation.model.NoResultModel;

import javax.jcr.ImportUUIDBehavior;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Session;
import java.io.*;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * @author <a href="mailto:boubaker.khanfir@exoplatform.com">Boubaker
 *         Khanfir</a>
 */
public class GroupImportResource implements OperationHandler {
  private static final Log log = ExoLogger.getLogger(GroupImportResource.class);
  private OrganizationService organizationService = null;
  private RepositoryService repositoryService = null;
  private NodeHierarchyCreator hierarchyCreator = null;
  private DataDistributionManager dataDistributionManager = null;
  private DataDistributionType dataDistributionType = null;
  private String groupsPath = null;

  @Override
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws OperationException {
    if (organizationService == null) {
      organizationService = operationContext.getRuntimeContext().getRuntimeComponent(OrganizationService.class);
      repositoryService = operationContext.getRuntimeContext().getRuntimeComponent(RepositoryService.class);
      hierarchyCreator = operationContext.getRuntimeContext().getRuntimeComponent(NodeHierarchyCreator.class);
      dataDistributionManager = operationContext.getRuntimeContext().getRuntimeComponent(DataDistributionManager.class);
      dataDistributionType = dataDistributionManager.getDataDistributionType(DataDistributionMode.NONE);
      groupsPath = hierarchyCreator.getJcrPath(OrganizationModelJCRContentExportTask.GROUPS_PATH);
    }

    Boolean replaceExisting = null;

    OperationAttributes attributes = operationContext.getAttributes();
    if (attributes != null && attributes.getValues("filter") != null && !attributes.getValues("filter").isEmpty()) {
      Iterator<String> filters = attributes.getValues("filter").iterator();
      while (filters.hasNext() && replaceExisting == null) {
        String filter = filters.next();
        if (filter.startsWith("replace-existing:")) {
          try {
            replaceExisting = Boolean.parseBoolean(filter.substring("replace-existing:".length()));
          } catch (Exception e) {
            log.warn(filter + " filter expression is not valid.");
          }
        }
      }
    }
    if (replaceExisting == null) {
      replaceExisting = false;
    }

    // get attachement input stream
    OperationAttachment attachment = operationContext.getAttachment(false);
    InputStream attachmentInputStream = attachment.getStream();
    File tempFile = null;
    try {
      tempFile = File.createTempFile("ImportOperationAttachment", ".zip");
      OutputStream fos = new FileOutputStream(tempFile);
      IOUtils.copy(attachmentInputStream, fos);
      fos.close();

      ZipInputStream zin = new ZipInputStream(new FileInputStream(tempFile));
      ZipEntry entry;
      Set<String> newlyCreatedGroups = new HashSet<String>();
      while ((entry = zin.getNextEntry()) != null) {
        String filePath = entry.getName();
        if (filePath.startsWith(OrganizationManagementExtension.PATH_ORGANIZATION + "/" + OrganizationManagementExtension.PATH_ORGANIZATION_GROUP + "/") && filePath.endsWith("group.xml")) {
          log.debug("Parsing : " + filePath);
          String groupId = createGroup(zin, replaceExisting);
          if (groupId != null) {
            newlyCreatedGroups.add(groupId);
          }
        }
        try {
          zin.closeEntry();
        } catch (Exception e) {
          // Already closed, expected
        }
      }
      zin.close();
      zin = new ZipInputStream(new FileInputStream(tempFile));
      while ((entry = zin.getNextEntry()) != null) {
        String filePath = entry.getName();
        if (filePath.endsWith("_membership.xml")) {
          log.debug("Parsing : " + filePath);
          createMembership(zin);
        }
        try {
          zin.closeEntry();
        } catch (Exception e) {
          // Already closed, expected
        }
      }
      zin.close();
      zin = new ZipInputStream(new FileInputStream(tempFile));
      while ((entry = zin.getNextEntry()) != null) {
        String filePath = entry.getName();
        if (!filePath.startsWith(OrganizationManagementExtension.PATH_ORGANIZATION + "/" + OrganizationManagementExtension.PATH_ORGANIZATION_GROUP + "/")) {
          continue;
        }
        if (entry.isDirectory() || filePath.trim().isEmpty() || !filePath.endsWith(".xml")) {
          continue;
        }
        if (filePath.endsWith("g_content.xml")) {
          log.debug("Parsing : " + filePath);
          String groupId = extractGroupName(filePath);
          boolean replaceExistingContent = replaceExisting || newlyCreatedGroups.contains(groupId);
          createContent(zin, groupId, replaceExistingContent);
        }
        try {
          zin.closeEntry();
        } catch (Exception e) {
          // Already closed, expected
        }
      }
      zin.close();
      resultHandler.completed(NoResultModel.INSTANCE);
    } catch (Exception e) {
      throw new OperationException(OperationNames.IMPORT_RESOURCE, "Error while reading group from Stream.", e);
    } finally {
      if (tempFile != null && tempFile.exists()) {
        try {
          tempFile.delete();
        } catch (Exception e) {
          tempFile.deleteOnExit();
        }
      }
    }
  }

  @SuppressWarnings("deprecation")
  private void createMembership(final ZipInputStream zin) throws Exception {
    Membership membership = null;
    try {
      membership = deserializeObject(zin, organizationService.getMembershipHandler().createMembershipInstance().getClass());
    } catch (Exception e) {
      log.warn("Can't serialize membership with : " + UserImpl.class.getName() + ". Trying with : " + org.exoplatform.services.organization.idm.UserImpl.class.getName());
      membership = deserializeObject(zin, org.exoplatform.services.organization.idm.MembershipImpl.class);
    }
    Membership oldMembership = organizationService.getMembershipHandler().findMembership(membership.getId());
    if (oldMembership == null) {
      log.info("Membership '" + membership.getId() + "' was not found, creating it.");
      User user = organizationService.getUserHandler().findUserByName(membership.getUserName());
      if (user != null) {
        Group group = organizationService.getGroupHandler().findGroupById(membership.getGroupId());
        MembershipType membershipType = organizationService.getMembershipTypeHandler().findMembershipType(membership.getMembershipType());
        organizationService.getMembershipHandler().linkMembership(user, group, membershipType, true);
      }
    } else {
      log.info("Membership already exists: Ignoring.");
    }
  }

  private void createContent(ZipInputStream zin, String groupId, boolean replaceExisting) {
    try {
      SessionProvider sessionProvider = SessionProvider.createSystemProvider();
      ManageableRepository manageableRepository = repositoryService.getCurrentRepository();
      String systemWorkspace = manageableRepository.getConfiguration().getDefaultWorkspaceName();

      Session session = sessionProvider.getSession(systemWorkspace, manageableRepository);
      Node groupsHome = (Node) session.getItem(groupsPath);

      Node groupNode = null;
      String parentPath = null;
      try {
        groupNode = dataDistributionType.getDataNode(groupsHome, groupId);
        parentPath = groupNode.getParent().getPath();
      } catch (PathNotFoundException e) {
        // Nothing to do
      }
      if (groupNode == null || replaceExisting) {
        if (groupNode != null) {
          groupNode.remove();
          session.save();
        }
        if (parentPath == null) {
          if (StringUtils.countMatches(groupId, "/") > 1) {
            String parentId = groupId.substring(0, groupId.lastIndexOf("/") + 1);
            // Should be found, else an exception will be thrown, because this
            // will be an incoherence
            Node parentNode = dataDistributionType.getDataNode(groupsHome, parentId);
            parentPath = parentNode.getPath();
          } else {
            parentPath = groupsPath;
          }
        }
        ByteArrayInputStream bis = new ByteArrayInputStream(IOUtils.toByteArray(zin));
        session.importXML(parentPath, bis, ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
        session.save();
      }
    } catch (Exception exception) {
      throw new OperationException(OperationNames.IMPORT_RESOURCE, "Error while getting group's folder:", exception);
    }
  }

  private String createGroup(final ZipInputStream zin, Boolean replaceExisting) throws Exception {
    Group group = deserializeObject(zin, organizationService.getGroupHandler().createGroupInstance().getClass());
    Group oldGroup = organizationService.getGroupHandler().findGroupById(group.getId());

    if (oldGroup != null) {
      if (replaceExisting) {
        log.info("ReplaceExisting is On: Updating group '" + group.getId() + "'.");
        organizationService.getGroupHandler().saveGroup(group, false);
      } else {
        log.info("ReplaceExisting is Off: group '" + group.getId() + "' is ignored.");
      }
    } else {
      Group parent = checkExists(group.getParentId());
      organizationService.getGroupHandler().addChild(parent, group, true);
      try {
        SessionProvider sessionProvider = SessionProvider.createSystemProvider();
        ManageableRepository manageableRepository = repositoryService.getCurrentRepository();
        String systemWorkspace = manageableRepository.getConfiguration().getDefaultWorkspaceName();

        Session session = sessionProvider.getSession(systemWorkspace, manageableRepository);
        try {
          Node groupsHome = (Node) session.getItem(groupsPath);
          Node groupNode = dataDistributionType.getDataNode(groupsHome, group.getId());
          groupNode.remove();
          session.save();
        } catch (PathNotFoundException e) {
          // Nothing to do
        }
      } catch (Exception exception) {
        throw new OperationException(OperationNames.IMPORT_RESOURCE, "Error while getting group's folder:", exception);
      }
    }
    return (oldGroup == null ? group.getId() : null);
  }

  private Group checkExists(String groupId) throws Exception {
    if (groupId == null || groupId.trim().isEmpty()) {
      return null;
    }
    Group group = organizationService.getGroupHandler().findGroupById(groupId);
    if (group == null) {
      groupId = groupId.replaceAll("/$", "");
      Group parent = null;
      if (StringUtils.countMatches(groupId, "/") > 1) {
        String parentId = groupId.substring(0, groupId.lastIndexOf("/"));
        parent = checkExists(parentId);
      }
      String groupName = groupId.substring(groupId.lastIndexOf("/") + 1);
      group = organizationService.getGroupHandler().createGroupInstance();
      group.setGroupName(groupName);
      group.setDescription("Description of " + groupName);
      group.setLabel(groupName);
      organizationService.getGroupHandler().addChild(parent, group, true);
    }
    return group;
  }

  private String extractGroupName(String filePath) {
    Pattern pattern = Pattern.compile(OrganizationManagementExtension.PATH_ORGANIZATION + "/" + OrganizationManagementExtension.PATH_ORGANIZATION_GROUP + "/(.*)/g_content.xml");
    Matcher matcher = pattern.matcher(filePath);
    if (matcher.find()) {
      return matcher.group(1);
    } else {
      throw new IllegalStateException("filePath doesn't match groups/*/g_content.xml");
    }
  }

  private <T> T deserializeObject(final ZipInputStream zin, Class<T> objectClass) {
    XStream xStream = new XStream();
    xStream.alias("organization", objectClass);
    @SuppressWarnings("unchecked")
    T object = (T) xStream.fromXML(zin);
    return object;
  }
}
