package org.exoplatform.management.forum.operations;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.forum.common.jcr.KSDataLocation;
import org.exoplatform.forum.service.Category;
import org.exoplatform.forum.service.Forum;
import org.exoplatform.forum.service.ForumService;
import org.exoplatform.forum.service.Post;
import org.exoplatform.forum.service.Topic;
import org.exoplatform.forum.service.Utils;
import org.exoplatform.forum.service.impl.model.TopicFilter;
import org.exoplatform.management.common.FileEntry;
import org.exoplatform.management.common.exportop.ActivitiesExportTask;
import org.exoplatform.management.common.exportop.SpaceMetadataExportTask;
import org.exoplatform.management.common.importop.AbstractJCRImportOperationHandler;
import org.exoplatform.management.common.importop.ActivityImportOperationInterface;
import org.exoplatform.management.common.importop.FileImportOperationInterface;
import org.exoplatform.management.forum.ForumExtension;
import org.exoplatform.poll.service.Poll;
import org.exoplatform.poll.service.PollService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.manager.ActivityManager;
import org.exoplatform.social.core.space.SpaceUtils;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.social.core.storage.api.ActivityStorage;
import org.exoplatform.social.core.storage.api.IdentityStorage;
import org.gatein.common.logging.Logger;
import org.gatein.common.logging.LoggerFactory;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.operation.OperationAttributes;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.NoResultModel;

