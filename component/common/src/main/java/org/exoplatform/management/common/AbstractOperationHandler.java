package org.exoplatform.management.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipInputStream;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.common.RealtimeListAccess;
import org.exoplatform.social.core.activity.model.ActivityStream.Type;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.identity.provider.SpaceIdentityProvider;
import org.exoplatform.social.core.manager.ActivityManager;
import org.exoplatform.social.core.space.SpaceUtils;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.social.core.storage.ActivityStorageException;
import org.exoplatform.social.core.storage.api.ActivityStorage;
import org.exoplatform.social.core.storage.api.IdentityStorage;
import org.gatein.management.api.operation.OperationHandler;

import com.thoughtworks.xstream.XStream;

public abstract class AbstractOperationHandler implements OperationHandler {

  protected static final Log log = ExoLogger.getLogger(AbstractOperationHandler.class);

  protected static final String[] EMPTY_STRING_ARRAY = new String[0];

  protected UserACL userACL;
  protected SpaceService spaceService;
  protected ActivityManager activityManager;
  protected ActivityStorage activityStorage;
  protected IdentityStorage identityStorage;

  // This is used to test on duplicated activities
  protected Set<Long> activitiesByPostTime = new HashSet<Long>();

  protected final void addActivityWithComments(List<ExoSocialActivity> activitiesList, String activityId) {
    if (activityId == null || activityId.isEmpty()) {
      return;
    }
    ExoSocialActivity parentActivity = activityManager.getActivity(activityId);
    addActivityWithComments(activitiesList, parentActivity);
  }

  protected void addActivityWithComments(List<ExoSocialActivity> activitiesList, ExoSocialActivity parentActivity) {
    if (parentActivity != null && parentActivity.getParentId() == null && !parentActivity.isComment()) {
      activitiesList.add(parentActivity);
      RealtimeListAccess<ExoSocialActivity> commentsListAccess = activityManager.getCommentsWithListAccess(parentActivity);
      if (commentsListAccess.getSize() > 0) {
        List<ExoSocialActivity> comments = commentsListAccess.loadAsList(0, commentsListAccess.getSize());
        for (ExoSocialActivity exoSocialActivityComment : comments) {
          exoSocialActivityComment.isComment(true);
          exoSocialActivityComment.setParentId(parentActivity.getId());
        }
        activitiesList.addAll(comments);
      }
    }
  }

  protected final void deleteActivity(String activityId) {
    if (activityId == null || activityId.isEmpty()) {
      return;
    }
    ExoSocialActivity activity = activityManager.getActivity(activityId);
    deleteActivity(activity);
  }

  protected final void deleteActivity(ExoSocialActivity activity) {
    if (activity == null) {
      return;
    }
    RealtimeListAccess<ExoSocialActivity> commentsListAccess = activityManager.getCommentsWithListAccess(activity);
    if (commentsListAccess.getSize() > 0) {
      List<ExoSocialActivity> comments = commentsListAccess.loadAsList(0, commentsListAccess.getSize());
      for (ExoSocialActivity commentActivity : comments) {
        activityManager.deleteActivity(commentActivity);
      }
    }
    log.info("   Delete activity : " + activity.getTitle());
    activityManager.deleteActivity(activity);
  }

  protected boolean createSpaceIfNotExists(File spaceMetadataFile, String groupId, boolean createSpace) throws IOException {
    Space space = spaceService.getSpaceByGroupId(groupId);
    if (space == null && createSpace) {
      FileInputStream spaceMetadataIS = new FileInputStream(spaceMetadataFile);
      try {
        // Unmarshall metadata xml file
        XStream xstream = new XStream();
        xstream.alias("metadata", SpaceMetaData.class);
        SpaceMetaData spaceMetaData = (SpaceMetaData) xstream.fromXML(spaceMetadataIS);

        log.info("Automatically create new space: '" + spaceMetaData.getPrettyName() + "'.");
        space = new Space();
        space.setPrettyName(spaceMetaData.getPrettyName());
        space.setDisplayName(spaceMetaData.getDisplayName());
        space.setGroupId(groupId);
        space.setTag(spaceMetaData.getTag());
        space.setApp(spaceMetaData.getApp());
        space.setEditor(spaceMetaData.getEditor() != null ? spaceMetaData.getEditor() : spaceMetaData.getManagers().length > 0 ? spaceMetaData.getManagers()[0] : userACL.getSuperUser());
        space.setManagers(spaceMetaData.getManagers());
        space.setInvitedUsers(spaceMetaData.getInvitedUsers());
        space.setRegistration(spaceMetaData.getRegistration());
        space.setDescription(spaceMetaData.getDescription());
        space.setType(spaceMetaData.getType());
        space.setVisibility(spaceMetaData.getVisibility());
        space.setPriority(spaceMetaData.getPriority());
        space.setUrl(spaceMetaData.getUrl());
        spaceService.createSpace(space, space.getEditor());
        return true;
      } finally {
        if (spaceMetadataIS != null) {
          try {
            spaceMetadataIS.close();
          } catch (Exception e) {
            log.warn(e);
          }
        }
        if (spaceMetadataFile != null && spaceMetadataFile.exists()) {
          spaceMetadataFile.delete();
        }
      }
    }
    return (space != null);
  }

