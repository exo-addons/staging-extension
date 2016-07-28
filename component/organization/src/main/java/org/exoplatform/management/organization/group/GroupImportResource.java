package org.exoplatform.management.organization.group;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.lang.StringUtils;
import org.apache.poi.util.IOUtils;
import org.exoplatform.management.common.exportop.JCRNodeExportTask;
import org.exoplatform.management.common.importop.AbstractJCRImportOperationHandler;
import org.exoplatform.management.organization.OrganizationManagementExtension;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.Membership;
import org.exoplatform.services.organization.MembershipType;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.operation.OperationAttachment;
import org.gatein.management.api.operation.OperationAttributes;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.NoResultModel;

/**
 * @author <a href="mailto:boubaker.khanfir@exoplatform.com">Boubaker
 *         Khanfir</a>
 */
public class GroupImportResource extends AbstractJCRImportOperationHandler {
  private static final String GROUPS_PARENT_PATH = OrganizationManagementExtension.PATH_ORGANIZATION + "/" + OrganizationManagementExtension.PATH_ORGANIZATION_GROUP + "/";

  private static final Log log = ExoLogger.getLogger(GroupImportResource.class);
  private OrganizationService organizationService = null;

  @Override
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws OperationException {
    if (organizationService == null) {
      organizationService = operationContext.getRuntimeContext().getRuntimeComponent(OrganizationService.class);
      repositoryService = operationContext.getRuntimeContext().getRuntimeComponent(RepositoryService.class);
    }

    increaseCurrentTransactionTimeOut(operationContext);
    try {
      OperationAttributes attributes = operationContext.getAttributes();
      List<String> filters = attributes.getValues("filter");

      // "replace-existing" attribute. Defaults to false.
      boolean replaceExisting = filters.contains("replace-existing:true");

      // get attachement input stream
      OperationAttachment attachment = operationContext.getAttachment(false);
      InputStream attachmentInputStream = attachment.getStream();
      File tempFile = null;
      try {
        tempFile = File.createTempFile("ImportOperationAttachment", ".zip");
        OutputStream fos = new FileOutputStream(tempFile);
        IOUtils.copy(attachmentInputStream, fos);
        fos.close();

        // Start by importing all groups
        Set<String> newlyCreatedGroups = importGroups(tempFile, replaceExisting);

        // Importing memberships
        importMemberships(tempFile);

        // Importing group JCR contents
        importGroupJCRNodes(tempFile, newlyCreatedGroups, replaceExisting);

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
    } finally {
      restoreDefaultTransactionTimeOut(operationContext);
    }
  }

  private void importGroupJCRNodes(File tempFile, Set<String> newlyCreatedGroups, boolean replaceExisting) throws FileNotFoundException, IOException, Exception {
    ManageableRepository manageableRepository = repositoryService.getCurrentRepository();
    String defaultWorkspace = manageableRepository.getConfiguration().getDefaultWorkspaceName();

    ZipEntry entry = null;
    ZipInputStream zin = new ZipInputStream(new FileInputStream(tempFile));
    try {
      while ((entry = zin.getNextEntry()) != null) {
        try {
          String filePath = entry.getName();

          if (!filePath.startsWith(GROUPS_PARENT_PATH) || !filePath.contains(JCRNodeExportTask.JCR_DATA_SEPARATOR)) {
            continue;
          }
          if (entry.isDirectory() || filePath.trim().isEmpty() || !filePath.endsWith(".xml")) {
            continue;
          }

          log.info("Parsing : " + filePath);
          String groupId = extractParam(filePath, 1);
          String nodePath = extractParam(filePath, 2);
          boolean replaceExistingContent = replaceExisting || newlyCreatedGroups.contains(groupId);
          if (replaceExistingContent) {
            importNode(nodePath, defaultWorkspace, zin, null, false, true);
          }
        } finally {
          try {
            zin.closeEntry();
          } catch (Exception e) {
            // Already closed, expected
          }
        }
      }
    } finally {
      zin.close();
    }
  }

  private Set<String> importGroups(File tempFile, boolean replaceExisting) throws Exception {
    Set<String> newlyCreatedGroups = new HashSet<String>();
    ZipInputStream zin = new ZipInputStream(new FileInputStream(tempFile));
    try {
      ZipEntry entry = null;
      while ((entry = zin.getNextEntry()) != null) {
        try {
          String filePath = entry.getName();
          if (filePath.startsWith(GROUPS_PARENT_PATH) && filePath.endsWith("group.xml")) {
            log.debug("Parsing : " + filePath);
            String groupId = createGroup(zin, replaceExisting);
            if (groupId != null) {
              newlyCreatedGroups.add(groupId);
            }
          }
        } finally {
          try {
            zin.closeEntry();
          } catch (Exception e) {
            // Already closed, expected
          }
        }
      }
    } finally {
      zin.close();
    }
    return newlyCreatedGroups;
  }

  private void importMemberships(File tempFile) throws Exception {
    ZipInputStream zin = new ZipInputStream(new FileInputStream(tempFile));
    try {
      ZipEntry entry = null;
      while ((entry = zin.getNextEntry()) != null) {
        try {
          String filePath = entry.getName();
          if (filePath.endsWith("_membership.xml")) {
            log.debug("Parsing : " + filePath);
            createMembership(zin);
          }
        } finally {
          try {
            zin.closeEntry();
          } catch (Exception e) {
            // Already closed, expected
          }
        }
      }
    } finally {
      zin.close();
    }
  }

  @SuppressWarnings("deprecation")
  private void createMembership(final ZipInputStream zin) throws Exception {
    Membership membership = null;
    try {
      membership = deserializeObject(zin, organizationService.getMembershipHandler().createMembershipInstance().getClass(), "organization");
    } catch (Exception e) {
      log.warn("Can't serialize membership with. Trying with : " + org.exoplatform.services.organization.idm.MembershipImpl.class.getName());
      membership = deserializeObject(zin, org.exoplatform.services.organization.idm.MembershipImpl.class, "organization");
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

  private String createGroup(final ZipInputStream zin, Boolean replaceExisting) throws Exception {
    Group group = deserializeObject(zin, organizationService.getGroupHandler().createGroupInstance().getClass(), "organization");
    Group oldGroup = organizationService.getGroupHandler().findGroupById(group.getId());

    if (oldGroup != null) {
      if (replaceExisting) {
        log.info("ReplaceExisting is On: Updating group '" + group.getId() + "'.");
        organizationService.getGroupHandler().saveGroup(group, false);
      } else {
        log.info("ReplaceExisting is Off: group '" + group.getId() + "' is ignored.");
      }
    } else {
      Group parent = createGroupIfNotExists(group.getParentId());
      organizationService.getGroupHandler().addChild(parent, group, true);
    }
    return (oldGroup == null ? group.getId() : null);
  }

  private Group createGroupIfNotExists(String groupId) throws Exception {
    if (groupId == null || groupId.trim().isEmpty()) {
      return null;
    }
    Group group = organizationService.getGroupHandler().findGroupById(groupId);
    if (group == null) {
      groupId = groupId.replaceAll("/$", "");
      Group parent = null;
      if (StringUtils.countMatches(groupId, "/") > 1) {
        String parentId = groupId.substring(0, groupId.lastIndexOf("/"));
        parent = createGroupIfNotExists(parentId);
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

  private static String extractParam(String filePath, int i) {
    Pattern pattern = Pattern.compile(GROUPS_PARENT_PATH + "(.*)/" + JCRNodeExportTask.JCR_DATA_SEPARATOR + "(.*)");
    Matcher matcher = pattern.matcher(filePath);
    if (matcher.find()) {
      return matcher.group(i);
    } else {
      throw new IllegalStateException("filePath can't be managed: " + filePath);
    }
  }
}
