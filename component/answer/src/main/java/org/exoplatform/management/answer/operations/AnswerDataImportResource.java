package org.exoplatform.management.answer.operations;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.exoplatform.faq.service.Answer;
import org.exoplatform.faq.service.Category;
import org.exoplatform.faq.service.Comment;
import org.exoplatform.faq.service.FAQService;
import org.exoplatform.faq.service.Question;
import org.exoplatform.faq.service.Utils;
import org.exoplatform.management.answer.AnswerExtension;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.social.core.space.SpaceUtils;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
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
      tmpZipFile.deleteOnExit();
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
        if (!filePath.endsWith(".xml") && !filePath.endsWith(SpaceMetadataExportTask.FILENAME)) {
          log.warn("Uknown file format found at location: '" + filePath + "'. Ignore it.");
          continue;
        }

        log.info("Receiving content " + filePath);

        // Put XML Export file in temp folder
        copyToDisk(zis, targetFolderPath + replaceSpecialChars(filePath));

        // Skip metadata file
        if (filePath.endsWith(SpaceMetadataExportTask.FILENAME)) {
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
    return name.replaceAll(":", "_");
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