  protected final void saveComment(ExoSocialActivity activity, ExoSocialActivity comment) {
    long updatedTime = activity.getUpdated().getTime();
    if (activity.getId() == null) {
      log.warn("Parent activity '" + activity.getTitle() + "' has a null ID, cannot import activity comment '" + comment.getTitle() + "'.");
      return;
    }
    activity = activityManager.getActivity(activity.getId());
    try {
      activityStorage.setInjectStreams(false);
      activityManager.saveComment(activity, comment);
    } catch (NullPointerException e) {
      log.warn("Error while importing comment: '" + comment.getTitle() + "'.");
      return;
    }
    if (comment.getId() == null) {
      log.warn("Error while importing comment, id is null: '" + comment.getTitle() + "'.");
      return;
    }
    activity = activityManager.getActivity(activity.getId());
    activity.setUpdated(updatedTime);
    activityManager.updateActivity(activity);
    log.info("Comment activity is imported: '" + comment.getTitle() + "'.");
  }

  protected final void saveActivity(ExoSocialActivity activity, String spacePrettyName) {
    activityStorage.setInjectStreams(false);
    long updatedTime = activity.getUpdated().getTime();
    if (spacePrettyName == null) {
      try {
        activityManager.saveActivityNoReturn(activity);
        activity.setUpdated(updatedTime);
        activityManager.updateActivity(activity);
        log.info("Activity  is imported: '" + activity.getTitle() + "'");
      } catch (ActivityStorageException e) {
        log.warn("Activity is not imported, it may already exist: '" + activity.getTitle() + "'.");
      }
    } else {
      Identity spaceIdentity = getIdentity(spacePrettyName);
      if (spaceIdentity == null) {
        log.warn("Activity is not imported: '" + activity.getTitle() + "'.");
        return;
      }
      try {
        activityManager.saveActivityNoReturn(spaceIdentity, activity);
        activity.setUpdated(updatedTime);
        activityManager.updateActivity(activity);
        log.info("Activity  is imported: '" + activity.getTitle() + "'");
      } catch (ActivityStorageException e) {
        log.warn("Activity is not imported, it may already exist: '" + activity.getTitle() + "'.");
      }
    }
  }

  protected final void saveActivity(ExoSocialActivity activity, Identity spaceIdentity) {
    activityStorage.setInjectStreams(false);
    long updatedTime = activity.getUpdated().getTime();
    try {
      activityManager.saveActivityNoReturn(spaceIdentity, activity);
      activity.setUpdated(updatedTime);
      activityManager.updateActivity(activity);
      log.info("Activity  is imported: '" + activity.getTitle() + "'.");
    } catch (ActivityStorageException e) {
      log.warn("Activity is not imported, it may already exist: '" + activity.getTitle() + "'.");
    }
  }

