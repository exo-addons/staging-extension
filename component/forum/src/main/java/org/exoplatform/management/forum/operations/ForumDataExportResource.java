/*
 * Copyright (C) 2003-2014 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.management.forum.operations;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Session;

import org.apache.commons.lang.ArrayUtils;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.forum.common.jcr.KSDataLocation;
import org.exoplatform.forum.service.Category;
import org.exoplatform.forum.service.Forum;
import org.exoplatform.forum.service.ForumService;
import org.exoplatform.forum.service.Topic;
import org.exoplatform.forum.service.Utils;
import org.exoplatform.forum.service.impl.model.TopicFilter;
import org.exoplatform.management.common.AbstractJCROperationHandler;
import org.exoplatform.management.common.ActivitiesExportTask;
import org.exoplatform.management.common.JCRNodeExportTask;
import org.exoplatform.management.common.SpaceMetadataExportTask;
import org.exoplatform.management.forum.ForumExtension;
import org.exoplatform.poll.service.Poll;
import org.exoplatform.poll.service.PollService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.manager.ActivityManager;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.space.SpaceUtils;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.social.core.storage.api.ActivityStorage;
import org.gatein.common.logging.Logger;
import org.gatein.common.logging.LoggerFactory;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.exceptions.ResourceNotFoundException;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.ExportResourceModel;
import org.gatein.management.api.operation.model.ExportTask;

/**
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class ForumDataExportResource extends AbstractJCROperationHandler {

  final private static Logger log = LoggerFactory.getLogger(ForumDataExportResource.class);

  private ForumService forumService;
  private PollService pollService;
  private KSDataLocation dataLocation;
  private IdentityManager identityManager;

  private boolean isSpaceForumType;
  private String type;

  public ForumDataExportResource(boolean isSpaceForumType) {
    this.isSpaceForumType = isSpaceForumType;
    this.type = isSpaceForumType ? ForumExtension.SPACE_FORUM_TYPE : ForumExtension.PUBLIC_FORUM_TYPE;
  }

  @Override
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws ResourceNotFoundException, OperationException {
    spaceService = operationContext.getRuntimeContext().getRuntimeComponent(SpaceService.class);
    forumService = operationContext.getRuntimeContext().getRuntimeComponent(ForumService.class);
    pollService = operationContext.getRuntimeContext().getRuntimeComponent(PollService.class);
    repositoryService = operationContext.getRuntimeContext().getRuntimeComponent(RepositoryService.class);
    dataLocation = operationContext.getRuntimeContext().getRuntimeComponent(KSDataLocation.class);
    activityManager = operationContext.getRuntimeContext().getRuntimeComponent(ActivityManager.class);
    activityStorage = operationContext.getRuntimeContext().getRuntimeComponent(ActivityStorage.class);
    identityManager = operationContext.getRuntimeContext().getRuntimeComponent(IdentityManager.class);

    increaseCurrentTransactionTimeOut(operationContext);

    String name = operationContext.getAttributes().getValue("filter");

    String excludeSpaceMetadataString = operationContext.getAttributes().getValue("exclude-space-metadata");
    boolean exportSpaceMetadata = excludeSpaceMetadataString == null || excludeSpaceMetadataString.trim().equalsIgnoreCase("false");

    List<ExportTask> exportTasks = new ArrayList<ExportTask>();

    Category spaceCategory = forumService.getCategoryIncludedSpace();
    String workspace = dataLocation.getWorkspace();
    String categoryHomePath = dataLocation.getForumCategoriesLocation();

    if (name == null || name.isEmpty()) {
      log.info("Exporting all Forums of type: " + (isSpaceForumType ? "Spaces" : "Non Spaces"));
      List<Category> categories = forumService.getCategories();
      for (Category category : categories) {
        if (spaceCategory != null && category.getId().equals(spaceCategory.getId())) {
          continue;
        }
        exportForum(exportTasks, workspace, categoryHomePath, category.getId(), null, exportSpaceMetadata);
      }
    } else {
      if (isSpaceForumType) {
        if (spaceCategory != null) {
          Space space = spaceService.getSpaceByDisplayName(name);
          exportForum(exportTasks, workspace, categoryHomePath, spaceCategory.getId(), space.getPrettyName(), exportSpaceMetadata);
        }
      } else {
        List<Category> categories = forumService.getCategories();
        for (Category category : categories) {
          if (!category.getCategoryName().equals(name)) {
            continue;
          }
          exportForum(exportTasks, workspace, categoryHomePath, category.getId(), null, exportSpaceMetadata);
        }
      }
    }
    resultHandler.completed(new ExportResourceModel(exportTasks));
  }

  @Override
  protected void addJCRNodeExportTask(Node childNode, List<ExportTask> subNodesExportTask, boolean recursive, String... params) {
    if (params.length != 4) {
      log.warn("Cannot add Forum Export Task, 4 parameters was expected, got: " + ArrayUtils.toString(params));
      return;
    }
    String entryPath = "forum/" + type + "/" + (params[2] == null || params[2].isEmpty() ? params[1] : params[2]);
    JCRNodeExportTask exportTask = new JCRNodeExportTask(repositoryService, params[0], params[3], entryPath, recursive, true);
    subNodesExportTask.add(exportTask);
  }

  private void exportForum(List<ExportTask> exportTasks, String workspace, String categoryHomePath, String categoryId, String spacePrettyName, boolean exportSpaceMetadata) {
    try {
      String forumId = (spacePrettyName == null ? "" : Utils.FORUM_SPACE_ID_PREFIX + spacePrettyName);
      if (isSpaceForumType) {
        Space space = spaceService.getSpaceByPrettyName(spacePrettyName);
        spacePrettyName = space.getGroupId().replace(SpaceUtils.SPACE_GROUP + "/", "");

        forumId = Utils.FORUM_SPACE_ID_PREFIX + spacePrettyName;
        if (exportSpaceMetadata) {
          String prefix = "forum/space/" + forumId + "/";
          exportTasks.add(new SpaceMetadataExportTask(space, prefix));
        }
      }

      String parentNodePath = "/" + categoryHomePath + "/" + categoryId + (forumId.isEmpty() ? "" : "/" + forumId);
      Session session = getSession(workspace);
      Node parentNode = (Node) session.getItem(parentNodePath);
      exportNode(parentNode, exportTasks, workspace, categoryId, forumId);

      // export Activities
      exportActivities(exportTasks, categoryId, forumId);
    } catch (Exception exception) {
      throw new OperationException(OperationNames.EXPORT_RESOURCE, "Error while exporting forum", exception);
    }
  }

  private void exportActivities(List<ExportTask> exportTasks, String categoryId, String forumId) throws Exception {
    log.info("export forum activities");
    List<String> forumIds = new ArrayList<String>();
    if (isSpaceForumType) {
      categoryId = forumService.getCategoryIncludedSpace().getId();
      forumIds.add(forumId);
    } else {
      @SuppressWarnings("deprecation")
      List<Forum> forums = forumService.getForums(categoryId, null);
      for (Forum forum : forums) {
        forumIds.add(forum.getId());
      }
    }
    for (String tmpForumId : forumIds) {
      TopicFilter topicFilter = new TopicFilter(categoryId, tmpForumId);
      ListAccess<Topic> topicsListAccess = forumService.getTopics(topicFilter);
      if (topicsListAccess.getSize() == 0) {
        continue;
      }
      Topic[] topics = topicsListAccess.load(0, topicsListAccess.getSize());
      List<ExoSocialActivity> activitiesList = new ArrayList<ExoSocialActivity>();
      for (Topic topic : topics) {
        String activityId = forumService.getActivityIdForOwnerId(topic.getId());
        addActivityWithComments(activitiesList, activityId);

        if (topic.getIsPoll()) {
          String pollId = topic.getId().replace(Utils.TOPIC, Utils.POLL);
          Poll poll = pollService.getPoll(pollId);
          if (poll != null) {
            String pollPath = poll.getParentPath() + "/" + poll.getId();
            String pollActivityId = pollService.getActivityIdForOwner(pollPath);
            addActivityWithComments(activitiesList, pollActivityId);
          }
        }
      }
      if (!activitiesList.isEmpty()) {
        String prefix = "forum/" + type + "/" + ((forumId == null || forumId.isEmpty()) ? categoryId : forumId);
        exportTasks.add(new ActivitiesExportTask(identityManager, activitiesList, prefix));
      }
    }
  }
}