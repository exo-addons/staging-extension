package org.exoplatform.management.answer.operations;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.exoplatform.faq.service.Answer;
import org.exoplatform.faq.service.Category;
import org.exoplatform.faq.service.Comment;
import org.exoplatform.faq.service.FAQService;
import org.exoplatform.faq.service.Question;
import org.exoplatform.faq.service.Utils;
import org.exoplatform.management.answer.AnswerExtension;
import org.exoplatform.management.common.FileEntry;
import org.exoplatform.management.common.exportop.ActivitiesExportTask;
import org.exoplatform.management.common.exportop.SpaceMetadataExportTask;
import org.exoplatform.management.common.importop.AbstractImportOperationHandler;
import org.exoplatform.management.common.importop.ActivityImportOperationInterface;
import org.exoplatform.management.common.importop.FileImportOperationInterface;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.manager.ActivityManager;
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
public class AnswerDataImportResource extends AbstractImportOperationHandler implements ActivityImportOperationInterface, FileImportOperationInterface {

  final private static Logger log = LoggerFactory.getLogger(AnswerDataImportResource.class);

  private FAQService faqService;

  private String type;

  public AnswerDataImportResource(boolean isSpaceType) {
    type = isSpaceType ? AnswerExtension.SPACE_FAQ_TYPE : AnswerExtension.PUBLIC_FAQ_TYPE;
  }

