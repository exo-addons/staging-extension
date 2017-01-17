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
 * The Class UserImportResource.
 *
 * @author <a href="mailto:boubaker.khanfir@exoplatform.com">Boubaker
 *         Khanfir</a>
 */
public class UserImportResource extends AbstractJCRImportOperationHandler implements FileImportOperationInterface {
  
  /** The Constant log. */
  private static final Log log = ExoLogger.getLogger(UserImportResource.class);

  /** The Constant USERS_BASE_PATH. */
  private static final String USERS_BASE_PATH = OrganizationManagementExtension.PATH_ORGANIZATION + "/" + OrganizationManagementExtension.PATH_ORGANIZATION_USER + "/";

  /** The organization service. */
  private OrganizationService organizationService = null;

  /**
   * {@inheritDoc}
   */
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

  /**
   * Import user JCR nodes.
   *
   * @param tempFile the temp file
   * @throws Exception the exception
   */
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

  /**
   * Import memberships.
   *
   * @param tempFile the temp file
   * @param newUsers the new users
   * @param replaceExisting the replace existing
   * @throws FileNotFoundException the file not found exception
   * @throws IOException Signals that an I/O exception has occurred.
   * @throws Exception the exception
   */
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

  /**
   * Import users.
   *
   * @param tempFile the temp file
   * @param newUsers the new users
   * @param replaceExisting the replace existing
   * @throws FileNotFoundException the file not found exception
   * @throws IOException Signals that an I/O exception has occurred.
   * @throws Exception the exception
   */
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

  /**
   * Import user profiles.
   *
   * @param tempFile the temp file
   * @param newUsers the new users
   * @param replaceExisting the replace existing
   * @throws FileNotFoundException the file not found exception
   * @throws IOException Signals that an I/O exception has occurred.
   * @throws Exception the exception
   */
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

  /**
   * Creates the user.
   *
   * @param username the username
   * @param zin the zin
   * @param existingUser the existing user
   * @param replaceExisting the replace existing
   * @throws Exception the exception
   */
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

  /**
   * Creates the user profile.
   *
   * @param username the username
   * @param zin the zin
   * @throws Exception the exception
   */
  private void createUserProfile(String username, final ZipInputStream zin) throws Exception {
    UserProfile profile = deserializeObject(zin, organizationService.getUserProfileHandler().createUserProfileInstance().getClass(), "organization");
    organizationService.getUserProfileHandler().saveUserProfile(profile, true);
  }

  /**
   * Creates the membership.
   *
   * @param zin the zin
   * @throws Exception the exception
   */
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

  /**
   * {@inheritDoc}
   */
  @Override
  public String getManagedFilesPrefix() {
    return "organization/user/";
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isUnKnownFileFormat(String filePath) {
    return !filePath.endsWith(".xml") || !filePath.contains(JCRNodeExportTask.JCR_DATA_SEPARATOR);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean addSpecialFile(List<FileEntry> fileEntries, String filePath, File file) {
    return false;
  }

  /**
   * {@inheritDoc}
   */
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
