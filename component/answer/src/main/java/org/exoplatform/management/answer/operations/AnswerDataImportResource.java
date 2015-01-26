package org.exoplatform.management.answer.operations;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;

import org.apache.commons.io.FileUtils;
import org.exoplatform.faq.service.Answer;
import org.exoplatform.faq.service.Category;
import org.exoplatform.faq.service.Comment;
import org.exoplatform.faq.service.FAQService;
import org.exoplatform.faq.service.Question;
import org.exoplatform.faq.service.Utils;
import org.exoplatform.management.answer.AnswerExtension;
import org.exoplatform.management.common.AbstractOperationHandler;
import org.exoplatform.management.common.ActivitiesExportTask;
import org.exoplatform.management.common.SpaceMetadataExportTask;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.manager.ActivityManager;
import org.exoplatform.social.core.space.SpaceUtils;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.social.core.storage.api.ActivityStorage;
import org.exoplatform.social.core.storage.api.IdentityStorage;
import org.gatein.common.logging.Logger;
import org.gatein.common.logging.LoggerFactory;
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
public class AnswerDataImportResource extends AbstractOperationHandler {

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

    // "create-space" attribute. Defaults to false.
    boolean createSpace = filters.contains("create-space:true");

    OperationAttachment attachment = operationContext.getAttachment(false);
    if (attachment == null) {
      throw new OperationException(OperationNames.IMPORT_RESOURCE, "No attachment available for FAQ import.");
    }

    InputStream attachmentInputStream = attachment.getStream();
    if (attachmentInputStream == null) {
      throw new OperationException(OperationNames.IMPORT_RESOURCE, "No data stream available for FAQ import.");
    }

