package org.exoplatform.management.answer.operations;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.exoplatform.faq.service.Answer;
import org.exoplatform.faq.service.Category;
import org.exoplatform.faq.service.Comment;
import org.exoplatform.faq.service.FAQService;
import org.exoplatform.faq.service.Question;
import org.exoplatform.faq.service.Utils;
import org.exoplatform.management.answer.AnswerExtension;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.identity.provider.SpaceIdentityProvider;
import org.exoplatform.social.core.manager.ActivityManager;
import org.exoplatform.social.core.space.SpaceUtils;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.social.core.storage.api.IdentityStorage;
import org.gatein.common.logging.Logger;
import org.gatein.common.logging.LoggerFactory;
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
public class AnswerDataImportResource implements OperationHandler {

  final private static Logger log = LoggerFactory.getLogger(AnswerDataImportResource.class);

  private SpaceService spaceService;
  private FAQService faqService;
  private UserACL userACL;
  private ActivityManager activityManager;
  private IdentityStorage identityStorage;

  private String type;

  final private static int BUFFER = 2048000;

  public AnswerDataImportResource(boolean isSpaceType) {
    type = isSpaceType ? AnswerExtension.SPACE_FAQ_TYPE : AnswerExtension.PUBLIC_FAQ_TYPE;
  }

  @Override
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws OperationException {
    spaceService = operationContext.getRuntimeContext().getRuntimeComponent(SpaceService.class);
    faqService = operationContext.getRuntimeContext().getRuntimeComponent(FAQService.class);
    userACL = operationContext.getRuntimeContext().getRuntimeComponent(UserACL.class);
    activityManager = operationContext.getRuntimeContext().getRuntimeComponent(ActivityManager.class);
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
          boolean spaceCreatedOrAlreadyExists = createSpaceIfNotExists(tempFolderPath, categoryId, createSpace);
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
        String activitiesFilePath = tempFolderPath + replaceSpecialChars(AnswerActivitiesExportTask.getEntryPath(type, categoryId));
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

  private File copyAttachementToLocalFolder(InputStream attachmentInputStream) throws IOException, FileNotFoundException {
    NonCloseableZipInputStream zis = null;
    File tmpZipFile = null;
    try {
      // Copy attachement to local File
      tmpZipFile = File.createTempFile("staging-answer", ".zip");
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
    return tmpZipFile;
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
    List<ExoSocialActivity> activitiesList = new ArrayList<ExoSocialActivity>();
    Identity identity = null;
    for (ExoSocialActivity activity : activities) {
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
      }
    }
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

  private void saveActivity(ExoSocialActivity activity, String spacePrettyName) {
    long updatedTime = activity.getUpdated().getTime();
    if (spacePrettyName == null) {
      activityManager.saveActivityNoReturn(activity);
      activity.setUpdated(updatedTime);
      activityManager.updateActivity(activity);
    } else {
      Identity spaceIdentity = getIdentity(spacePrettyName);
      if (spaceIdentity == null) {
        log.warn("Cannot get identity of space '" + spacePrettyName + "'");
        return;
      }
      activityManager.saveActivityNoReturn(spaceIdentity, activity);
      activity.setUpdated(updatedTime);
      activityManager.updateActivity(activity);
    }
    log.info("Answer activity : '" + activity.getTitle() + " is imported.");
  }

  private void saveComment(ExoSocialActivity activity, ExoSocialActivity comment) {
    long updatedTime = activity.getUpdated().getTime();
    activityManager.saveComment(activity, comment);
    activity = activityManager.getActivity(activity.getId());
    activity.setUpdated(updatedTime);
    activityManager.updateActivity(activity);
    log.info("Answer activity comment: '" + activity.getTitle() + " is imported.");
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
        if (!filePath.endsWith(".xml") && !filePath.endsWith(SpaceMetadataExportTask.FILENAME) && !filePath.endsWith(AnswerActivitiesExportTask.FILENAME)) {
          log.warn("Uknown file format found at location: '" + filePath + "'. Ignore it.");
          continue;
        }

        log.info("Receiving content " + filePath);

        // Put XML Export file in temp folder
        copyToDisk(zis, targetFolderPath + replaceSpecialChars(filePath));

        // Skip metadata file
        if (filePath.endsWith(SpaceMetadataExportTask.FILENAME) || filePath.endsWith(AnswerActivitiesExportTask.FILENAME)) {
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
  private String extractIdFromPath(String path) {
    int beginIndex = ("answer/" + type + "/").length();
    int endIndex = path.indexOf("/", beginIndex + 1);
    return path.substring(beginIndex, endIndex);
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

  private boolean createSpaceIfNotExists(String tempFolderPath, String faqId, boolean createSpace) throws Exception {
    String spaceId = faqId.replace(Utils.CATE_SPACE_ID_PREFIX, "");
    Space space = spaceService.getSpaceByGroupId(SpaceUtils.SPACE_GROUP + "/" + spaceId);
    if (space == null && createSpace) {
      FileInputStream spaceMetadataFile = new FileInputStream(tempFolderPath + "/" + SpaceMetadataExportTask.getEntryPath(faqId));
      try {
        // Unmarshall metadata xml file
        XStream xstream = new XStream();
        xstream.alias("metadata", SpaceMetaData.class);
        SpaceMetaData spaceMetaData = (SpaceMetaData) xstream.fromXML(spaceMetadataFile);

        log.info("Automatically create new space: '" + spaceMetaData.getPrettyName() + "'.");
        space = new Space();

        String originalSpacePrettyName = spaceMetaData.getGroupId().replace(SpaceUtils.SPACE_GROUP + "/", "");
        if (originalSpacePrettyName.equals(spaceMetaData.getPrettyName())) {
          space.setPrettyName(spaceMetaData.getPrettyName());
        } else {
          space.setPrettyName(originalSpacePrettyName);
        }
        space.setDisplayName(spaceMetaData.getDisplayName());
        space.setGroupId(spaceMetaData.getGroupId());
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
        if (!originalSpacePrettyName.equals(spaceMetaData.getPrettyName())) {
          spaceService.renameSpace(space, spaceMetaData.getDisplayName());
        }
        return true;
      } finally {
        if (spaceMetadataFile != null) {
          try {
            spaceMetadataFile.close();
          } catch (Exception e) {
            log.warn(e);
          }
        }
      }
    }
    return (space != null);
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

  private static File createFile(File file, boolean folder) throws Exception {
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

}
