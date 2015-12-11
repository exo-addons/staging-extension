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
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.poi.util.IOUtils;
import org.exoplatform.management.common.FileEntry;
import org.exoplatform.management.common.exportop.JCRNodeExportTask;
import org.exoplatform.management.common.importop.AbstractJCRImportOperationHandler;
import org.exoplatform.management.common.importop.FileImportOperationInterface;
import org.exoplatform.management.organization.OrganizationManagementExtension;
import org.exoplatform.services.jcr.RepositoryService;
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
public class UserImportResource extends AbstractJCRImportOperationHandler implements FileImportOperationInterface {
  private static final Log log = ExoLogger.getLogger(UserImportResource.class);

  private static final String USERS_BASE_PATH = OrganizationManagementExtension.PATH_ORGANIZATION + "/" + OrganizationManagementExtension.PATH_ORGANIZATION_USER + "/";

  private OrganizationService organizationService = null;

  @Override
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws OperationException {
    if (organizationService == null) {
      organizationService = operationContext.getRuntimeContext().getRuntimeComponent(OrganizationService.class);
      repositoryService = operationContext.getRuntimeContext().getRuntimeComponent(RepositoryService.class);
    }

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

    increaseCurrentTransactionTimeOut(operationContext);
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
      importUserJCRNodes(tempFile);

      resultHandler.completed(NoResultModel.INSTANCE);
    } catch (Exception e) {
      throw new OperationException(OperationNames.IMPORT_RESOURCE, "Error while importing users.", e);
    } finally {
      restoreDefaultTransactionTimeOut(operationContext);
      if (tempFile != null && tempFile.exists()) {
        try {
          tempFile.delete();
        } catch (Exception e) {
          tempFile.deleteOnExit();
        }
      }
    }
  }

  private void importUserJCRNodes(File tempFile) throws Exception {
    // extract data from zip
    Map<String, List<FileEntry>> contentsByOwner = extractDataFromZip(new FileInputStream(tempFile));

    for (String site : contentsByOwner.keySet()) {
      List<FileEntry> fileEntries = contentsByOwner.get(site);
      try {
        if (fileEntries != null) {
          for (FileEntry fileEntry : fileEntries) {
            log.info("Importing content '" + fileEntry.getNodePath() + "'.");
            importNode(fileEntry, null, false);
          }
          log.info("Content import is done.");
        }
      } catch (Exception e) {
        log.error("Error while importing users: " + site, e);
      }
    }
  }

  private void importMemberships(File tempFile, Set<String> newUsers, boolean replaceExisting) throws FileNotFoundException, IOException, Exception {
    ZipEntry entry;
    ZipInputStream zin = new ZipInputStream(new FileInputStream(tempFile));
    while ((entry = zin.getNextEntry()) != null) {
      String filePath = entry.getName();
      if (filePath.startsWith(USERS_BASE_PATH) && filePath.endsWith("_membership.xml")) {
        log.info("Parsing : " + filePath);
        String userName = extractIdFromPath(filePath);
        if (replaceExisting || newUsers.contains(userName)) {
          createMembership(zin);
        }
      }
      try {
        zin.closeEntry();
      } catch (Exception e) {
        // Already closed, expected
      }
    }
    zin.close();
  }

  private void importUsers(File tempFile, Set<String> newUsers, boolean replaceExisting) throws FileNotFoundException, IOException, Exception {
    ZipEntry entry;
    ZipInputStream zin = new ZipInputStream(new FileInputStream(tempFile));
    while ((entry = zin.getNextEntry()) != null) {
      String filePath = entry.getName();
      if (filePath.startsWith(USERS_BASE_PATH) && filePath.endsWith("user.xml")) {
        log.debug("Parsing : " + filePath);
        String userName = extractIdFromPath(filePath);
        User existingUser = organizationService.getUserHandler().findUserByName(userName);
        if (existingUser == null) {
          newUsers.add(userName);
        }
        createUser(userName, zin, existingUser, replaceExisting);
      }
      try {
        zin.closeEntry();
      } catch (Exception e) {
        // Already closed, expected
      }
    }
    zin.close();
  }

  private void importUserProfiles(File tempFile, Set<String> newUsers, boolean replaceExisting) throws FileNotFoundException, IOException, Exception {
    ZipEntry entry;
    ZipInputStream zin = new ZipInputStream(new FileInputStream(tempFile));
    while ((entry = zin.getNextEntry()) != null) {
      String filePath = entry.getName();
      if (filePath.startsWith(USERS_BASE_PATH) && filePath.endsWith("profile.xml")) {
        log.debug("Parsing : " + filePath);
        String userName = extractIdFromPath(filePath);
        if (replaceExisting || newUsers.contains(userName)) {
          createUserProfile(userName, zin);
        }
      }
      try {
        zin.closeEntry();
      } catch (Exception e) {
        // Already closed, expected
      }
    }
    zin.close();
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
      try {
        organizationService.getUserHandler().saveUser(user, true);
      } catch (Exception e) {
        log.warn("Error while updating user '" + username + "'. Save operation will try with broadcast = false.", e);
        organizationService.getUserHandler().saveUser(user, false);
      }

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
      if (group == null) {
        log.info("Can't create Membership '" + membership.getMembershipType() + ":" + membership.getGroupId() + ":" + membership.getUserName() + "', the group was not found.");
        return;
      }
      User user = organizationService.getUserHandler().findUserByName(membership.getUserName());
      MembershipType membershipType = organizationService.getMembershipTypeHandler().findMembershipType(membership.getMembershipType());
      organizationService.getMembershipHandler().linkMembership(user, group, membershipType, true);
    } else {
      log.info("Membership '" + membership.getMembershipType() + ":" + membership.getGroupId() + "' already exists for user " + membership.getUserName() + " : ignoring.");
    }
  }

  @Override
  public String getManagedFilesPrefix() {
    return "organization/user/";
  }

  @Override
  public boolean isUnKnownFileFormat(String filePath) {
    return !filePath.endsWith(".xml") || !filePath.contains(JCRNodeExportTask.JCR_DATA_SEPARATOR);
  }

  @Override
  public boolean addSpecialFile(List<FileEntry> fileEntries, String filePath, File file) {
    return false;
  }

  @Override
  public String extractIdFromPath(String filePath) {
    String username = null;
    if (filePath.startsWith(USERS_BASE_PATH)) {
      String filePathEnd = filePath.substring(USERS_BASE_PATH.length());
      username = filePathEnd.substring(0, filePathEnd.indexOf("/"));
    } else {
      throw new IllegalStateException("Incorrect file path (" + filePath + ") must start with /organization/users/");
    }
    return username;
  }
}
