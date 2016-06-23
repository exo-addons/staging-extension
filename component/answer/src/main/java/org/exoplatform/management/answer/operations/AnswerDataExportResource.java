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
import org.exoplatform.faq.service.FAQService;
import org.exoplatform.faq.service.FileAttachment;
import org.exoplatform.faq.service.Question;
import org.exoplatform.faq.service.QuestionPageList;
import org.exoplatform.faq.service.Utils;
import org.exoplatform.management.answer.AnswerExtension;
import org.exoplatform.management.common.InputStreamWrapper;
import org.exoplatform.management.common.exportop.AbstractExportOperationHandler;
import org.exoplatform.management.common.exportop.ActivityExportOperationInterface;
import org.exoplatform.management.common.exportop.SpaceMetadataExportTask;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.manager.ActivityManager;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.space.SpaceUtils;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.social.core.storage.api.IdentityStorage;
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
public class AnswerDataExportResource extends AbstractExportOperationHandler implements ActivityExportOperationInterface {

  final private static Logger log = LoggerFactory.getLogger(AnswerDataExportResource.class);

  private FAQService faqService;
  private UserACL userACL;

  private boolean isSpaceType;
  private String type;

  private ThreadLocal<Category> categoryThreadLocal = new ThreadLocal<Category>();

  public AnswerDataExportResource(boolean isSpaceType) {
    this.isSpaceType = isSpaceType;
    this.type = isSpaceType ? AnswerExtension.SPACE_FAQ_TYPE : AnswerExtension.PUBLIC_FAQ_TYPE;
  }

  @Override
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws ResourceNotFoundException, OperationException {
    spaceService = operationContext.getRuntimeContext().getRuntimeComponent(SpaceService.class);
    faqService = operationContext.getRuntimeContext().getRuntimeComponent(FAQService.class);
    userACL = operationContext.getRuntimeContext().getRuntimeComponent(UserACL.class);
    activityManager = operationContext.getRuntimeContext().getRuntimeComponent(ActivityManager.class);
    identityManager = operationContext.getRuntimeContext().getRuntimeComponent(IdentityManager.class);
    identityStorage = operationContext.getRuntimeContext().getRuntimeComponent(IdentityStorage.class);

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

    categoryThreadLocal.set(category);
    // In case of minimal profile
    if (activityManager != null) {
      String prefix = "answer/" + type + "/" + category.getId() + "/";
      exportActivities(exportTasks, space == null ? ((category.getModerators() == null || category.getModerators().length == 0) ? userACL.getSuperUser() : category.getModerators()[0])
              : space.getPrettyName(), prefix, ANSWER_ACTIVITY_TYPE);

      if (exportSpaceMetadata && isSpaceType) {
        if (space == null) {
          log.warn("Should export space DATA but it is null");
        } else {
          prefix = "answer/space/" + category.getId() + "/";
          exportTasks.add(new SpaceMetadataExportTask(space, prefix));
        }
      }
    }
  }

  @Override
  protected void refactorActivityComment(ExoSocialActivity activity, ExoSocialActivity comment) {
    if (activity.getTemplateParams().containsKey("Link") && activity.getTemplateParams().get("Link").contains("questionId=")) {
      String questionId = activity.getTemplateParams().get("Id");
      Question question = null;
      try {
        question = faqService.getQuestionById(questionId);
      } catch (Exception e) {
        throw new RuntimeException("error while retrieving question", e);
      }
      String commentActivityId = faqService.getActivityIdForComment(question.getPath(), comment.getId(), question.getLanguage());
      if (commentActivityId != null && commentActivityId.equals(activity.getId())) {
        activity.getTemplateParams().put("Link", comment.getId());
      }
    }
  }

  @Override
  public boolean isActivityValid(ExoSocialActivity activity) throws Exception {
    if (activity.isComment()) {
      return true;
    } else {
      String questionId = activity.getTemplateParams().get("Id");
      if (questionId == null) {
        log.warn("An activity that is not a question nor an answer nor a comment was found in Answer activities.");
        return false;
      }
      Question question = faqService.getQuestionById(questionId);
      if (question == null) {
        log.warn("Question not found. Cannot import activity '" + activity.getTitle() + "'.");
        return false;
      }
      if (categoryThreadLocal.get() == null) {
        log.warn("Cannot import activity, no category is selected for '" + activity.getTitle() + "'.");
        return false;
      }
      return categoryThreadLocal.get().getId().equals(question.getCategoryId());
    }
  }
}
