package org.exoplatform.management.organization.user;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

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
import org.exoplatform.services.organization.UserProfile;
import org.exoplatform.services.organization.impl.MembershipImpl;
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
public class UserImportResource extends AbstractJCRImportOperationHandler {
  private static final Log log = ExoLogger.getLogger(UserImportResource.class);

  private static final String USERS_BASE_PATH = OrganizationManagementExtension.PATH_ORGANIZATION + "/" + OrganizationManagementExtension.PATH_ORGANIZATION_USER + "/";

  private OrganizationService organizationService = null;

  @Override
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws OperationException {
    if (organizationService == null) {
      organizationService = operationContext.getRuntimeContext().getRuntimeComponent(OrganizationService.class);
      repositoryService = operationContext.getRuntimeContext().getRuntimeComponent(RepositoryService.class);
    }

    increaseCurrentTransactionTimeOut(operationContext);
    try {
      InputStream attachmentInputStream = null;

      OperationAttributes attributes = operationContext.getAttributes();
      List<String> filters = attributes.getValues("filter");

      // "replace-existing" attribute. Defaults to false.
      boolean replaceExisting = filters.contains("replace-existing:true");

      Set<String> newUsers = new HashSet<String>();

      // get attachement input stream
      OperationAttachment attachment = operationContext.getAttachment(false);
      attachmentInputStream = attachment.getStream();
      File tempFile = null;
      try {
        tempFile = File.createTempFile("ImportOperationAttachment", ".zip");
        OutputStream fos = new FileOutputStream(tempFile);
        IOUtils.copy(attachmentInputStream, fos);
        fos.close();

        // User
        importUsers(tempFile, newUsers, replaceExisting);

        // UserProfile
        importUserProfiles(tempFile, newUsers, replaceExisting);

        // Memberships
        importMemberships(tempFile, newUsers, replaceExisting);

        // Content
        importUserJCRNodes(tempFile, newUsers, replaceExisting);

        resultHandler.completed(NoResultModel.INSTANCE);
      } catch (Exception e) {
        throw new OperationException(OperationNames.IMPORT_RESOURCE, "Error while reading View Templates from Stream.", e);
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

  private void importUserJCRNodes(File tempFile, Set<String> newlyCreatedUsers, boolean replaceExisting) throws FileNotFoundException, IOException, Exception {
    ManageableRepository manageableRepository = repositoryService.getCurrentRepository();
    String defaultWorkspace = manageableRepository.getConfiguration().getDefaultWorkspaceName();

    ZipEntry entry = null;
    ZipInputStream zin = new ZipInputStream(new FileInputStream(tempFile));
    try {
      while ((entry = zin.getNextEntry()) != null) {
        try {
          String filePath = entry.getName();

          if (!filePath.startsWith(USERS_BASE_PATH) || !filePath.contains(JCRNodeExportTask.JCR_DATA_SEPARATOR)) {
            continue;
          }
          if (entry.isDirectory() || filePath.trim().isEmpty() || !filePath.endsWith(".xml")) {
            continue;
          }

          log.info("Parsing : " + filePath);
          String username = extractUserName(filePath);
          String nodePath = extractParam(filePath, 2);
          boolean replaceExistingContent = replaceExisting || newlyCreatedUsers.contains(username);
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

  private void importMemberships(File tempFile, Set<String> newUsers, boolean replaceExisting) throws FileNotFoundException, IOException, Exception {
    ZipInputStream zin = new ZipInputStream(new FileInputStream(tempFile));
    try {
      ZipEntry entry;
      while ((entry = zin.getNextEntry()) != null) {
        try {
          String filePath = entry.getName();
          if (filePath.startsWith(USERS_BASE_PATH) && filePath.endsWith("_membership.xml")) {
            log.debug("Parsing : " + filePath);
            String userName = extractUserName(filePath);
            if (replaceExisting || newUsers.contains(userName)) {
              createMembership(zin);
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
  }

  private void importUsers(File tempFile, Set<String> newUsers, boolean replaceExisting) throws FileNotFoundException, IOException, Exception {
    ZipInputStream zin = new ZipInputStream(new FileInputStream(tempFile));
    try {
      ZipEntry entry;
      while ((entry = zin.getNextEntry()) != null) {
        try {
          String filePath = entry.getName();
          if (filePath.startsWith(USERS_BASE_PATH) && filePath.endsWith("user.xml")) {
            log.debug("Parsing : " + filePath);
            String userName = extractUserName(filePath);
            User existingUser = organizationService.getUserHandler().findUserByName(userName);
            if (existingUser == null) {
              newUsers.add(userName);
            }
            createUser(userName, zin, existingUser, replaceExisting);
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

  private void importUserProfiles(File tempFile, Set<String> newUsers, boolean replaceExisting) throws FileNotFoundException, IOException, Exception {
    ZipInputStream zin = new ZipInputStream(new FileInputStream(tempFile));
    try {
      ZipEntry entry;
      while ((entry = zin.getNextEntry()) != null) {
        try {
          String filePath = entry.getName();
          if (filePath.startsWith(USERS_BASE_PATH) && filePath.endsWith("profile.xml")) {
            log.debug("Parsing : " + filePath);
            String userName = extractUserName(filePath);
            if (replaceExisting || newUsers.contains(userName)) {
              createUserProfile(userName, zin);
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
  }

  /**
   * Extract the username from the file path
   * 
   * @param filePath
   * @return
   */
  private String extractUserName(String filePath) {
    String username = null;
    if (filePath.startsWith(USERS_BASE_PATH)) {
      String filePathEnd = filePath.substring(USERS_BASE_PATH.length());
      username = filePathEnd.substring(0, filePathEnd.indexOf("/"));
    } else {
      throw new IllegalStateException("Incorrect file path (" + filePath + ") must start with /organization/users/");
    }
    return username;
  }

  private void createUser(String username, final ZipInputStream zin, User existingUser, boolean replaceExisting) throws Exception {

    boolean alreadyExists = (existingUser != null);

    if (!alreadyExists) {
      log.info("Creating user '" + username + "'");
      User user = deserializeObject(zin, organizationService.getUserHandler().createUserInstance("test").getClass(), "organization");
      organizationService.getUserHandler().createUser(user, true);
    } else if (replaceExisting && alreadyExists) {
      log.info("ReplaceExisting is On: Updating user '" + username + "'");
      User user = deserializeObject(zin, organizationService.getUserHandler().createUserInstance("test").getClass(), "organization");
      organizationService.getUserHandler().saveUser(user, true);

      // Delete Memberships to replace it by what is unserialized
      Collection<?> memberships = organizationService.getMembershipHandler().findMembershipsByUser(user.getUserName());
      for (Object membership : memberships) {
        organizationService.getMembershipHandler().removeMembership(((Membership) membership).getId(), true);
      }
    } else {
      log.info("ReplaceExisting is Off: Ignoring user '" + username + "'");
    }
  }

  private void createUserProfile(String username, final ZipInputStream zin) throws Exception {
    UserProfile profile = deserializeObject(zin, organizationService.getUserProfileHandler().createUserProfileInstance().getClass(), "organization");
    organizationService.getUserProfileHandler().saveUserProfile(profile, true);
  }

  @SuppressWarnings("deprecation")
  private void createMembership(final ZipInputStream zin) throws Exception {
    Membership membership = null;
    try {
      membership = deserializeObject(zin, organizationService.getMembershipHandler().createMembershipInstance().getClass(), "organization");
    } catch (Exception e) {
      log.warn("Can't serialize membership with : " + MembershipImpl.class.getName() + ". Trying with : " + org.exoplatform.services.organization.idm.MembershipImpl.class.getName());
      membership = deserializeObject(zin, org.exoplatform.services.organization.idm.MembershipImpl.class, "organization");
    }

    Membership oldMembership = organizationService.getMembershipHandler().findMembership(membership.getId());
    if (oldMembership == null) {
      log.info("Membership '" + membership.getMembershipType() + ":" + membership.getGroupId() + "' was not found for user " + membership.getUserName() + ", creating it.");
      Group group = organizationService.getGroupHandler().findGroupById(membership.getGroupId());
      User user = organizationService.getUserHandler().findUserByName(membership.getUserName());
      MembershipType membershipType = organizationService.getMembershipTypeHandler().findMembershipType(membership.getMembershipType());
      organizationService.getMembershipHandler().linkMembership(user, group, membershipType, true);
    } else {
      log.info("Membership '" + membership.getMembershipType() + ":" + membership.getGroupId() + "' already exists for user " + membership.getUserName() + " : ignoring.");
    }
  }

  private static String extractParam(String filePath, int i) {
    Pattern pattern = Pattern.compile(USERS_BASE_PATH + "(.*)/" + JCRNodeExportTask.JCR_DATA_SEPARATOR + "(.*)");
    Matcher matcher = pattern.matcher(filePath);
    if (matcher.find()) {
      return matcher.group(i);
    } else {
      throw new IllegalStateException("filePath can't be managed: " + filePath);
    }
  }
}