/**
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class ForumDataImportResource extends AbstractJCRImportOperationHandler implements ActivityImportOperationInterface, FileImportOperationInterface {

  final private static Logger log = LoggerFactory.getLogger(ForumDataImportResource.class);

  private ForumService forumService;
  private PollService pollService;
  private KSDataLocation dataLocation;

  private String type;

  public ForumDataImportResource(boolean isSpaceForumType) {
    type = isSpaceForumType ? ForumExtension.SPACE_FORUM_TYPE : ForumExtension.PUBLIC_FORUM_TYPE;
  }

  @Override
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws OperationException {
    spaceService = operationContext.getRuntimeContext().getRuntimeComponent(SpaceService.class);
    forumService = operationContext.getRuntimeContext().getRuntimeComponent(ForumService.class);
    repositoryService = operationContext.getRuntimeContext().getRuntimeComponent(RepositoryService.class);
    dataLocation = operationContext.getRuntimeContext().getRuntimeComponent(KSDataLocation.class);
    activityManager = operationContext.getRuntimeContext().getRuntimeComponent(ActivityManager.class);
    activityStorage = operationContext.getRuntimeContext().getRuntimeComponent(ActivityStorage.class);
    identityStorage = operationContext.getRuntimeContext().getRuntimeComponent(IdentityStorage.class);
    pollService = operationContext.getRuntimeContext().getRuntimeComponent(PollService.class);

    increaseCurrentTransactionTimeOut(operationContext);
    try {
      OperationAttributes attributes = operationContext.getAttributes();
      List<String> filters = attributes.getValues("filter");

      // "replace-existing" attribute. Defaults to false.
      boolean replaceExisting = filters.contains("replace-existing:true");

      // "create-space" attribute. Defaults to false.
      boolean createSpace = filters.contains("create-space:true");

      log.info("Importing Forums Data");
      InputStream attachmentInputStream = getAttachementInputStream(operationContext);
      try {
        // extract data from zip
        Map<String, List<FileEntry>> contentsByOwner = extractDataFromZip(attachmentInputStream);

        String workspace = dataLocation.getWorkspace();

        for (String categoryId : contentsByOwner.keySet()) {
          List<FileEntry> fileEntries = contentsByOwner.get(categoryId);

          boolean isSpaceForum = categoryId.contains(Utils.FORUM_SPACE_ID_PREFIX);
          if (isSpaceForum) {
            String forumId = categoryId;
            FileEntry spaceMetadataFile = getAndRemoveFileByPath(fileEntries, SpaceMetadataExportTask.FILENAME);
            if (spaceMetadataFile != null && spaceMetadataFile.getFile().exists()) {
              boolean spaceCreatedOrAlreadyExists = createSpaceIfNotExists(spaceMetadataFile.getFile(), createSpace);
              if (!spaceCreatedOrAlreadyExists) {
                log.warn("Import of forum category '" + categoryId + "' is ignored because space doesn't exist. Turn on 'create-space:true' option if you want to automatically create the space.");
                continue;
              }
            }

            Category spaceCategory = forumService.getCategoryIncludedSpace();
            Forum forum = forumService.getForum(spaceCategory.getId(), forumId);

            if (forum != null) {
              if (replaceExisting) {
                log.info("Overwrite existing Space Forum: '" + forum.getForumName() + "'  (replace-existing=true)");
                deleteActivities(spaceCategory.getId(), forumId);
              } else {
                log.info("Ignore existing Space Forum: '" + forum.getForumName() + "'  (replace-existing=false)");
                continue;
              }
            }

            FileEntry activitiesFile = getAndRemoveFileByPath(fileEntries, ActivitiesExportTask.FILENAME);

            Collections.sort(fileEntries);
            for (FileEntry fileEntry : fileEntries) {
              importNode(fileEntry, workspace, false);
            }

            // Refresh caches
            forumService.calculateDeletedUser("fakeUser" + Utils.DELETED);

            // Import activities
            if (activitiesFile != null && activitiesFile.getFile().exists()) {
              String spaceGroupName = forumId.replace(Utils.FORUM_SPACE_ID_PREFIX, "");
              String spaceGroupId = SpaceUtils.SPACE_GROUP + "/" + spaceGroupName;
              Space space = spaceService.getSpaceByGroupId(spaceGroupId);

              log.info("Importing Forum activities");
              importActivities(activitiesFile.getFile(), space.getPrettyName(), true);
            }
          } else {
            Category category = forumService.getCategory(categoryId);

            if (category != null) {
              if (replaceExisting) {
                log.info("Overwrite existing Forum Category: '" + category.getCategoryName() + "'  (replace-existing=true)");
                @SuppressWarnings("deprecation")
                List<Forum> forums = forumService.getForums(categoryId, null);
                for (Forum forum : forums) {
                  deleteActivities(categoryId, forum.getId());
                }
              } else {
                log.info("Ignore existing Forum Category: '" + category.getCategoryName() + "'  (replace-existing=false)");
                continue;
              }
            }

            FileEntry activitiesFile = getAndRemoveFileByPath(fileEntries, ActivitiesExportTask.FILENAME);

            Collections.sort(fileEntries);
            for (FileEntry fileEntry : fileEntries) {
              importNode(fileEntry, workspace, false);
            }

            // Refresh caches
            forumService.calculateDeletedUser("fakeUser" + Utils.DELETED);
            // Import activities
            if (activitiesFile != null && activitiesFile.getFile().exists()) {
              log.info("Importing Forum activities");
              importActivities(activitiesFile.getFile(), null, true);
            }
          }
        }
        forumService.calculateDeletedUser("fakeUser" + Utils.DELETED);
      } catch (Exception e) {
        throw new OperationException(OperationNames.IMPORT_RESOURCE, "Unable to import forum contents", e);
      } finally {
        if (attachmentInputStream != null) {
          try {
            attachmentInputStream.close();
          } catch (IOException e) {
            // Nothing to do
          }
        }
      }
      resultHandler.completed(NoResultModel.INSTANCE);
    } finally {
      restoreDefaultTransactionTimeOut(operationContext);
    }
  }

  @Override
  public String getNodePath(String filePath) {
    return super.getNodePath(filePath);
  }

  public String getManagedFilesPrefix() {
    return "forum/" + type + "/";
  }

  public boolean isUnKnownFileFormat(String filePath) {
    return !filePath.endsWith(".xml") && !filePath.endsWith(SpaceMetadataExportTask.FILENAME) && !filePath.contains(ActivitiesExportTask.FILENAME);
  }

  public boolean addSpecialFile(List<FileEntry> fileEntries, String filePath, File file) {
    if (filePath.endsWith(SpaceMetadataExportTask.FILENAME)) {
      fileEntries.add(new FileEntry(SpaceMetadataExportTask.FILENAME, file));
      return true;
    } else if (filePath.endsWith(ActivitiesExportTask.FILENAME)) {
      fileEntries.add(new FileEntry(ActivitiesExportTask.FILENAME, file));
      return true;
    }
    return false;
  }

  public void attachActivityToEntity(ExoSocialActivity activity, ExoSocialActivity comment) throws Exception {
    if (activity.getTemplateParams().containsKey("PollLink")) {
      if (comment == null) {
        String topicId = activity.getTemplateParams().get("Id");
        String pollId = topicId.replace(Utils.TOPIC, Utils.POLL);
        Poll poll = pollService.getPoll(pollId);
        String pollPath = poll.getParentPath() + "/" + pollId;
        pollService.saveActivityIdForOwner(pollPath, activity.getId());
      } else {
        // Don't attach poll comment to anything
      }
      return;
    }
    String catId = activity.getTemplateParams().get("CateId");
    if (catId == null) {
      return;
    } else {
      String forumId = activity.getTemplateParams().get("ForumId");
      String topicId = activity.getTemplateParams().get("TopicId");
      Topic topic = forumService.getTopic(catId, forumId, topicId, null);
      if (comment == null) {
        forumService.saveActivityIdForOwnerPath(topic.getPath(), activity.getId());
      } else {
        String postId = comment.getTemplateParams().get("PostId");
        if (postId != null) {
          Post post = forumService.getPost(catId, forumId, topicId, postId);
          forumService.saveActivityIdForOwnerPath(post.getPath(), comment.getId());
        }
      }
    }
  }

  public boolean isActivityNotValid(ExoSocialActivity activity, ExoSocialActivity comment) throws Exception {
    if (activity.getTemplateParams().containsKey("PollLink")) {
      String topicId = activity.getTemplateParams().get("Id");
      if (topicId == null) {
        log.warn("Poll with null id of topic. Cannot import activity '" + activity.getTitle() + "'.");
        return true;
      }
      String pollId = topicId.replace(Utils.TOPIC, Utils.POLL);
      Poll poll = pollService.getPoll(pollId);
      if (poll == null) {
        log.warn("Poll not found. Cannot import activity '" + activity.getTitle() + "'.");
        return true;
      }
      return false;
    }

    String catId = activity.getTemplateParams().get("CateId");
    if (catId == null) {
      return false;
    } else {
      String forumId = activity.getTemplateParams().get("ForumId");
      String topicId = activity.getTemplateParams().get("TopicId");
      if (forumId == null || topicId == null) {
        log.warn("Activity template params are inconsistent: '" + activity.getTitle() + "'.");
        return true;
      }
      Topic topic = forumService.getTopic(catId, forumId, topicId, null);
      if (topic == null) {
        log.warn("Forum Topic not found. Cannot import activity '" + activity.getTitle() + "'.");
        return true;
      }
      if (comment != null) {
        String postId = comment.getTemplateParams().get("PostId");
        if (postId != null) {
          Post post = forumService.getPost(catId, forumId, topicId, postId);
          if (post == null) {
            log.warn("Forum Post not found. Cannot import activity '" + activity.getTitle() + "'.");
            return true;
          }
        }
      }
    }
    return false;
  }

  public String extractIdFromPath(String path) {
    int beginIndex = ("forum/" + type + "/").length();
    int endIndex = path.indexOf("/", beginIndex + 1);
    return path.substring(beginIndex, endIndex);
  }

  private void deleteActivities(String categoryId, String forumId) throws Exception {
    // Delete Forum activity stream
    TopicFilter topicFilter = new TopicFilter(categoryId, forumId);
    ListAccess<Topic> topicsListAccess = forumService.getTopics(topicFilter);
    if (topicsListAccess.getSize() > 0) {
      Topic[] topics = topicsListAccess.load(0, topicsListAccess.getSize());
      for (Topic topic : topics) {
        ExoSocialActivity activity = activityManager.getActivity(forumService.getActivityIdForOwnerId(topic.getId()));
        if (activity != null) {
          deleteActivity(activity);
        }
      }
    }
  }
}
