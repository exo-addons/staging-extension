package org.exoplatform.management.social.operations;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.IOUtils;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.management.common.SpaceMetaData;
import org.exoplatform.management.common.exportop.ActivitiesExportTask;
import org.exoplatform.management.common.exportop.SpaceMetadataExportTask;
import org.exoplatform.management.common.importop.AbstractImportOperationHandler;
import org.exoplatform.management.common.importop.ActivityImportOperationInterface;
import org.exoplatform.management.social.SocialExtension;
import org.exoplatform.portal.config.DataStorage;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.portal.config.model.Dashboard;
import org.exoplatform.portal.mop.importer.ImportMode;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.social.common.RealtimeListAccess;
import org.exoplatform.social.core.activity.model.ActivityStream.Type;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.application.SpaceActivityPublisher;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.model.Profile;
import org.exoplatform.social.core.identity.provider.SpaceIdentityProvider;
import org.exoplatform.social.core.manager.ActivityManager;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.model.AvatarAttachment;
import org.exoplatform.social.core.space.SpaceUtils;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.social.core.storage.api.ActivityStorage;
import org.exoplatform.social.core.storage.api.IdentityStorage;
import org.exoplatform.social.core.storage.cache.CachedActivityStorage;
import org.gatein.common.logging.Logger;
import org.gatein.common.logging.LoggerFactory;
import org.gatein.management.api.ContentType;
import org.gatein.management.api.PathAddress;
import org.gatein.management.api.controller.ManagedRequest;
import org.gatein.management.api.controller.ManagedResponse;
import org.gatein.management.api.controller.ManagementController;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.operation.OperationAttachment;
import org.gatein.management.api.operation.OperationAttributes;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.NoResultModel;

import com.thoughtworks.xstream.XStream;

