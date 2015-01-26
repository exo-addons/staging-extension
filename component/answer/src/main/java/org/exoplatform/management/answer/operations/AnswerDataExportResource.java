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
package org.exoplatform.management.answer.operations;

import java.util.ArrayList;
import java.util.List;

import org.apache.poi.util.IOUtils;
import org.exoplatform.faq.service.Category;
import org.exoplatform.faq.service.Comment;
import org.exoplatform.faq.service.FAQService;
import org.exoplatform.faq.service.FileAttachment;
import org.exoplatform.faq.service.Question;
import org.exoplatform.faq.service.QuestionPageList;
import org.exoplatform.faq.service.Utils;
import org.exoplatform.management.answer.AnswerExtension;
import org.exoplatform.management.common.AbstractOperationHandler;
import org.exoplatform.management.common.ActivitiesExportTask;
import org.exoplatform.management.common.SpaceMetadataExportTask;
import org.exoplatform.social.common.RealtimeListAccess;
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
public class AnswerDataExportResource extends AbstractOperationHandler {

  final private static Logger log = LoggerFactory.getLogger(AnswerDataExportResource.class);

  private FAQService faqService;

  private IdentityManager identityManager;

  private boolean isSpaceType;
  private String type;

  public AnswerDataExportResource(boolean isSpaceType) {
    this.isSpaceType = isSpaceType;
    this.type = isSpaceType ? AnswerExtension.SPACE_FAQ_TYPE : AnswerExtension.PUBLIC_FAQ_TYPE;
  }

  @Override
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws ResourceNotFoundException, OperationException {
    spaceService = operationContext.getRuntimeContext().getRuntimeComponent(SpaceService.class);
    faqService = operationContext.getRuntimeContext().getRuntimeComponent(FAQService.class);
    activityManager = operationContext.getRuntimeContext().getRuntimeComponent(ActivityManager.class);
    activityStorage = operationContext.getRuntimeContext().getRuntimeComponent(ActivityStorage.class);
    identityManager = operationContext.getRuntimeContext().getRuntimeComponent(IdentityManager.class);

    String name = operationContext.getAttributes().getValue("filter");

    String excludeSpaceMetadataString = operationContext.getAttributes().getValue("exclude-space-metadata");
    boolean exportSpaceMetadata = excludeSpaceMetadataString == null || excludeSpaceMetadataString.trim().equalsIgnoreCase("false");

    List<ExportTask> exportTasks = new ArrayList<ExportTask>();

    try {
      if (name == null || name.isEmpty()) {
        log.info("Exporting all FAQ of type: " + (isSpaceType ? "Spaces" : "Public"));
        List<Category> categories = faqService.getAllCategories();
        for (Category category : categories) {
          if ((isSpaceType && !category.getId().startsWith(Utils.CATE_SPACE_ID_PREFIX)) || (!isSpaceType && category.getId().startsWith(Utils.CATE_SPACE_ID_PREFIX))) {
            continue;
          }
          Space space = null;
          if (isSpaceType) {
            space = spaceService.getSpaceByGroupId(SpaceUtils.SPACE_GROUP + "/" + category.getId().replace(Utils.CATE_SPACE_ID_PREFIX, ""));
          }
          exportAnswer(exportTasks, category, space, exportSpaceMetadata);
        }
      } else {
        if (isSpaceType) {
          Space space = spaceService.getSpaceByDisplayName(name);
          String groupName = space.getGroupId().replace(SpaceUtils.SPACE_GROUP + "/", "");

          Category category = faqService.getCategoryById(Utils.CATE_SPACE_ID_PREFIX + groupName);
          if (category != null) {
            exportAnswer(exportTasks, category, space, exportSpaceMetadata);
          } else {
            log.info("Cannot find Answer Category of Space: " + space.getDisplayName());
          }
        } else {
          if (name.equals(AnswerExtension.ROOT_CATEGORY)) {
            Category defaultCategory = faqService.getCategoryById(Utils.CATEGORY_HOME);
            // Export questions from root category
            exportAnswer(exportTasks, defaultCategory, null, exportSpaceMetadata);
          } else {
            List<Category> categories = faqService.getAllCategories();
            for (Category category : categories) {
              if (!category.getName().equals(name)) {
                continue;
              }
              exportAnswer(exportTasks, category, null, exportSpaceMetadata);
            }
          }
        }
      }
    } catch (Exception e) {
      throw new OperationException(OperationNames.EXPORT_RESOURCE, "Error while exporting FAQ categories.", e);
    }
    resultHandler.completed(new ExportResourceModel(exportTasks));
  }

  private void exportAnswer(List<ExportTask> exportTasks, Category category, Space space, boolean exportSpaceMetadata) throws Exception {
    QuestionPageList questionsPageList = faqService.getAllQuestionsByCatetory(category.getId(), AnswerExtension.EMPTY_FAQ_SETTIGNS);
    List<Question> questions = questionsPageList.getAll();
    for (Question question : questions) {
      if (question.getAttachMent() != null && !question.getAttachMent().isEmpty()) {
        List<FileAttachment> attachments = question.getAttachMent();
        for (FileAttachment fileAttachment : attachments) {
          InputStreamWrapper inputStream = new InputStreamWrapper(IOUtils.toByteArray(fileAttachment.getInputStream()));
          fileAttachment.setInputStream(inputStream);
        }
      }
    }
    exportTasks.add(new AnswerExportTask(type, category, questions));
    exportActivities(exportTasks, category, questions);

    if (exportSpaceMetadata && isSpaceType) {
      if (space == null) {
        log.warn("Should export space DATA but it is null");
      } else {
        String prefix = "answer/space/" + category.getId() + "/";
        exportTasks.add(new SpaceMetadataExportTask(space, prefix));
      }
    }
  }

  private void exportActivities(List<ExportTask> exportTasks, Category category, List<Question> questions) throws Exception {
    log.info("export answer activities");
    List<ExoSocialActivity> activitiesList = new ArrayList<ExoSocialActivity>();
    for (Question question : questions) {
      String activityId = faqService.getActivityIdForQuestion(question.getPath());
      if (activityId == null) {
        continue;
      }
      ExoSocialActivity questionActivity = activityManager.getActivity(activityId);
      activitiesList.add(questionActivity);

      RealtimeListAccess<ExoSocialActivity> commentsListAccess = activityManager.getCommentsWithListAccess(questionActivity);
      if (commentsListAccess.getSize() > 0) {
        List<ExoSocialActivity> comments = commentsListAccess.loadAsList(0, commentsListAccess.getSize());
        // set CommentId in Link where QuestionLink is added instead
        for (ExoSocialActivity exoSocialActivity : comments) {
          if (exoSocialActivity.getTemplateParams().containsKey("Link") && exoSocialActivity.getTemplateParams().get("Link").contains("questionId=")) {
            Comment[] questionComments = question.getComments();
            for (Comment comment : questionComments) {
              String commentActivityId = faqService.getActivityIdForComment(question.getPath(), comment.getId(), question.getLanguage());
              if (commentActivityId != null && commentActivityId.equals(exoSocialActivity.getId())) {
                exoSocialActivity.getTemplateParams().put("Link", comment.getId());
              }
            }
          }
          exoSocialActivity.isComment(true);
          exoSocialActivity.setParentId(activityId);
        }
        activitiesList.addAll(comments);
      }
    }
    if (!activitiesList.isEmpty()) {
      String prefix = "answer/" + type + "/" + category.getId();
      exportTasks.add(new ActivitiesExportTask(identityManager, activitiesList, prefix));
    }
  }

}
