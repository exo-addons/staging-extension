package org.exoplatform.management.social.operations;

import java.io.BufferedOutputStream;
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
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.management.social.SocialExtension;
import org.exoplatform.portal.config.DataStorage;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.portal.config.model.Dashboard;
import org.exoplatform.portal.mop.importer.ImportMode;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.social.common.RealtimeListAccess;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.application.SpaceActivityPublisher;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.model.Profile;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
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
import org.gatein.management.api.operation.OperationHandler;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.NoResultModel;

import com.thoughtworks.xstream.XStream;

/**
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class SocialDataImportResource implements OperationHandler {

  private static final String[] EMPTY_STRING_ARRAY = new String[0];

  final private static Logger log = LoggerFactory.getLogger(SocialDataImportResource.class);

  final private static int BUFFER = 2048000;

  private OrganizationService organizationService;
  private SpaceService spaceService;
  private IdentityStorage identityStorage;
  private ActivityManager activityManager;
  private IdentityManager identityManager;
  private ManagementController managementController;
  private UserACL userACL;
  private DataStorage dataStorage;
  private ActivityStorage activityStorage;

  // This is used to test on duplicated activities
  private Set<Long> activityPostTime = new HashSet<Long>();

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
    activityPostTime.clear();

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
            if (fileKey.contains(SpaceActivitiesExportTask.FILENAME)) {
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

        for (File file : activitiesFileList) {
          createActivities(extractedSpacePrettyName, file);
          deleteTempFile(file);
        }

        log.info("Import operation finished successfully for space: " + extractedSpacePrettyName);
      }
      log.info("Import operation finished successfully for all space.");
    } catch (IOException e) {
      log.warn("Cannot create temporary file.", e);
    } finally {
      if (tmpZipFile != null) {
        try {
          String tempFolderPath = tmpZipFile.getAbsolutePath().replaceAll("\\.zip$", "");
          File tempFolderFile = new File(tempFolderPath);
          if (tempFolderFile.exists()) {
            FileUtils.deleteDirectory(tempFolderFile);
          }
          FileUtils.forceDelete(tmpZipFile);
        } catch (Exception e) {
          log.warn("Unable to delete temp file: " + tmpZipFile.getAbsolutePath() + ". Not blocker.");
          tmpZipFile.deleteOnExit();
        }
      }
    }

    resultHandler.completed(NoResultModel.INSTANCE);
  }

  private void deleteTempFile(File fileToImport) {
    try {
      FileUtils.forceDelete(fileToImport);
    } catch (Exception e) {
      log.warn("Cannot delete temporary file from disk: " + fileToImport.getAbsolutePath() + ". It seems we have an opened InputStream. Anyway, it's not blocker.", e);
    }
  }

  private void deleteSpaceActivities(String extractedSpacePrettyName) {
    if (activityStorage instanceof CachedActivityStorage) {
      ((CachedActivityStorage) activityStorage).clearCache();
    }
    Identity spaceIdentity = identityManager.getOrCreateIdentity(SpaceIdentityProvider.NAME, extractedSpacePrettyName, false);
    RealtimeListAccess<ExoSocialActivity> listAccess = activityManager.getActivitiesOfSpaceWithListAccess(spaceIdentity);
    ExoSocialActivity[] activities = listAccess.load(0, listAccess.getSize());
    for (ExoSocialActivity activity : activities) {
      RealtimeListAccess<ExoSocialActivity> commentsListAccess = activityManager.getCommentsWithListAccess(activity);
      if (commentsListAccess.getSize() > 0) {
        List<ExoSocialActivity> comments = commentsListAccess.loadAsList(0, commentsListAccess.getSize());
        for (ExoSocialActivity commentActivity : comments) {
          activityManager.deleteActivity(commentActivity);
        }
      }
      log.info("Delete activity : " + activity.getTitle());
      activityManager.deleteActivity(activity);
    }
  }

  private void updateAvatar(Space space, File fileToImport) {
    log.info("Update Space avatar '" + space.getDisplayName() + "'");

    FileInputStream inputStream = null;
    try {
      inputStream = new FileInputStream(fileToImport);
      InputStreamReader reader = new InputStreamReader(inputStream, "UTF-8");

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

  @SuppressWarnings("unchecked")
  private void createActivities(String spacePrettyName, File activitiesFile) {
    log.info("Importing space '" + spacePrettyName + "' activities.");
    List<ExoSocialActivity> activities = null;
    FileInputStream inputStream = null;
    try {
      inputStream = new FileInputStream(activitiesFile);
      // Unmarshall metadata xml file
      XStream xstream = new XStream();

      activities = (List<ExoSocialActivity>) xstream.fromXML(inputStream);
    } catch (FileNotFoundException e) {
      throw new OperationException(OperationNames.IMPORT_RESOURCE, "Cannot find extracted file: " + (activitiesFile != null ? activitiesFile.getAbsolutePath() : activitiesFile), e);
    } finally {
      if (inputStream != null) {
        try {
          inputStream.close();
        } catch (IOException e) {
          log.warn("Cannot close input stream: " + activitiesFile.getAbsolutePath() + ". Ignore non blocking operation.");
        }
      }
    }
    List<ExoSocialActivity> activitiesList = new ArrayList<ExoSocialActivity>();
    Identity identity = null;
    for (ExoSocialActivity activity : activities) {
      if (activity.getPostedTime() != null && activityPostTime.contains(activity.getPostedTime())) {
        log.info("Ignore duplicated Activity '" + activity.getTitle() + "'.");
        continue;
      } else {
        activityPostTime.add(activity.getPostedTime());
      }
      identity = getIdentity(activity.getUserId());

      if (identity != null) {
        activity.setUserId(identity.getId());

        identity = getIdentity(activity.getPosterId());

        if (identity != null) {
          activity.setPosterId(identity.getId());
          activitiesList.add(activity);

          Set<String> keys = activity.getTemplateParams().keySet();
          for (String key : keys) {
            String value = activity.getTemplateParams().get(key);
            if (value != null) {
              activity.getTemplateParams().put(key, StringEscapeUtils.unescapeHtml(value));
            }
          }
          if (StringUtils.isNotEmpty(activity.getTitle())) {
            activity.setTitle(StringEscapeUtils.unescapeHtml(activity.getTitle()));
          }
          if (StringUtils.isNotEmpty(activity.getBody())) {
            activity.setBody(StringEscapeUtils.unescapeHtml(activity.getBody()));
          }
          if (StringUtils.isNotEmpty(activity.getSummary())) {
            activity.setSummary(StringEscapeUtils.unescapeHtml(activity.getSummary()));
          }
        }
        activity.setReplyToId(null);
        String[] commentedIds = activity.getCommentedIds();
        if (commentedIds != null && commentedIds.length > 0) {
          for (int i = 0; i < commentedIds.length; i++) {
            identity = getIdentity(commentedIds[i]);
            if (identity != null) {
              commentedIds[i] = identity.getId();
            }
          }
          activity.setCommentedIds(commentedIds);
        }
        String[] mentionedIds = activity.getMentionedIds();
        if (mentionedIds != null && mentionedIds.length > 0) {
          for (int i = 0; i < mentionedIds.length; i++) {
            identity = getIdentity(mentionedIds[i]);
            if (identity != null) {
              mentionedIds[i] = identity.getId();
            }
          }
          activity.setMentionedIds(mentionedIds);
        }
        String[] likeIdentityIds = activity.getLikeIdentityIds();
        if (likeIdentityIds != null && likeIdentityIds.length > 0) {
          for (int i = 0; i < likeIdentityIds.length; i++) {
            identity = getIdentity(likeIdentityIds[i]);
            if (identity != null) {
              likeIdentityIds[i] = identity.getId();
            }
          }
          activity.setLikeIdentityIds(likeIdentityIds);
        }
      } else {
        log.warn("Space activity : '" + activity.getTitle() + "' isn't imported because the associated user '" + activity.getUserId() + "' wasn't found.");
      }
    }
    Identity spaceIdentity = getIdentity(spacePrettyName);
    ExoSocialActivity parentActivity = null;
    for (ExoSocialActivity exoSocialActivity : activitiesList) {
      try {
        exoSocialActivity.setId(null);
        if (exoSocialActivity.isComment()) {
          if (parentActivity == null) {
            log.warn("Attempt to add Social activity comment to a null activity");
          } else {
            saveComment(parentActivity, exoSocialActivity);
          }
        } else {
          parentActivity = null;
          saveActivity(exoSocialActivity, spaceIdentity);
          if (exoSocialActivity.getId() == null) {
            log.warn("Activity '" + exoSocialActivity.getTitle() + "' is not imported, id is null");
            continue;
          }
          if (SpaceActivityPublisher.SPACE_PROFILE_ACTIVITY.equals(exoSocialActivity.getType()) || SpaceActivityPublisher.USER_ACTIVITIES_FOR_SPACE.equals(exoSocialActivity.getType())) {
            identityStorage.updateProfileActivityId(spaceIdentity, exoSocialActivity.getId(), Profile.AttachedActivityType.SPACE);
          }
          parentActivity = activityManager.getActivity(exoSocialActivity.getId());
        }
      } catch (Exception e) {
        log.warn("Error while adding activity: " + exoSocialActivity.getTitle(), e);
      }
    }
  }

  private void saveActivity(ExoSocialActivity activity, Identity spaceIdentity) {
    long updatedTime = activity.getUpdated().getTime();
    activityManager.saveActivityNoReturn(spaceIdentity, activity);
    activity.setUpdated(updatedTime);
    activityManager.updateActivity(activity);
    log.info("Space activity : '" + activity.getTitle() + " is imported.");
  }

  private void saveComment(ExoSocialActivity activity, ExoSocialActivity comment) {
    long updatedTime = activity.getUpdated().getTime();
    if (activity.getId() == null) {
      log.warn("Parent activity '" + activity.getTitle() + "' has a null ID, cannot import activity comment '" + comment.getTitle() + "'.");
      return;
    }
    activity = activityManager.getActivity(activity.getId());
    activityManager.saveComment(activity, comment);
    activity = activityManager.getActivity(activity.getId());
    activity.setUpdated(updatedTime);
    activityManager.updateActivity(activity);
    log.info("Space activity comment: '" + activity.getTitle() + " is imported.");
  }

  private Identity getIdentity(String userId) {
    Identity userIdentity = identityStorage.findIdentity(OrganizationIdentityProvider.NAME, userId);
    try {
      if (userIdentity != null) {
        return userIdentity;
      } else {
        Identity spaceIdentity = identityStorage.findIdentity(SpaceIdentityProvider.NAME, userId);

        // Try to see if space was renamed
        if (spaceIdentity == null) {
          Space space = spaceService.getSpaceByGroupId(SpaceUtils.SPACE_GROUP + "/" + userId);
          spaceIdentity = getIdentity(space.getPrettyName());
        }

        return spaceIdentity;
      }
    } catch (Exception e) {
      log.error(e);
    }
    return null;
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
        spaceService.deleteSpace(space);

        // FIXME Workaround: deleting a space don't delete the corresponding
        // group
        RequestLifeCycle.begin(PortalContainer.getInstance());
        Group group = organizationService.getGroupHandler().findGroupById(groupId);
        if (group != null) {
          organizationService.getGroupHandler().removeGroup(group, true);
        }
        RequestLifeCycle.end();

        // FIXME Answer Bug: deleting a space don't delete answers category, but
        // it will be deleted if answer data is imported

      } else {
        log.info("Space '" + space.getDisplayName() + "' was found but replaceExisting=false. Ignore space import.");
        return true;
      }
    }

    if (createAbsentUsers) {
      RequestLifeCycle.begin(PortalContainer.getInstance());
      log.info("Create not found users of space: '" + spaceMetaData.getPrettyName() + "'.");
      String[] members = spaceMetaData.getMembers();
      for (String member : members) {
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
        }
      }
      RequestLifeCycle.end();
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
      log.warn("Editor of space '" + spaceMetaData.getDisplayName() + "' is empty, the manager '" + editor[0] + "' will be used instead.");
    }
    space.setEditor(editor[0]);

    space.setInvitedUsers(getExistingUsers(spaceMetaData.getInvitedUsers()));

    space.setRegistration(spaceMetaData.getRegistration());
    space.setDescription(spaceMetaData.getDescription());
    space.setType(spaceMetaData.getType());
    space.setVisibility(spaceMetaData.getVisibility());
    space.setPriority(spaceMetaData.getPriority());
    space.setUrl(spaceMetaData.getUrl());
    space = spaceService.createSpace(space, space.getEditor());
    if (isRenamed) {
      log.info("Rename space from '" + oldSpacePrettyName + "' to '" + newSpacePrettyName + "'.");
      spaceService.renameSpace(space, spaceMetaData.getDisplayName().trim());
      space = spaceService.getSpaceByDisplayName(spaceMetaData.getDisplayName());
    }

    RequestLifeCycle.begin(PortalContainer.getInstance());
    log.info("Add members to space: '" + spaceMetaData.getPrettyName() + "'.");

    space.setEditor(managers[0]);
    space.setMembers(members);
    space = spaceService.updateSpace(space);

    for (String member : members) {
      try {
        SpaceUtils.addUserToGroupWithMemberMembership(member, space.getGroupId());
        log.info("User '" + member + "' added to space as member: '" + spaceMetaData.getPrettyName() + "'.");
      } catch (Exception e) {
        log.warn("Cannot add member '" + member + "' to space: " + space.getPrettyName(), e);
      }
    }

    log.info("Set manager(s) of space: '" + spaceMetaData.getPrettyName() + "'.");

    space.setEditor(managers[0]);
    space.setManagers(managers);
    space = spaceService.updateSpace(space);

    for (String manager : managers) {
      try {
        log.info("User '" + manager + "' promoted to manager of space: '" + spaceMetaData.getPrettyName() + "'.");
        SpaceUtils.addUserToGroupWithManagerMembership(manager, space.getGroupId());

      } catch (Exception e) {
        log.warn("Cannot add manager '" + manager + "' to space: " + space.getPrettyName(), e);
      }
    }
    RequestLifeCycle.end();

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

  private void copyAttachementToLocalFolder(InputStream attachmentInputStream, File tmpZipFile) throws IOException, FileNotFoundException {
    NonCloseableZipInputStream zis = null;
    try {
      FileOutputStream tmpFileOutputStream = new FileOutputStream(tmpZipFile);
      IOUtils.copy(attachmentInputStream, tmpFileOutputStream);
      tmpFileOutputStream.close();
      attachmentInputStream.close();
    } finally {
      if (zis != null) {
        try {
          zis.reallyClose();
        } catch (IOException e) {
          log.warn("Can't close inputStream of attachement.");
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
      String managedEntryPathPrefix = "social/space/";
      ZipEntry entry;
      while ((entry = zis.getNextEntry()) != null) {
        String zipEntryPath = entry.getName();
        // Skip entries not managed by this extension
        if (!zipEntryPath.startsWith(managedEntryPathPrefix)) {
          continue;
        }

        // Skip directories
        if (entry.isDirectory()) {
          // Create directory in unzipped folder location
          createFile(new File(targetFolderPath + replaceSpecialChars(zipEntryPath)), true);
          continue;
        }
        int idBeginIndex = ("social/space/").length();
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
      String spacePrettyName, Map<String, File> ownerFiles, String subResourcePath) throws IOException, FileNotFoundException {
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

  private static void copyToDisk(InputStream input, String output) throws Exception {
    byte data[] = new byte[BUFFER];
    BufferedOutputStream dest = null;
    try {
      FileOutputStream fileOuput = new FileOutputStream(createFile(new File(output), false));
      dest = new BufferedOutputStream(fileOuput, BUFFER);
      int count = 0;
      while ((count = input.read(data, 0, BUFFER)) != -1)
        dest.write(data, 0, count);
    } finally {
      if (dest != null) {
        dest.close();
      }
    }
  }

  private static String replaceSpecialChars(String name) {
    name = name.replaceAll(":", "_");
    return name.replaceAll("\\?", "_");
  }

  private static File createFile(File file, boolean folder) throws IOException {
    if (file.getParentFile() != null)
      createFile(file.getParentFile(), true);
    if (file.exists())
      return file;
    if (file.isDirectory() || folder)
      file.mkdir();
    else
      file.createNewFile();
    return file;
  }

  // Bug in SUN's JDK XMLStreamReader implementation closes the underlying
  // stream when
  // it finishes reading an XML document. This is no good when we are using
  // a
  // ZipInputStream.
  // See http://bugs.sun.com/view_bug.do?bug_id=6539065 for more
  // information.
  public static class NonCloseableZipInputStream extends ZipInputStream {
    public NonCloseableZipInputStream(InputStream inputStream) {
      super(inputStream);
    }

    @Override
    public void close() throws IOException {}

    private void reallyClose() throws IOException {
      super.close();
    }
  }

}