/**
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class SocialDataImportResource extends AbstractImportOperationHandler implements ActivityImportOperationInterface {

  final private static Logger log = LoggerFactory.getLogger(SocialDataImportResource.class);

  final private static String MANAGED_ENTRY_PATH_PREFIX = "social/space/";

  private OrganizationService organizationService;
  private IdentityManager identityManager;
  private ManagementController managementController;
  private DataStorage dataStorage;

  @Override
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws OperationException {
    organizationService = operationContext.getRuntimeContext().getRuntimeComponent(OrganizationService.class);
    spaceService = operationContext.getRuntimeContext().getRuntimeComponent(SpaceService.class);
    managementController = operationContext.getRuntimeContext().getRuntimeComponent(ManagementController.class);
    activityManager = operationContext.getRuntimeContext().getRuntimeComponent(ActivityManager.class);
    identityManager = operationContext.getRuntimeContext().getRuntimeComponent(IdentityManager.class);
    userACL = operationContext.getRuntimeContext().getRuntimeComponent(UserACL.class);
    dataStorage = operationContext.getRuntimeContext().getRuntimeComponent(DataStorage.class);
    activityStorage = operationContext.getRuntimeContext().getRuntimeComponent(ActivityStorage.class);
    identityStorage = operationContext.getRuntimeContext().getRuntimeComponent(IdentityStorage.class);
    activitiesByPostTime.clear();

    OperationAttributes attributes = operationContext.getAttributes();
    List<String> filters = attributes.getValues("filter");

    // "replace-existing" attribute. Defaults to false.
    boolean replaceExisting = filters.contains("replace-existing:true");

    // "create-users" attribute. Defaults to false.
    boolean createAbsentUsers = filters.contains("create-users:true");

    // space-name = spaceDisplayName || spacePrettyName ||
    // spaceOriginalPrettyName (before renaming)
    String spaceName = operationContext.getAddress().resolvePathTemplate("space-name");
    if (spaceName != null) {
      Space space = spaceService.getSpaceByDisplayName(spaceName);
      if (space == null) {
        space = spaceService.getSpaceByPrettyName(spaceName);
        if (space == null) {
          space = spaceService.getSpaceByGroupId(SpaceUtils.SPACE_GROUP + "/" + spaceName);
        }
      }
      if (space != null) {
        if (!replaceExisting) {
          log.info("Space '" + space.getDisplayName() + "' was found but replaceExisting=false. Ignore space '" + spaceName + "' import.");
          resultHandler.completed(NoResultModel.INSTANCE);
          return;
        }
        spaceName = space.getPrettyName();
      }
    }

    File tmpZipFile = null;
    increaseCurrentTransactionTimeOut(operationContext);
    try {
      // Copy attachement to local temporary File
      tmpZipFile = File.createTempFile("staging-social", ".zip");
      Map<String, Map<String, File>> fileToImportByOwner = extractDataFromZipAndCreateSpaces(operationContext.getAttachment(false), spaceName, replaceExisting, createAbsentUsers, tmpZipFile);
      Set<String> spacePrettyNames = fileToImportByOwner.keySet();
      for (String extractedSpacePrettyName : spacePrettyNames) {
        log.info("Importing applications data for space: " + extractedSpacePrettyName + " ...");

        Space space = spaceService.getSpaceByPrettyName(extractedSpacePrettyName);
        if (space == null) {
          continue;
        }

        Map<String, File> spaceFiles = fileToImportByOwner.get(extractedSpacePrettyName);
        Set<String> filesKeys = spaceFiles.keySet();
        List<String> filesKeysList = new ArrayList<String>(filesKeys);
        Collections.sort(filesKeysList, new Comparator<String>() {
          @Override
          public int compare(String o1, String o2) {
            if (o1.contains(SocialDashboardExportTask.FILENAME)) {
              return 1;
            }
            if (o2.contains(SocialDashboardExportTask.FILENAME)) {
              return -1;
            }
            return o1.compareTo(o2);
          }
        });

        List<File> activitiesFileList = new ArrayList<File>();

        for (String fileKey : filesKeysList) {
          File fileToImport = spaceFiles.get(fileKey);
          if (fileKey.equals(SocialExtension.ANSWER_RESOURCE_PATH) || fileKey.equals(SocialExtension.CALENDAR_RESOURCE_PATH) || fileKey.equals(SocialExtension.CONTENT_RESOURCE_PATH)
              || fileKey.equals(SocialExtension.FAQ_RESOURCE_PATH) || fileKey.equals(SocialExtension.FORUM_RESOURCE_PATH) || fileKey.equals(SocialExtension.WIKI_RESOURCE_PATH)
              || fileKey.equals(SocialExtension.SITES_IMPORT_RESOURCE_PATH)) {
            importSubResource(fileToImport, fileKey);
            deleteTempFile(fileToImport);
          } else {
            if (fileKey.contains(ActivitiesExportTask.FILENAME)) {
              activitiesFileList.add(fileToImport);
            } else if (fileToImport.getAbsolutePath().contains(SocialDashboardExportTask.FILENAME)) {
              updateDashboard(space.getGroupId(), fileToImport);
              deleteTempFile(fileToImport);
            } else if (fileToImport.getAbsolutePath().contains(SpaceAvatarExportTask.FILENAME)) {
              updateAvatar(space, fileToImport);
              deleteTempFile(fileToImport);
            } else {
              log.warn("Cannot handle file: " + fileToImport.getAbsolutePath() + ". Ignore it.");
              deleteTempFile(fileToImport);
            }
          }
        }

        log.info("Importing space '" + extractedSpacePrettyName + "' activities.");
        activitiesByPostTime.clear();
        for (File file : activitiesFileList) {
          importActivities(file, extractedSpacePrettyName, false);
        }

        log.info("Import operation finished successfully for space: " + extractedSpacePrettyName);
      }
      log.info("Import operation finished successfully for all space.");
    } catch (IOException e) {
      log.warn("Cannot create temporary file.", e);
    } finally {
      restoreDefaultTransactionTimeOut(operationContext);
      if (tmpZipFile != null) {
        String tempFolderPath = tmpZipFile.getAbsolutePath().replaceAll("\\.zip$", "");
        File tempFolderFile = new File(tempFolderPath);
        deleteTempFile(tempFolderFile);
        deleteTempFile(tmpZipFile);
      }
    }

    resultHandler.completed(NoResultModel.INSTANCE);
  }

  private void deleteSpaceActivities(String extractedSpacePrettyName) {
    Identity spaceIdentity = identityManager.getOrCreateIdentity(SpaceIdentityProvider.NAME, extractedSpacePrettyName, false);
    RealtimeListAccess<ExoSocialActivity> listAccess = activityManager.getActivitiesOfSpaceWithListAccess(spaceIdentity);

    // To refresh activities list from storage
    listAccess.getNumberOfUpgrade();

    if (listAccess.getSize() > 0) {
      ExoSocialActivity[] activities = listAccess.load(0, listAccess.getSize());
      log.info("Delete " + listAccess.getSize() + " space activities");
      deleteActivities(activities);
    }
    RequestLifeCycle.end();
    RequestLifeCycle.begin(PortalContainer.getInstance());
    if (activityStorage instanceof CachedActivityStorage) {
      ((CachedActivityStorage) activityStorage).clearCache();
    }
  }

  private void updateAvatar(Space space, File fileToImport) {
    log.info("Update Space avatar '" + space.getDisplayName() + "'");

    FileInputStream inputStream = null;
    try {
      inputStream = new FileInputStream(fileToImport);
      InputStreamReader reader = new InputStreamReader(inputStream, "UTF-8");

      space = spaceService.getSpaceByGroupId(space.getGroupId());

      XStream xstream = new XStream();
      AvatarAttachment avatarAttachment = (AvatarAttachment) xstream.fromXML(reader);
      space.setAvatarAttachment(avatarAttachment);

      fixSpaceEditor(space);

      space = spaceService.updateSpace(space);
      space = spaceService.getSpaceByGroupId(space.getGroupId());

      fixSpaceEditor(space);

      if (space.getAvatarAttachment() == null) {
        space.setAvatarAttachment(avatarAttachment);
      }
      spaceService.updateSpaceAvatar(space);
    } catch (Exception e) {
      log.error("Error while updating Space '" + space.getDisplayName() + "' avatar.", e);
    } finally {
      if (inputStream != null) {
        try {
          inputStream.close();
        } catch (IOException e) {
          log.warn("Cannot close input stream: " + fileToImport.getAbsolutePath() + ". Ignore non blocking operation.");
        }
      }
    }
  }

  private void fixSpaceEditor(Space space) {
    if (space.getEditor() == null) {
      // Fix space editor that is always null
      space.setEditor(space.getManagers()[0]);
    }
  }

  private void updateDashboard(String spaceGroupId, File fileToImport) {
    FileInputStream inputStream = null;
    try {
      Dashboard dashboard = SocialDashboardExportTask.getDashboard(dataStorage, spaceGroupId);
      if (dashboard == null) {
        return;
      }

      inputStream = new FileInputStream(fileToImport);

      XStream xstream = new XStream();
      Dashboard newDashboard = (Dashboard) xstream.fromXML(inputStream);

      dashboard.setAccessPermissions(newDashboard.getAccessPermissions());
      dashboard.setChildren(newDashboard.getChildren());
      dashboard.setDecorator(newDashboard.getDecorator());
      dashboard.setDescription(newDashboard.getDescription());
      dashboard.setFactoryId(newDashboard.getFactoryId());
      dashboard.setHeight(newDashboard.getHeight());
      dashboard.setIcon(newDashboard.getIcon());
      dashboard.setName(newDashboard.getName());
      dashboard.setTemplate(newDashboard.getTemplate());
      dashboard.setTitle(newDashboard.getTitle());
      dashboard.setWidth(newDashboard.getWidth());

      dataStorage.saveDashboard(newDashboard);
    } catch (Exception e) {
      throw new OperationException(OperationNames.IMPORT_RESOURCE, "Error while updating Space '" + spaceGroupId + "' dashbord.", e);
    } finally {
      if (inputStream != null) {
        try {
          inputStream.close();
        } catch (IOException e) {
          log.warn("Cannot close input stream: " + fileToImport.getAbsolutePath() + ". Ignore non blocking operation.");
        }
      }
    }
  }

  public void attachActivityToEntity(ExoSocialActivity activity, ExoSocialActivity comment) throws Exception {
    Identity spaceIdentity = getIdentity(activity.getStreamOwner());
    if (SpaceActivityPublisher.SPACE_PROFILE_ACTIVITY.equals(activity.getType()) || SpaceActivityPublisher.USER_ACTIVITIES_FOR_SPACE.equals(activity.getType())) {
      if (comment != null) {
        activity = comment;
      }
      identityStorage.updateProfileActivityId(spaceIdentity, activity.getId(), Profile.AttachedActivityType.SPACE);
    }
  }

  public boolean isActivityNotValid(ExoSocialActivity activity, ExoSocialActivity comment) throws Exception {
    if (comment == null) {
      boolean notValidActivity = activity.getActivityStream() == null || !activity.getActivityStream().getType().equals(Type.SPACE);
      if (notValidActivity) {
        log.warn("NOT Valid space activity: '" + activity.getTitle() + "'");
      }
      return notValidActivity;
    } else {
      return false;
    }
  }

  private boolean createOrReplaceSpace(String spacePrettyName, String targetSpaceName, boolean replaceExisting, boolean createAbsentUsers, InputStream inputStream) throws Exception {
    // Unmarshall metadata xml file
    XStream xstream = new XStream();
    xstream.alias("metadata", SpaceMetaData.class);
    SpaceMetaData spaceMetaData = (SpaceMetaData) xstream.fromXML(inputStream);
    String newSpacePrettyName = spaceMetaData.getPrettyName();
    String oldSpacePrettyName = spaceMetaData.getGroupId().replace(SpaceUtils.SPACE_GROUP + "/", "");

    // If selected space doesn't fit the found space, ignore it
    if (targetSpaceName != null && !(targetSpaceName.equals(spaceMetaData.getDisplayName()) || targetSpaceName.equals(oldSpacePrettyName) || targetSpaceName.equals(newSpacePrettyName))) {
      return true;
    }

    Space space = spaceService.getSpaceByGroupId(spaceMetaData.getGroupId());
    if (space != null) {
      if (replaceExisting) {
        String groupId = space.getGroupId();

        deleteSpaceActivities(spaceMetaData.getPrettyName());

        log.info("Delete space: '" + spaceMetaData.getPrettyName() + "'.");
        RequestLifeCycle.begin(PortalContainer.getInstance());
        try {
          String[] members = space.getMembers();
          for (String member : members) {
            spaceService.removeMember(space, member);
          }
          spaceService.deleteSpace(space);
        } finally {
          RequestLifeCycle.end();
        }

        // FIXME Workaround: deleting a space don't delete the corresponding
        // group
        RequestLifeCycle.begin(PortalContainer.getInstance());
        try {
          Group group = organizationService.getGroupHandler().findGroupById(groupId);
          if (group != null) {
            organizationService.getGroupHandler().removeGroup(group, true);
          }
        } finally {
          RequestLifeCycle.end();
        }

        // FIXME Answer Bug: deleting a space don't delete answers category, but
        // it will be deleted if answer data is imported

      } else {
        log.info("Space '" + space.getDisplayName() + "' was found but replaceExisting=false. Ignore space import.");
        return true;
      }
    }

    if (createAbsentUsers) {
      log.info("Create not found users of space: '" + spaceMetaData.getPrettyName() + "'.");
      String[] members = spaceMetaData.getMembers();
      for (String member : members) {
        RequestLifeCycle.begin(PortalContainer.getInstance());
        try {
          User user = organizationService.getUserHandler().findUserByName(member);
          if (user == null) {
            user = organizationService.getUserHandler().createUserInstance(member);
            user.setDisplayName(member);
            user.setFirstName(member);
            user.setLastName(member);
            user.setEmail(member + "@example.com");
            user.setPassword("" + Math.random());
            log.info("     Create not found user of space: '" + member + "' with random password.");
            try {
              organizationService.getUserHandler().createUser(user, true);
            } catch (RuntimeException e) {
              // The user JCR data are already in the JCR
              if (null == organizationService.getUserHandler().findUserByName(member)) {
                log.warn("Exception while creating the user '" + member + "' with broadcasting event, try without broadcast", e);
                organizationService.getUserHandler().createUser(user, false);
              } else {
                throw e;
              }
            }
          }
        } catch (Exception e) {
          log.warn("Exception while creating the user: " + member, e);
        } finally {
          RequestLifeCycle.end();
        }
      }
    }

    log.info("Create new space: '" + spaceMetaData.getPrettyName() + "'.");
    space = new Space();

    boolean isRenamed = !newSpacePrettyName.equals(oldSpacePrettyName);

    space.setPrettyName(oldSpacePrettyName);
    if (isRenamed) {
      space.setDisplayName(oldSpacePrettyName);
    } else {
      space.setDisplayName(spaceMetaData.getDisplayName());
    }
    space.setGroupId(spaceMetaData.getGroupId());
    space.setTag(spaceMetaData.getTag());
    space.setApp(spaceMetaData.getApp());

    // Filter on existing users
    String[] members = getExistingUsers(spaceMetaData.getMembers());
    if (members == null || members.length == 0) {
      members = new String[] { userACL.getSuperUser() };
      log.warn("Members of space '" + spaceMetaData.getDisplayName() + "' is empty, the super user '" + Arrays.toString(space.getMembers()) + "' will be used instead.");
    }

    String[] managers = getExistingUsers(spaceMetaData.getManagers());
    if (managers == null || managers.length == 0) {
      managers = new String[] { userACL.getSuperUser() };
      log.warn("Managers of space '" + spaceMetaData.getDisplayName() + "' is empty, the super user '" + Arrays.toString(space.getManagers()) + "' will be used instead.");
    }

    String[] editor = getExistingUsers(spaceMetaData.getEditor());
    if (editor == null || editor.length == 0) {
      editor = new String[] { managers[0] };
    }
    space.setEditor(editor[0]);

    space.setInvitedUsers(getExistingUsers(spaceMetaData.getInvitedUsers()));

    space.setRegistration(spaceMetaData.getRegistration());
    space.setDescription(spaceMetaData.getDescription());
    space.setType(spaceMetaData.getType());
    space.setVisibility(spaceMetaData.getVisibility());
    space.setPriority(spaceMetaData.getPriority());
    space.setUrl(spaceMetaData.getUrl());

    RequestLifeCycle.begin(PortalContainer.getInstance());
    try {
      space = spaceService.createSpace(space, space.getEditor());
      if (isRenamed) {
        log.info("Rename space from '" + oldSpacePrettyName + "' to '" + newSpacePrettyName + "'.");
        spaceService.renameSpace(space, spaceMetaData.getDisplayName().trim());
        space = spaceService.getSpaceByDisplayName(spaceMetaData.getDisplayName());
      }
      if (space == null) {
        throw new IllegalStateException("Space '" + spaceMetaData.getDisplayName() + "' was not found after creating it.");
      }
    } finally {
      RequestLifeCycle.end();
    }

    // FIXME Workaround, after replacing space, it still using flag
    // deleted=false
    RequestLifeCycle.begin(PortalContainer.getInstance());
    try {
      Identity identity = null;
      int countIerations = 0;
      // Wait until the identity of the space is committed
      do {
        identity = getIdentity(space.getPrettyName());
        if (identity == null) {
          log.warn("Identity of space '" + spaceMetaData.getPrettyName() + "' not found, retry getting it.");
          Thread.sleep(2000);
          countIerations++;
        }
      } while (identity == null && countIerations < 5);
      if (identity == null) {
        throw new OperationException(OperationNames.IMPORT_RESOURCE, "Identity of space with pretty name '" + spaceMetaData.getPrettyName() + "' was not found. Cannot continue importing the space.");
      }
      if (identity.isDeleted()) {
        log.info("Set space identity not deleted, it was deleted=" + spaceMetaData.getPrettyName() + "'.");
        identity.setDeleted(false);
        identityStorage.saveIdentity(identity);
      }
    } finally {
      RequestLifeCycle.end();
    }

    RequestLifeCycle.begin(PortalContainer.getInstance());
    try {
      log.info("Add members to space: '" + spaceMetaData.getPrettyName() + "'.");

      space.setEditor(managers[0]);
      space.setMembers(members);
      space = spaceService.updateSpace(space);

      for (String member : members) {
        try {
          SpaceUtils.addUserToGroupWithMemberMembership(member, space.getGroupId());
          log.info("  User '" + member + "' added to space as member: '" + spaceMetaData.getPrettyName() + "'.");
        } catch (Exception e) {
          log.warn("  Cannot add member '" + member + "' to space: " + space.getPrettyName(), e);
        }
      }
    } finally {
      RequestLifeCycle.end();
    }

    RequestLifeCycle.begin(PortalContainer.getInstance());
    try {
      log.info("Set manager(s) of space: '" + spaceMetaData.getPrettyName() + "'.");

      space.setEditor(managers[0]);
      space.setManagers(managers);
      space = spaceService.updateSpace(space);

      for (String manager : managers) {
        try {
          log.info("  User '" + manager + "' promoted to manager of space: '" + spaceMetaData.getPrettyName() + "'.");
          SpaceUtils.addUserToGroupWithManagerMembership(manager, space.getGroupId());

        } catch (Exception e) {
          log.warn("  Cannot add manager '" + manager + "' to space: " + space.getPrettyName(), e);
        }
      }
    } finally {
      RequestLifeCycle.end();
    }

    deleteSpaceActivities(spaceMetaData.getPrettyName());
    return false;
  }

  private String[] getExistingUsers(String... users) {
    Set<String> existingUsers = new HashSet<String>();
    if (users != null && users.length > 0) {
      for (String userId : users) {
        if (userId == null || userId.isEmpty()) {
          continue;
        }
        try {
          User user = organizationService.getUserHandler().findUserByName(userId);
          if (user != null) {
            existingUsers.add(userId);
          } else {
            log.info("   User '" + userId + "' doesn't exist, the user will not be added as member or manager of the space.");
          }
        } catch (Exception e) {
          log.warn("Exception while attempting to get user: " + userId, e);
        }
      }
    }
    return existingUsers.toArray(EMPTY_STRING_ARRAY);
  }

  private void importSubResource(File tempFile, String subResourcePath) {
    Map<String, List<String>> attributesMap = new HashMap<String, List<String>>();
    attributesMap.put("filter", Collections.singletonList("replace-existing:true"));
    attributesMap.put("importMode", Collections.singletonList(ImportMode.OVERWRITE.name()));

    // This will be closed in sub resources, don't close it here
    InputStream inputStream = null;
    try {
      inputStream = new FileInputStream(tempFile);
      ManagedRequest request = ManagedRequest.Factory.create(OperationNames.IMPORT_RESOURCE, PathAddress.pathAddress(subResourcePath), attributesMap, inputStream, ContentType.ZIP);
      ManagedResponse response = managementController.execute(request);
      Object model = response.getResult();
      if (!(model instanceof NoResultModel)) {
        throw new OperationException(OperationNames.IMPORT_RESOURCE, "Unknown error while importing to path: " + subResourcePath);
      }
    } catch (FileNotFoundException e) {
      throw new OperationException(OperationNames.IMPORT_RESOURCE, "Cannot find extracted file: " + (tempFile != null ? tempFile.getAbsolutePath() : tempFile), e);
    } finally {
      if (inputStream != null) {
        try {
          inputStream.close();
        } catch (IOException e) {
          log.warn("Cannot close input stream: " + tempFile + ". Ignore non blocking operation.");
        }
      }
    }
  }

  private Map<String, Map<String, File>> extractDataFromZipAndCreateSpaces(OperationAttachment attachment, String spaceName, boolean replaceExisting, boolean createAbsentUsers, File tmpZipFile)
      throws OperationException {
    if (attachment == null || attachment.getStream() == null) {
      throw new OperationException(OperationNames.IMPORT_RESOURCE, "No attachment available for Social import.");
    }
    InputStream inputStream = attachment.getStream();
    try {
      copyAttachementToLocalFolder(inputStream, tmpZipFile);

      // Organize File paths by id and extract files from zip to a temp
      // folder and create spaces
      return extractFilesByIdAndCreateSpaces(tmpZipFile, spaceName, replaceExisting, createAbsentUsers);
    } catch (Exception e) {
      throw new OperationException(OperationNames.IMPORT_RESOURCE, "Error occured while handling attachement", e);
    } finally {
      if (inputStream != null) {
        try {
          inputStream.close();
        } catch (IOException e) {
          log.warn("Cannot close input stream.");
        }
      }
    }
  }

  private Map<String, Map<String, File>> extractFilesByIdAndCreateSpaces(File tmpZipFile, String targetSpaceName, boolean replaceExisting, boolean createAbsentUsers) throws Exception {
    // Get path of folder where to unzip files
    String targetFolderPath = tmpZipFile.getAbsolutePath().replaceAll("\\.zip$", "") + "/";

    // Open an input stream on local zip file
    NonCloseableZipInputStream zis = new NonCloseableZipInputStream(new FileInputStream(tmpZipFile));

    Map<String, Map<String, File>> filesToImportByOwner = new HashMap<String, Map<String, File>>();
    List<String> ignoredSpaces = new ArrayList<String>();
    try {
      Map<String, ZipOutputStream> zipOutputStreamMap = new HashMap<String, ZipOutputStream>();
      ZipEntry entry;
      while ((entry = zis.getNextEntry()) != null) {
        String zipEntryPath = entry.getName();
        // Skip entries not managed by this extension
        if (!zipEntryPath.startsWith(MANAGED_ENTRY_PATH_PREFIX)) {
          continue;
        }

        // Skip directories
        if (entry.isDirectory()) {
          // Create directory in unzipped folder location
          createFile(new File(targetFolderPath + replaceSpecialChars(zipEntryPath)), true);
          continue;
        }
        int idBeginIndex = MANAGED_ENTRY_PATH_PREFIX.length();
        String spacePrettyName = zipEntryPath.substring(idBeginIndex, zipEntryPath.indexOf("/", idBeginIndex));
        if (ignoredSpaces.contains(spacePrettyName)) {
          continue;
        }

        if (!filesToImportByOwner.containsKey(spacePrettyName)) {
          filesToImportByOwner.put(spacePrettyName, new HashMap<String, File>());
        }
        Map<String, File> ownerFiles = filesToImportByOwner.get(spacePrettyName);

        log.info("Receiving content " + zipEntryPath);

        if (zipEntryPath.contains(SocialExtension.ANSWER_RESOURCE_PATH)) {
          putSubResourceEntry(tmpZipFile, targetFolderPath, zis, zipOutputStreamMap, zipEntryPath, spacePrettyName, ownerFiles, SocialExtension.ANSWER_RESOURCE_PATH);
        } else if (zipEntryPath.contains(SocialExtension.CALENDAR_RESOURCE_PATH)) {
          putSubResourceEntry(tmpZipFile, targetFolderPath, zis, zipOutputStreamMap, zipEntryPath, spacePrettyName, ownerFiles, SocialExtension.CALENDAR_RESOURCE_PATH);
        } else if (zipEntryPath.contains(SocialExtension.CONTENT_RESOURCE_PATH)) {
          putSubResourceEntry(tmpZipFile, targetFolderPath, zis, zipOutputStreamMap, zipEntryPath, spacePrettyName, ownerFiles, SocialExtension.CONTENT_RESOURCE_PATH);
        } else if (zipEntryPath.contains(SocialExtension.FAQ_RESOURCE_PATH)) {
          putSubResourceEntry(tmpZipFile, targetFolderPath, zis, zipOutputStreamMap, zipEntryPath, spacePrettyName, ownerFiles, SocialExtension.FAQ_RESOURCE_PATH);
        } else if (zipEntryPath.contains(SocialExtension.FORUM_RESOURCE_PATH)) {
          putSubResourceEntry(tmpZipFile, targetFolderPath, zis, zipOutputStreamMap, zipEntryPath, spacePrettyName, ownerFiles, SocialExtension.FORUM_RESOURCE_PATH);
        } else if (zipEntryPath.contains(SocialExtension.WIKI_RESOURCE_PATH)) {
          putSubResourceEntry(tmpZipFile, targetFolderPath, zis, zipOutputStreamMap, zipEntryPath, spacePrettyName, ownerFiles, SocialExtension.WIKI_RESOURCE_PATH);
        } else if (zipEntryPath.contains(SocialExtension.GROUP_SITE_RESOURCE_PATH)) {
          putSubResourceEntry(tmpZipFile, targetFolderPath, zis, zipOutputStreamMap, zipEntryPath, spacePrettyName, ownerFiles, SocialExtension.SITES_IMPORT_RESOURCE_PATH);
        } else {
          String localFilePath = targetFolderPath + replaceSpecialChars(zipEntryPath);
          if (localFilePath.endsWith(SpaceMetadataExportTask.FILENAME)) {
            // Create space here to be sure that it's created before importing
            // other application resources
            boolean toIgnore = createOrReplaceSpace(spacePrettyName, targetSpaceName, replaceExisting, createAbsentUsers, zis);
            if (toIgnore) {
              ignoredSpaces.add(spacePrettyName);
              filesToImportByOwner.remove(spacePrettyName);
            }
          } else {
            ownerFiles.put(zipEntryPath, new File(localFilePath));

            // Put file Export file in temp folder
            copyToDisk(zis, localFilePath);
          }
        }
        zis.closeEntry();
      }

      Collection<ZipOutputStream> zipOutputStreams = zipOutputStreamMap.values();
      for (ZipOutputStream zipOutputStream : zipOutputStreams) {
        zipOutputStream.close();
      }
    } finally {
      try {
        zis.reallyClose();
      } catch (Exception e) {
        log.warn("Cannot delete temporary file " + tmpZipFile + ". Ignore it.");
      }
    }

    return filesToImportByOwner;
  }

  private void putSubResourceEntry(File tmpZipFile, String targetFolderPath, NonCloseableZipInputStream zis, Map<String, ZipOutputStream> zipOutputStreamMap, String zipEntryPath,
      String spacePrettyName, Map<String, File> ownerFiles, String subResourcePath) throws Exception {
    if (!ownerFiles.containsKey(subResourcePath)) {
      createFile(new File(targetFolderPath + "social/space/" + spacePrettyName + subResourcePath), true);
      String zipFilePath = targetFolderPath + "social/space/" + spacePrettyName + subResourcePath + ".zip";
      ownerFiles.put(subResourcePath, new File(zipFilePath));
      ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFilePath));
      zipOutputStreamMap.put(spacePrettyName + subResourcePath, zos);
    }
    ZipOutputStream zos = zipOutputStreamMap.get(spacePrettyName + subResourcePath);
    String subResourceZipEntryPath = zipEntryPath.replace("social/space/" + spacePrettyName + "/", "");
    zos.putNextEntry(new ZipEntry(subResourceZipEntryPath));
    IOUtils.copy(zis, zos);
    zos.closeEntry();
  }

}