    String tempFolderPath = null;
    Map<String, String> contentsByOwner = new HashMap<String, String>();
    try {
      // extract data from zip
      tempFolderPath = extractDataFromZip(attachmentInputStream, contentsByOwner);
      for (String categoryId : contentsByOwner.keySet()) {
        boolean isSpaceFAQ = categoryId.contains(Utils.CATE_SPACE_ID_PREFIX);
        if (isSpaceFAQ) {
          String groupId = SpaceUtils.SPACE_GROUP + "/" + categoryId.replace(Utils.CATE_SPACE_ID_PREFIX, "");
          File spaceMetadataFile = new File(tempFolderPath + "/" + getEntryPath(categoryId));

          boolean spaceCreatedOrAlreadyExists = createSpaceIfNotExists(spaceMetadataFile, groupId, createSpace);
          if (!spaceCreatedOrAlreadyExists) {
            log.warn("Import of Answer category '" + categoryId + "' is ignored. Turn on 'create-space:true' option if you want to automatically create the space.");
            continue;
          }
          String filePath = contentsByOwner.get(categoryId);

          importAnswerData(tempFolderPath, filePath, replaceExisting);
        } else {
          String filePath = contentsByOwner.get(categoryId);
          importAnswerData(tempFolderPath, filePath, replaceExisting);
        }
      }

      // To refresh caches
      faqService.calculateDeletedUser("fakeUser" + Utils.DELETED);

      Set<String> categoryIds = contentsByOwner.keySet();
      for (String categoryId : categoryIds) {
        String activitiesFilePath = tempFolderPath + replaceSpecialChars(getEntryPath(type, categoryId));
        File activitiesFile = new File(activitiesFilePath);
        if (activitiesFile.exists()) {
          String spacePrettyName = null;
          boolean isSpaceFAQ = categoryId.contains(Utils.CATE_SPACE_ID_PREFIX);
          if (isSpaceFAQ) {
            spacePrettyName = categoryId.replace(Utils.CATE_SPACE_ID_PREFIX, "");
          }
          createActivities(activitiesFile, spacePrettyName);
        }
      }
    } catch (Exception e) {
      throw new OperationException(OperationNames.IMPORT_RESOURCE, "Unable to import FAQ contents", e);
    } finally {
      if (tempFolderPath != null) {
        try {
          FileUtils.deleteDirectory(new File(tempFolderPath));
        } catch (IOException e) {
          log.warn("Unable to delete temp folder: " + tempFolderPath + ". Not blocker.", e);
        }
      }

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

  public static String getEntryPath(String type, String id) {
    return new StringBuilder("answer/").append(type).append("/").append(id).append(ActivitiesExportTask.FILENAME).toString();
  }

  public static String getEntryPath(String faqId) {
    return new StringBuilder("answer/space/").append(faqId).append("/").append(SpaceMetadataExportTask.FILENAME).toString();
  }

  /**
   * Extract data from zip
   * 
   * @param attachment
   * @return
   */
  private String extractDataFromZip(InputStream attachmentInputStream, Map<String, String> contentsByOwner) throws Exception {
    File tmpZipFile = null;
    String targetFolderPath = null;
    try {
      tmpZipFile = copyAttachementToLocalFolder(attachmentInputStream);

      // Get path of folder where to unzip files
      targetFolderPath = tmpZipFile.getAbsolutePath().replaceAll("\\.zip$", "") + "/";

      // Organize File paths by id and extract files from zip to a temp
      // folder
      extractFilesById(tmpZipFile, targetFolderPath, contentsByOwner);
    } finally {
      if (tmpZipFile != null) {
        try {
          FileUtils.forceDelete(tmpZipFile);
        } catch (Exception e) {
          log.warn("Unable to delete temp file: " + tmpZipFile.getAbsolutePath() + ". Not blocker.");
          tmpZipFile.deleteOnExit();
        }
      }
    }
    return targetFolderPath;
  }

  private void importAnswerData(String targetFolderPath, String filePath, boolean replaceExisting) throws Exception {
    // Unmarshall answer category data file
    XStream xStream = new XStream();

    @SuppressWarnings("unchecked")
    List<Object> objects = (List<Object>) xStream.fromXML(FileUtils.readFileToString(new File(targetFolderPath + replaceSpecialChars(filePath)), "UTF-8"));

    Category category = (Category) objects.get(0);
    @SuppressWarnings("unchecked")
    List<Question> questions = (List<Question>) objects.get(1);

    String parentId = category.getPath().replace("/" + category.getId(), "");

    Category parentCategory = faqService.getCategoryById(parentId);
    if (parentCategory == null) {
      log.warn("Parent Answer Category of Category '" + category.getName() + "' doesn't exist, ignore import operation for this category.");
      return;
    }

    Category toReplaceCategory = faqService.getCategoryById(category.getId());
    if (toReplaceCategory != null) {
      if (replaceExisting) {
        log.info("Overwrite existing FAQ Category: '" + toReplaceCategory.getName() + "'  (replace-existing=true)");
        deleteActivities(category.getId(), null);
        faqService.removeCategory(category.getPath());

        // FIXME Exception swallowed FORUM-971, so we have to make test
        if (faqService.getCategoryById(category.getId()) != null) {
          throw new RuntimeException("Cannot delete category: " + category.getName() + ". Internal error.");
        }
      } else {
        log.info("Ignore existing FAQ Category: '" + category.getName() + "'  (replace-existing=false)");
        return;
      }
    }

    faqService.saveCategory(parentId, category, true);

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
    deleteActivities(category.getId(), questions);
  }

  @SuppressWarnings("unchecked")
  private void createActivities(File activitiesFile, String spacePrettyName) {
    log.info("Importing Answer activities");

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

    List<ExoSocialActivity> activitiesList = sanitizeContent(activities);

    ExoSocialActivity questionActivity = null;
    String questionId = null;
    Question question = null;
    for (ExoSocialActivity exoSocialActivity : activitiesList) {
      try {
        exoSocialActivity.setId(null);
        if (exoSocialActivity.getTemplateParams() == null) {
          log.warn("Answer Activity TemplateParams is null, can't process activity: '" + exoSocialActivity.getTitle() + "'.");
          questionActivity = null;
          question = null;
          continue;
        }
        questionId = exoSocialActivity.getTemplateParams().containsKey("Id") ? exoSocialActivity.getTemplateParams().get("Id") : questionId;
        String linkId = exoSocialActivity.getTemplateParams().get("Link");
        if (linkId == null) {
          if (questionActivity != null && exoSocialActivity.isComment()) {
            saveComment(questionActivity, exoSocialActivity);
          } else {
            log.warn("An activity that is not a question nor a answer nor a comment was found in Answer activities.");
            questionActivity = null;
            question = null;
            continue;
          }
        } else {
          if (exoSocialActivity.isComment()) {
            if (questionId == null || question == null) {
              log.warn("Answer Activity comment was found with no question.");
              continue;
            }
            if (linkId != null && linkId.startsWith("Comment")) {
              Comment comment = faqService.getCommentById(question.getPath(), linkId);
              if (comment == null) {
                log.warn("Answer Comment not found. Cannot import activity '" + exoSocialActivity.getTitle() + "'.");
                continue;
              }
              saveComment(questionActivity, exoSocialActivity);
              faqService.saveActivityIdForComment(question.getPath(), comment.getId(), question.getLanguage(), exoSocialActivity.getId());
            } else if (linkId != null && linkId.startsWith("Answer")) {
              Answer answer = faqService.getAnswerById(question.getPath(), linkId);
              if (answer == null) {
                log.warn("Question's answer not found. Cannot import activity '" + exoSocialActivity.getTitle() + "'.");
                continue;
              }
              saveComment(questionActivity, exoSocialActivity);
              faqService.saveActivityIdForAnswer(question.getPath(), answer, exoSocialActivity.getId());
            } else {
              log.warn("Comment Activity of Type 'Answer application' was found but is not for an answer nor for a comment");
            }
          } else {
            question = faqService.getQuestionById(questionId);
            if (question == null) {
              log.warn("Question not found. Cannot import activity '" + exoSocialActivity.getTitle() + "'.");
              continue;
            }
            saveActivity(exoSocialActivity, spacePrettyName);
            if (exoSocialActivity.getId() == null) {
              log.warn("Activity '" + exoSocialActivity.getTitle() + "' is not imported, id is null");
              continue;
            }
            faqService.saveActivityIdForQuestion(questionId, exoSocialActivity.getId());
            questionActivity = exoSocialActivity;
          }
        }
      } catch (Exception e) {
        log.warn("Error while adding activity: " + exoSocialActivity.getTitle(), e);
      }
    }
  }

  private void deleteActivities(String categoryId, List<Question> questions) throws Exception {
    // Delete Answer activity stream
    if (questions == null) {
      questions = faqService.getQuestionsByCatetory(categoryId, AnswerExtension.EMPTY_FAQ_SETTIGNS).getAll();
    }
    for (Question question : questions) {
      ExoSocialActivity activity = activityManager.getActivity(faqService.getActivityIdForQuestion(question.getId()));
      if (activity != null) {
        activityManager.deleteActivity(activity);
      }
    }
  }

  private void extractFilesById(File tmpZipFile, String targetFolderPath, Map<String, String> contentsByOwner) throws FileNotFoundException, IOException, Exception {
    NonCloseableZipInputStream zis;
    // Open an input stream on local zip file
    zis = new NonCloseableZipInputStream(new FileInputStream(tmpZipFile));

    try {
      ZipEntry entry;
      while ((entry = zis.getNextEntry()) != null) {
        String filePath = entry.getName();
        // Skip entries not managed by this extension
        if (filePath.equals("") || !filePath.startsWith("answer/" + type + "/")) {
          continue;
        }

        // Skip directories
        if (entry.isDirectory()) {
          // Create directory in unzipped folder location
          createFile(new File(targetFolderPath + replaceSpecialChars(filePath)), true);
          continue;
        }

        // Skip non managed
        if (!filePath.endsWith(".xml") && !filePath.endsWith(SpaceMetadataExportTask.FILENAME) && !filePath.endsWith(ActivitiesExportTask.FILENAME)) {
          log.warn("Uknown file format found at location: '" + filePath + "'. Ignore it.");
          continue;
        }

        log.info("Receiving content " + filePath);

        File file = new File(targetFolderPath + replaceSpecialChars(filePath));
        // Put XML Export file in temp folder
        copyToDisk(zis, file);

        // Skip metadata file
        if (filePath.endsWith(SpaceMetadataExportTask.FILENAME) || filePath.endsWith(ActivitiesExportTask.FILENAME)) {
          continue;
        }

        // Extract ID owner
        String id = extractIdFromPath(filePath);

        // Add nodePath by answer ID
        if (contentsByOwner.containsKey(id)) {
          log.warn("Two different files was found for Answer category: \r\n\t-" + contentsByOwner.get(id) + "\r\n\t-" + filePath + "\r\n. Ignore the new one.");
          continue;
        }
        contentsByOwner.put(id, filePath);
      }
    } finally {
      zis.reallyClose();
    }
  }

  /**
   * Extract Wiki owner from the file path
   * 
   * @param path
   *          The path of the file
   * @return The Wiki owner
   */
  protected String extractIdFromPath(String path) {
    int beginIndex = ("answer/" + type + "/").length();
    int endIndex = path.indexOf("/", beginIndex + 1);
    return path.substring(beginIndex, endIndex);
  }

}
