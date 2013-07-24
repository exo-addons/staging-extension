package org.exoplatform.management.organization.user;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.jcr.ImportUUIDBehavior;
import javax.jcr.Node;
import javax.jcr.Session;

import org.apache.poi.util.IOUtils;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.Membership;
import org.exoplatform.services.organization.MembershipType;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserProfile;
import org.exoplatform.services.organization.impl.UserImpl;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.operation.OperationAttachment;
import org.gatein.management.api.operation.OperationAttributes;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.OperationHandler;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.NoResultModel;

import com.thoughtworks.xstream.XStream;

/**
 * @author <a href="mailto:boubaker.khanfir@exoplatform.com">Boubaker
 *         Khanfir</a>
 */
public class UserImportResource implements OperationHandler {
  private static final Log log = ExoLogger.getLogger(UserImportResource.class);
  private OrganizationService organizationService = null;
  private RepositoryService repositoryService = null;
  private NodeHierarchyCreator hierarchyCreator = null;

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
    Boolean replaceExisting = null;

    OperationAttributes attributes = operationContext.getAttributes();
    if (attributes != null && attributes.getValues("filter") != null && !attributes.getValues("filter").isEmpty()) {
      Iterator<String> filters = attributes.getValues("filter").iterator();
      while (filters.hasNext() && replaceExisting == null) {
        String filter = filters.next();
        if (filter.startsWith("replaceExisting:")) {
          replaceExisting = Boolean.parseBoolean(filter.substring("replaceExisting:".length()));
        }
      }
    }
    if (replaceExisting == null) {
      replaceExisting = true;
    }

    // get attachement input stream
    OperationAttachment attachment = operationContext.getAttachment(false);
    attachmentInputStream = attachment.getStream();
    File tempFile = null;
    try {
      tempFile = File.createTempFile("ImportOperationAttachment", ".zip");
      OutputStream fos = new FileOutputStream(tempFile);
      IOUtils.copy(attachmentInputStream, fos);
      fos.close();

      ZipInputStream zin = new ZipInputStream(new FileInputStream(tempFile));
      ZipEntry entry;
      while ((entry = zin.getNextEntry()) != null) {
        String filePath = entry.getName();
        if (filePath.endsWith("user.xml")) {
          log.debug("Parsing : " + filePath);
          createUser(zin, replaceExisting);
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
        if (filePath.endsWith("profile.xml")) {
          log.debug("Parsing : " + filePath);
          createUserProfile(zin);
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
        if (filePath.endsWith("u_content.xml")) {
          log.debug("Parsing : " + filePath);
          String userName = extractUserName(filePath);
          createContent(zin, userName, replaceExisting);
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
      if (tempFile != null) {
        try {
          tempFile.delete();
        } catch (Exception e) {
          tempFile.deleteOnExit();
        }
      }
    }
  }

  private void createContent(ZipInputStream zin, String userName, boolean replaceExisting) throws Exception {
    SessionProvider sessionProvider = SessionProvider.createSystemProvider();
    Node userNode = null;
    try {
      userNode = hierarchyCreator.getUserNode(sessionProvider, userName);
    } catch (Exception exception) {
      throw new OperationException(OperationNames.IMPORT_RESOURCE, "Error while getting user's personnel folder:" + exception.getMessage());
    }
    if (replaceExisting && userNode != null) {
      Session session = userNode.getSession();
      userNode.remove();
      session.save();
      userNode = null;
    }
    if (userNode == null) {
      try {
        userNode = hierarchyCreator.getUserNode(sessionProvider, userName);
        Session session = userNode.getSession();
        String parentPath = userNode.getParent().getPath();
        userNode.remove();
        session.save();
        ByteArrayInputStream bis = new ByteArrayInputStream(IOUtils.toByteArray(zin));
        session.importXML(parentPath, bis, ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
        session.save();
      } catch (Exception exception) {
        throw new OperationException(OperationNames.IMPORT_RESOURCE, "Error while importing user's personnal JCR contents", exception);
      }
    }
  }

  private String extractUserName(String filePath) {
    Pattern pattern = Pattern.compile("users/(.*)/");
    Matcher matcher = pattern.matcher(filePath);
    if (matcher.find()) {
      return matcher.group(1);
    } else {
      throw new IllegalStateException("filePath doesn't match users/*/FILE.xml");
    }
  }

  private void createUser(final ZipInputStream zin, Boolean replaceExisting) throws Exception {
    User user = deserializeObject(zin, organizationService.getUserHandler().createUserInstance("test").getClass());
    User oldUser = organizationService.getUserHandler().findUserByName(user.getUserName());
    boolean alreadyExists = (oldUser != null);
    if (alreadyExists && replaceExisting) {
      log.info("ReplaceExisting is On: Deleting user '" + user.getUserName() + "'");
      organizationService.getUserHandler().removeUser(user.getUserName(), true);
      log.info("ReplaceExisting is On: Deleting profile '" + user.getUserName() + "'");
      organizationService.getUserProfileHandler().removeUserProfile(user.getUserName(), true);
      oldUser = null;
    }
    if (oldUser == null) {
      organizationService.getUserHandler().createUser(user, false);
      // Delete Profile to replace it by what is unserialized
      UserProfile oldUserProfile = organizationService.getUserProfileHandler().findUserProfileByName(user.getUserName());
      if (oldUserProfile != null && replaceExisting) {
        if (alreadyExists) {
          log.info("ReplaceExisting is On: Deleting profile '" + user.getUserName() + "'");
        }
        organizationService.getUserProfileHandler().removeUserProfile(user.getUserName(), true);
        oldUserProfile = null;
      }
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
          throw new OperationException(OperationNames.IMPORT_RESOURCE, "Error while getting user's personnel folder:" + exception.getMessage());
        }
      }
    } else {
      log.info("ReplaceExisting is Off: Ignoring user '" + user.getUserName() + "'");
    }
  }

  private void createUserProfile(final ZipInputStream zin) throws Exception {
    UserProfile profile = deserializeObject(zin, organizationService.getUserProfileHandler().createUserProfileInstance().getClass());
    organizationService.getUserProfileHandler().saveUserProfile(profile, true);
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
      Group group = organizationService.getGroupHandler().findGroupById(membership.getGroupId());
      User user = organizationService.getUserHandler().findUserByName(membership.getUserName());
      MembershipType membershipType = organizationService.getMembershipTypeHandler().findMembershipType(membership.getMembershipType());
      organizationService.getMembershipHandler().linkMembership(user, group, membershipType, true);
    } else {
      log.info("Membership already exists: Ignoring.");
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