  @Override
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws OperationException {
    spaceService = operationContext.getRuntimeContext().getRuntimeComponent(SpaceService.class);
    faqService = operationContext.getRuntimeContext().getRuntimeComponent(FAQService.class);
    userACL = operationContext.getRuntimeContext().getRuntimeComponent(UserACL.class);
    activityManager = operationContext.getRuntimeContext().getRuntimeComponent(ActivityManager.class);
    activityStorage = operationContext.getRuntimeContext().getRuntimeComponent(ActivityStorage.class);
    identityStorage = operationContext.getRuntimeContext().getRuntimeComponent(IdentityStorage.class);

    OperationAttributes attributes = operationContext.getAttributes();
    List<String> filters = attributes.getValues("filter");

    // "replace-existing" attribute. Defaults to false.
    boolean replaceExisting = filters.contains("replace-existing:true");
    boolean deleteNewCategories = filters.contains("delete-newcategories:true");
    boolean deleteNewQuestions = filters.contains("delete-newquestions:true");

    // "create-space" attribute. Defaults to false.
    boolean createSpace = filters.contains("create-space:true");

    InputStream attachmentInputStream = getAttachementInputStream(operationContext);
    try {
      // extract data from zip
      Map<String, List<FileEntry>> contentsByOwner = extractDataFromZip(attachmentInputStream);

      for (String categoryId : contentsByOwner.keySet()) {
        List<FileEntry> fileEntries = contentsByOwner.get(categoryId);
        FileEntry spaceMetadataFile = getAndRemoveFileByPath(fileEntries, SpaceMetadataExportTask.FILENAME);
        FileEntry activitiesFile = getAndRemoveFileByPath(fileEntries, ActivitiesExportTask.FILENAME);

        for (FileEntry fileEntry : fileEntries) {
          importAnswerData(fileEntry.getFile(), spaceMetadataFile == null ? null : spaceMetadataFile.getFile(), replaceExisting, deleteNewCategories, deleteNewQuestions, createSpace);
        }

        boolean isSpaceFAQ = categoryId.contains(Utils.CATE_SPACE_ID_PREFIX);
        if (isSpaceFAQ) {
          if (activitiesFile != null && activitiesFile.getFile().exists()) {
            String spacePrettyName = categoryId.replace(Utils.CATE_SPACE_ID_PREFIX, "");
            Space space = spaceService.getSpaceByPrettyName(spacePrettyName);

            log.info("Importing Answer activities");
            importActivities(activitiesFile.getFile(), space == null ? null : spacePrettyName, true);
          }
        }
      }
      // To refresh caches
      faqService.calculateDeletedUser("fakeUser" + Utils.DELETED);
    } catch (Exception e) {
      throw new OperationException(OperationNames.IMPORT_RESOURCE, "Unable to import FAQ contents", e);
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
  }

  @SuppressWarnings("unchecked")
  private void importAnswerData(File file, File spaceMetadataFile, boolean replaceExisting, boolean deleteNewCategories, boolean deleteNewQuestions, boolean createSpace) throws Exception {
    List<Object> objects = (List<Object>) deserializeObject(file, null, null);
    // object i : the category, object i+1 : the list of category questions
    for (int i=0 ; i<objects.size(); i+=2) {
      Category category = (Category) objects.get(i);
      String parentPath = getParentPath(category);
      List<Question> questions = (List<Question>) objects.get(i+1);
      Category parentCategory = faqService.getCategoryById(parentPath);
      if (parentCategory == null) {
        log.warn("Parent Answer Category of Category '" + category.getName() + "' doesn't exist, ignore import operation for this category.");
        return;
      }

      if (spaceMetadataFile != null && spaceMetadataFile.exists()) {
        boolean spaceCreatedOrAlreadyExists = createSpaceIfNotExists(spaceMetadataFile, createSpace);
        if (!spaceCreatedOrAlreadyExists) {
          log.warn("Import of Answer category '" + parentCategory.getName() + "' is ignored. Turn on 'create-space:true' option if you want to automatically create the space.");
        }
      }

      Category toReplaceCategory = faqService.getCategoryById(category.getId());
      if (toReplaceCategory != null) {
        if (replaceExisting ) {
          log.info("Overwrite existing FAQ Category: '" + toReplaceCategory.getName() + "'  (replace-existing=true)");
          deleteActivities(toReplaceCategory.getId(), null);
          if(deleteNewQuestions) {
           List<Question> targetQuestions = faqService.getAllQuestionsByCatetory(toReplaceCategory.getId(), AnswerExtension.EMPTY_FAQ_SETTIGNS).getAll();
           removeCategoryQuestions(toReplaceCategory, targetQuestions);
         } else {
            removeCategoryQuestions(toReplaceCategory, questions);
          }
        } else {
          log.info("Ignore existing FAQ Category: '" + category.getName() + "'  (replace-existing=false)");
          return;
        }
        if(!category.getName().equals(toReplaceCategory.getName())){
          faqService.saveCategory(parentPath,category,false);
        }
      } else {
        faqService.saveCategory(parentPath, category, true);
      }

      // FIXME Exception swallowed FORUM-971, so we have to make test
      if (faqService.getCategoryById(category.getId()) == null) {
        throw new RuntimeException("Category isn't imported");
      }

      for (Question question : questions) {
        faqService.saveQuestion(question, true, AnswerExtension.EMPTY_FAQ_SETTIGNS);
        if (question.getAnswers() != null) {
          for (Answer answer : question.getAnswers()) {
            answer.setNew(true);
            faqService.saveAnswer(question.getPath(), answer, true);
          }
        }
        if (question.getComments() != null) {
          for (Comment comment : question.getComments()) {
            comment.setNew(true);
            faqService.saveComment(question.getPath(), comment, question.getLanguage());
          }
        }
      }
      if(deleteNewCategories){
        removeNewCategories(objects,category);
      }
      deleteActivities(category.getId(), questions);
    }
  }

  private void deleteActivities(String categoryId, List<Question> questions) throws Exception {
    // Delete Answer activity stream
    if (questions == null) {
      questions = faqService.getQuestionsByCatetory(categoryId, AnswerExtension.EMPTY_FAQ_SETTIGNS).getAll();
    }
    for (Question question : questions) {
      deleteActivity(faqService.getActivityIdForQuestion(question.getId()));
    }
  }

  @Override
  public String getManagedFilesPrefix() {
    return "answer/" + type + "/";
  }

  @Override
  public boolean isUnKnownFileFormat(String filePath) {
    return !filePath.endsWith(AnswerExportTask.FILENAME) && !filePath.endsWith(SpaceMetadataExportTask.FILENAME) && !filePath.endsWith(ActivitiesExportTask.FILENAME);
  }

  @Override
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

  @Override
  public String extractIdFromPath(String path) {
    int beginIndex = ("answer/" + type + "/").length();
    int endIndex = path.indexOf("/", beginIndex + 1);
    return path.substring(beginIndex, endIndex);
  }

  @Override
  public String getNodePath(String filePath) {
    return filePath;
  }

  @Override
  public void attachActivityToEntity(ExoSocialActivity activity, ExoSocialActivity comment) throws Exception {
    String questionId = activity.getTemplateParams().get("Id");
    Question question = faqService.getQuestionById(questionId);
    if (comment == null) {
      faqService.saveActivityIdForQuestion(questionId, activity.getId());
    } else {
      String linkId = comment.getTemplateParams().get("Link");
      if (linkId == null) {
        return;
      } else if (linkId.startsWith("Comment")) {
        faqService.saveActivityIdForComment(question.getPath(), linkId, question.getLanguage(), comment.getId());
      } else if (linkId != null && linkId.startsWith("Answer")) {
        Answer answer = faqService.getAnswerById(question.getPath(), linkId);
        faqService.saveActivityIdForAnswer(question.getPath(), answer, comment.getId());
      }
    }
  }

  @Override
  public boolean isActivityNotValid(ExoSocialActivity activity, ExoSocialActivity comment) throws Exception {
    if (activity.getTemplateParams() == null) {
      log.warn("Answer Activity TemplateParams is null, can't process activity: '" + activity.getTitle() + "'.");
      return true;
    }
    String questionId = activity.getTemplateParams().get("Id");
    if (questionId == null && comment == null) {
      log.warn("An activity that is not a question nor a answer nor a comment was found in Answer activities.");
      return true;
    }
    Question question = faqService.getQuestionById(questionId);
    if (question == null) {
      log.warn("Question not found. Cannot import activity '" + activity.getTitle() + "'.");
      return true;
    }
    if (comment != null) {
      String linkId = comment.getTemplateParams().get("Link");
      if (linkId == null) {
        return false;
      } else if (linkId.startsWith("Comment")) {
        Comment faqComment = faqService.getCommentById(question.getPath(), linkId);
        if (faqComment == null) {
          log.warn("Answer Comment not found. Cannot import activity '" + comment.getTitle() + "'.");
          return true;
        }
      } else if (linkId != null && linkId.startsWith("Answer")) {
        Answer answer = faqService.getAnswerById(question.getPath(), linkId);
        if (answer == null) {
          log.warn("Question's answer not found. Cannot import activity '" + comment.getTitle() + "'.");
          return true;
        }
      } else {
        log.warn("Question's comment activity not well formated. linkId param = '" + linkId + "'.");
        return true;
      }
    }
    return false;
  }
  private void removeCategoryQuestions(Category category, List<Question> questions){
    try {
      List<Question> catQuestions = faqService.getAllQuestionsByCatetory(category.getId(),AnswerExtension.EMPTY_FAQ_SETTIGNS).getAll();
      for(Question question : catQuestions){
        for(Question question1 : questions){
          if(question1.getPath().equals(question.getPath())){
            faqService.removeQuestion(question.getPath());
          }
        }
      }
    } catch (Exception e) {
      log.error("Fail to remove questions of category: "+category.getName(),e);
    }
  }
  private void removeNewCategories(List<Object> objectList,Category category){
    try {
      Category toReplaceCategory = faqService.getCategoryById(category.getId());
      if(toReplaceCategory==null){
        return;
      }
      List<Category> targetCategoriesList = faqService.getSubCategories(category.getPath(),AnswerExtension.EMPTY_FAQ_SETTIGNS,true,null);
      if(targetCategoriesList==null || targetCategoriesList.size()==0){
        return;
      }
      List<Category> sourceCategoriesList = new ArrayList<Category>();
      String categoryPath = category.getPath();
      for(int i=0; i<objectList.size(); i+=2){
        Category sub_category = (Category) objectList.get(i);
        if(getParentPath(sub_category).equals(categoryPath)){
          sourceCategoriesList.add(sub_category);
        }
      }
      for(Category sub_category : targetCategoriesList){
        if(!sourceCategoriesList.contains(sub_category)){
          log.info("Delete the category: '" + sub_category.getName() + "' because it's only found here (delete-newcategories=true)");
          deleteActivities(sub_category.getId(), null);
          faqService.removeCategory(sub_category.getPath());
        }
      }
    } catch (Exception e) {
      log.error("Error when trying to delete new categories",e);
    }
  }

  private String getParentPath(Category category){
    return category.getPath().replace("/" + category.getId(), "");
  }
}