  protected final void saveActivity(ExoSocialActivity activity) {
    activityStorage.setInjectStreams(false);
    long updatedTime = activity.getUpdated().getTime();
    if (activity.getActivityStream().getType().equals(Type.SPACE)) {
      String spacePrettyName = activity.getActivityStream().getPrettyId();
      Identity spaceIdentity = getIdentity(spacePrettyName);
      if (spaceIdentity == null) {
        log.warn("Activity is not imported'" + activity.getTitle() + "'.");
        return;
      }
      try {
        activityManager.saveActivityNoReturn(spaceIdentity, activity);
        activity.setUpdated(updatedTime);
        activityManager.updateActivity(activity);
        log.info("Activity : '" + activity.getTitle() + " is imported.");
      } catch (ActivityStorageException e) {
        log.warn("Activity is not imported, it may already exist: '" + activity.getTitle() + "'.");
      }
    } else {
      try {
        activityManager.saveActivityNoReturn(activity);
        activity.setUpdated(updatedTime);
        activityManager.updateActivity(activity);
        log.info("Activity : '" + activity.getTitle() + " is imported.");
      } catch (ActivityStorageException e) {
        log.warn("Activity is not imported, it may already exist: '" + activity.getTitle() + "'.");
      }
    }
  }

  protected final Identity getIdentity(String id) {
    try {
      Identity userIdentity = identityStorage.findIdentity(OrganizationIdentityProvider.NAME, id);

      if (userIdentity != null) {
        return userIdentity;
      } else {
        Identity spaceIdentity = identityStorage.findIdentity(SpaceIdentityProvider.NAME, id);

        // Try to see if space was renamed
        if (spaceIdentity == null) {
          Space space = spaceService.getSpaceByGroupId(id);
          if (space == null) {
            space = spaceService.getSpaceByGroupId(SpaceUtils.SPACE_GROUP + "/" + id);
          }
          if (space != null) {
            spaceIdentity = getIdentity(space.getPrettyName());
          }
        }
        return spaceIdentity;
      }
    } catch (Exception e) {
      log.error("Error while retrieving identity: ", e);
    }
    return null;
  }

  protected final List<ExoSocialActivity> sanitizeContent(List<ExoSocialActivity> activities) {
    List<ExoSocialActivity> activitiesList = new ArrayList<ExoSocialActivity>();
    Identity identity = null;
    boolean ignoreNextComments = false;
    for (ExoSocialActivity activity : activities) {
      if (ignoreNextComments) {
        if (activity.isComment()) {
          continue;
        } else {
          ignoreNextComments = false;
        }
      }
      if (activity.getPostedTime() != null && activitiesByPostTime.contains(activity.getPostedTime())) {
        log.info("Ignore duplicated Activity '" + activity.getTitle() + "'.");
        continue;
      } else {
        activitiesByPostTime.add(activity.getPostedTime());
      }
      identity = getIdentity(activity.getUserId());
      if (identity != null) {
        activity.setUserId(identity.getId());
        identity = getIdentity(activity.getPosterId());
        if (identity != null) {
          activitiesList.add(activity);

          activity.setPosterId(identity.getId());

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
          activity.setReplyToId(null);

          String[] commentedIds = activity.getCommentedIds();
          commentedIds = changeUsernameIdToIdentity(commentedIds);
          activity.setCommentedIds(commentedIds);

          String[] mentionedIds = activity.getMentionedIds();
          mentionedIds = changeUsernameIdToIdentity(mentionedIds);
          activity.setMentionedIds(mentionedIds);

          String[] likeIdentityIds = activity.getLikeIdentityIds();
          likeIdentityIds = changeUsernameIdToIdentity(likeIdentityIds);
          activity.setLikeIdentityIds(likeIdentityIds);
        }
      }
      if (identity == null) {
        ignoreNextComments = true;
        log.info("ACTIVITY IS NOT IMPORTED because the associated user '" + activity.getUserId() + "' wasn't found:  '" + activity.getTitle() + "'");
      }
    }
    return activitiesList;
  }

  private String[] changeUsernameIdToIdentity(String[] ids) {
    List<String> resultIds = new ArrayList<String>();
    if (ids != null && ids.length > 0) {
      for (int i = 0; i < ids.length; i++) {
        String[] id = ids[i].split("@");
        Identity identity = getIdentity(id[0]);
        if (identity != null) {
          id[0] = (String) identity.getId();
          if (id.length == 2) {
            ids[i] = id[0] + "@" + id[1];
          } else {
            ids[i] = id[0];
          }
          resultIds.add(ids[i]);
        }
      }
      ids = resultIds.toArray(EMPTY_STRING_ARRAY);
    }
    return ids;
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

    public void reallyClose() throws IOException {
      super.close();
    }
  }

}
