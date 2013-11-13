package org.exoplatform.management.organization.user;

import com.thoughtworks.xstream.XStream;
import org.apache.poi.util.IOUtils;
import org.exoplatform.management.organization.OrganizationManagementExtension;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
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
import javax.jcr.Session;
import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * @author <a href="mailto:boubaker.khanfir@exoplatform.com">Boubaker
 *         Khanfir</a>
 */
public class UserImportResource implements OperationHandler {
  private static final Log log = ExoLogger.getLogger(UserImportResource.class);
  private OrganizationService organizationService = null;
  private RepositoryService repositoryService = null;
  private NodeHierarchyCreator hierarchyCreator = null;

  private String usersBasePath = OrganizationManagementExtension.PATH_ORGANIZATION + "/" + OrganizationManagementExtension.PATH_ORGANIZATION_USER + "/";

  @Override
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws OperationException {
    if (organizationService == null) {
      organizationService = operationContext.getRuntimeContext().getRuntimeComponent(OrganizationService.class);
    }
    if (repositoryService == null) {
      repositoryService = operationContext.getRuntimeContext().getRuntimeComponent(RepositoryService.class);
    }
    if (hierarchyCreator == null) {
      hierarchyCreator = operationContext.getRuntimeContext().getRuntimeComponent(NodeHierarchyCreator.class);
    }

    InputStream attachmentInputStream = null;

    OperationAttributes attributes = operationContext.getAttributes();
    List<String> filters = attributes.getValues("filter");

    // "replace-existing" attribute. Defaults to false.
    boolean replaceExisting = filters.contains("replace-existing:true");

    List<String> newUsers = new ArrayList<String>();

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
      ZipInputStream zin = new ZipInputStream(new FileInputStream(tempFile));
      ZipEntry entry;
      while ((entry = zin.getNextEntry()) != null) {
        String filePath = entry.getName();
        if (filePath.startsWith(usersBasePath) && filePath.endsWith("user.xml")) {
          log.debug("Parsing : " + filePath);
          String username = extractUserName(filePath);
          User existingUser = organizationService.getUserHandler().findUserByName(username);
          if(existingUser == null) {
            newUsers.add(username);
          }
          createUser(username, zin, existingUser, replaceExisting);
        }
        try {
          zin.closeEntry();
        } catch (Exception e) {
          // Already closed, expected
        }
      }
      zin.close();

      // UserProfile
      zin = new ZipInputStream(new FileInputStream(tempFile));
      while ((entry = zin.getNextEntry()) != null) {
        String filePath = entry.getName();
        if (filePath.startsWith(usersBasePath) && filePath.endsWith("profile.xml")) {
          log.debug("Parsing : " + filePath);
          String username = extractUserName(filePath);
          if(replaceExisting || newUsers.contains(username)) {
            createUserProfile(username, zin);
          }
        }
        try {
          zin.closeEntry();
        } catch (Exception e) {
          // Already closed, expected
        }
      }
      zin.close();

      // Memberships
      zin = new ZipInputStream(new FileInputStream(tempFile));
      while ((entry = zin.getNextEntry()) != null) {
        String filePath = entry.getName();
        if (filePath.startsWith(usersBasePath) && filePath.endsWith("_membership.xml")) {
          log.debug("Parsing : " + filePath);
          String username = extractUserName(filePath);
          if(replaceExisting || newUsers.contains(username)) {
            createMembership(username, zin);
          }
        }
        try {
          zin.closeEntry();
        } catch (Exception e) {
          // Already closed, expected
        }
      }
      zin.close();

      // Content
      zin = new ZipInputStream(new FileInputStream(tempFile));
      while ((entry = zin.getNextEntry()) != null) {
        String filePath = entry.getName();
        if (filePath.startsWith(usersBasePath) && filePath.endsWith("u_content.xml")) {
          log.debug("Parsing : " + filePath);
          String userName = extractUserName(filePath);
          createContent(userName, zin);
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
  }

  /**
   * Extract the username from the file path
   * @param filePath
   * @return
   */
  private String extractUserName(String filePath) {
    String username = null;
    if(filePath.startsWith(usersBasePath)) {
      String filePathEnd = filePath.substring(usersBasePath.length());
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
      User user = deserializeObject(zin, organizationService.getUserHandler().createUserInstance("test").getClass());
      organizationService.getUserHandler().createUser(user, true);
      /*
      if (alreadyExists && replaceExisting) {
        // Delete Memberships to replace it by what is unserialized
        Collection<?> memberships = organizationService.getMembershipHandler().findMembershipsByUser(user.getUserName());
        for (Object membership : memberships) {
          organizationService.getMembershipHandler().removeMembership(((Membership) membership).getId(), true);
        }
        Node userNode = null;
        try {
          SessionProvider sessionProvider = SessionProvider.createSystemProvider();
          userNode = hierarchyCreator.getUserNode(sessionProvider, user.getUserName());
          if (userNode != null) {
            Session session = userNode.getSession();
            userNode.remove();
            session.save();
          }
        } catch (Exception exception) {
          throw new OperationException(OperationNames.IMPORT_RESOURCE, "Error while getting user's personnel folder:", exception);
        }
      }
      */
    } else if(replaceExisting && alreadyExists) {
      log.info("ReplaceExisting is On: Updating user '" + username + "'");
      User user = deserializeObject(zin, organizationService.getUserHandler().createUserInstance("test").getClass());
      organizationService.getUserHandler().saveUser(user, true);
      /*
      // Delete Profile to replace it by what is unserialized
      UserProfile oldUserProfile = organizationService.getUserProfileHandler().findUserProfileByName(user.getUserName());
      if (oldUserProfile != null && replaceExisting) {
        if (alreadyExists) {
          log.info("ReplaceExisting is On: Deleting profile '" + user.getUserName() + "'");
        }
        organizationService.getUserProfileHandler().removeUserProfile(user.getUserName(), true);
        oldUserProfile = null;
      }
      */

      // Delete Memberships to replace it by what is unserialized
      Collection<?> memberships = organizationService.getMembershipHandler().findMembershipsByUser(user.getUserName());
      for (Object membership : memberships) {
        organizationService.getMembershipHandler().removeMembership(((Membership) membership).getId(), true);
      }
      Node userNode = null;
      try {
        SessionProvider sessionProvider = SessionProvider.createSystemProvider();
        userNode = hierarchyCreator.getUserNode(sessionProvider, user.getUserName());
        if (userNode != null) {
          Session session = userNode.getSession();
          userNode.remove();
          session.save();
        }
      } catch (Exception exception) {
        throw new OperationException(OperationNames.IMPORT_RESOURCE, "Error while getting user's personnel folder:", exception);
      }
    } else {
      log.info("ReplaceExisting is Off: Ignoring user '" + username + "'");
    }
  }

  private void createUserProfile(String username, final ZipInputStream zin) throws Exception {
    UserProfile profile = deserializeObject(zin, organizationService.getUserProfileHandler().createUserProfileInstance().getClass());
    organizationService.getUserProfileHandler().saveUserProfile(profile, true);
  }

  @SuppressWarnings("deprecation")
  private void createMembership(String username, final ZipInputStream zin) throws Exception {
    Membership membership = null;
    try {
      membership = deserializeObject(zin, organizationService.getMembershipHandler().createMembershipInstance().getClass());
    } catch (Exception e) {
      log.warn("Can't serialize membership with : " + UserImpl.class.getName() + ". Trying with : " + org.exoplatform.services.organization.idm.UserImpl.class.getName());
      membership = deserializeObject(zin, org.exoplatform.services.organization.idm.MembershipImpl.class);
    }

    Membership oldMembership = organizationService.getMembershipHandler().findMembership(membership.getId());
    if (oldMembership == null) {
      log.info("Membership '" + membership.getMembershipType() + ":" + membership.getGroupId() +"' was not found for user " + membership.getUserName() + ", creating it.");
      Group group = organizationService.getGroupHandler().findGroupById(membership.getGroupId());
      User user = organizationService.getUserHandler().findUserByName(membership.getUserName());
      MembershipType membershipType = organizationService.getMembershipTypeHandler().findMembershipType(membership.getMembershipType());
      organizationService.getMembershipHandler().linkMembership(user, group, membershipType, true);
    } else {
      log.info("Membership '" + membership.getMembershipType() + ":" + membership.getGroupId() +"' already exists for user " + membership.getUserName() + " : ignoring.");
    }
  }

  private void createContent(String username, ZipInputStream zin) throws Exception {
    SessionProvider sessionProvider = SessionProvider.createSystemProvider();

    try {
      // getUserNode() creates the folder if it does not exist
      Node userNode = hierarchyCreator.getUserNode(sessionProvider, username);
      Session session = userNode.getSession();
      String parentPath = userNode.getParent().getPath();
      userNode.remove();
      session.save();

      ByteArrayInputStream bis = new ByteArrayInputStream(IOUtils.toByteArray(zin));
      session.importXML(parentPath, bis, ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
      session.save();
    } catch (Exception exception) {
      throw new OperationException(OperationNames.IMPORT_RESOURCE, "Error while importing user's personal folder of " + username + " : " + exception.getMessage(), exception);
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
