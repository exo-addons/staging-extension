package org.exoplatform.management.forum.operations;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.jcr.ImportUUIDBehavior;
import javax.jcr.Node;
import javax.jcr.Session;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.forum.common.jcr.KSDataLocation;
import org.exoplatform.forum.service.Category;
import org.exoplatform.forum.service.Forum;
import org.exoplatform.forum.service.ForumService;
import org.exoplatform.forum.service.Post;
import org.exoplatform.forum.service.Topic;
import org.exoplatform.forum.service.Utils;
import org.exoplatform.forum.service.impl.model.TopicFilter;
import org.exoplatform.management.common.AbstractJCROperationHandler;
import org.exoplatform.management.forum.ForumExtension;
import org.exoplatform.poll.service.Poll;
import org.exoplatform.poll.service.PollService;
import org.exoplatform.portal.config.UserACL;
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
public class ForumDataImportResource extends AbstractJCROperationHandler {

  final private static Logger log = LoggerFactory.getLogger(ForumDataImportResource.class);

  private ForumService forumService;
  private PollService pollService;
  private KSDataLocation dataLocation;

  private String type;

  final private static int BUFFER = 2048000;

  public ForumDataImportResource(boolean isSpaceForumType) {
    type = isSpaceForumType ? ForumExtension.SPACE_FORUM_TYPE : ForumExtension.PUBLIC_FORUM_TYPE;
  }

  @Override
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws OperationException {
    spaceService = operationContext.getRuntimeContext().getRuntimeComponent(SpaceService.class);
    forumService = operationContext.getRuntimeContext().getRuntimeComponent(ForumService.class);
    repositoryService = operationContext.getRuntimeContext().getRuntimeComponent(RepositoryService.class);
    dataLocation = operationContext.getRuntimeContext().getRuntimeComponent(KSDataLocation.class);
    userACL = operationContext.getRuntimeContext().getRuntimeComponent(UserACL.class);
    activityManager = operationContext.getRuntimeContext().getRuntimeComponent(ActivityManager.class);
    activityStorage = operationContext.getRuntimeContext().getRuntimeComponent(ActivityStorage.class);
    identityStorage = operationContext.getRuntimeContext().getRuntimeComponent(IdentityStorage.class);
    pollService = operationContext.getRuntimeContext().getRuntimeComponent(PollService.class);

    OperationAttributes attributes = operationContext.getAttributes();
    List<String> filters = attributes.getValues("filter");

    // "replace-existing" attribute. Defaults to false.
    boolean replaceExisting = filters.contains("replace-existing:true");

    // "create-space" attribute. Defaults to false.
    boolean createSpace = filters.contains("create-space:true");

    OperationAttachment attachment = operationContext.getAttachment(false);
    if (attachment == null) {
      throw new OperationException(OperationNames.IMPORT_RESOURCE, "No attachment available for forum import.");
    }

    InputStream attachmentInputStream = attachment.getStream();
    if (attachmentInputStream == null) {
      throw new OperationException(OperationNames.IMPORT_RESOURCE, "No data stream available for forum import.");
    }

    log.info("Importing Forums Data");

    String tempFolderPath = null;
    Map<String, List<String>> contentsByOwner = new HashMap<String, List<String>>();
    try {
      // extract data from zip
      tempFolderPath = extractDataFromZip(attachmentInputStream, contentsByOwner);
      String workspace = dataLocation.getWorkspace();

      for (String categoryId : contentsByOwner.keySet()) {
        boolean isSpaceForum = categoryId.contains(Utils.FORUM_SPACE_ID_PREFIX);
        if (isSpaceForum) {
          String forumId = categoryId;
          String spacePrettyName = forumId.replace(Utils.FORUM_SPACE_ID_PREFIX, "");
          String groupId = SpaceUtils.SPACE_GROUP + "/" + spacePrettyName;
          File file = new File(tempFolderPath + "/" + SpaceMetadataExportTask.getEntryPath(forumId));
          if (file.exists()) {
            boolean spaceCreatedOrAlreadyExists = createSpaceIfNotExists(file, groupId, createSpace);
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

          List<String> paths = contentsByOwner.get(forumId);

          Collections.sort(paths);
          File activitiesFile = null;
          for (String nodePath : paths) {
            if (nodePath.endsWith(ForumActivitiesExportTask.FILENAME)) {
              activitiesFile = new File(nodePath);
            } else {
              importNode(forumId, nodePath, workspace, tempFolderPath);
            }
          }
          // Refresh caches
          forumService.calculateDeletedUser("fakeUser" + Utils.DELETED);
          // Import activities
          if (activitiesFile != null && activitiesFile.exists()) {
            String spaceGroupName = forumId.replace(Utils.FORUM_SPACE_ID_PREFIX, "");
            String spaceGroupId = SpaceUtils.SPACE_GROUP + "/" + spaceGroupName;
            Space space = spaceService.getSpaceByGroupId(spaceGroupId);

            createActivities(activitiesFile, space.getPrettyName());
          }
        } else {
          Category category = forumService.getCategory(categoryId);

          if (category != null) {
            if (replaceExisting) {
              log.info("Overwrite existing Forum Category: '" + category.getCategoryName() + "'  (replace-existing=true)");
              List<Forum> forums = forumService.getForums(categoryId, null);
              for (Forum forum : forums) {
                deleteActivities(categoryId, forum.getId());
              }
            } else {
              log.info("Ignore existing Forum Category: '" + category.getCategoryName() + "'  (replace-existing=false)");
              continue;
            }
          }

          List<String> paths = contentsByOwner.get(categoryId);

          Collections.sort(paths);
          File activitiesFile = null;
          for (String nodePath : paths) {
            if (nodePath.endsWith(ForumActivitiesExportTask.FILENAME)) {
              activitiesFile = new File(nodePath);
            } else {
              importNode(categoryId, nodePath, workspace, tempFolderPath);
            }
          }
          // Refresh caches
          forumService.calculateDeletedUser("fakeUser" + Utils.DELETED);
          // Import activities
          if (activitiesFile != null && activitiesFile.exists()) {
            createActivities(activitiesFile, null);
          }
        }
      }
      forumService.calculateDeletedUser("fakeUser" + Utils.DELETED);
    } catch (Exception e) {
      throw new OperationException(OperationNames.IMPORT_RESOURCE, "Unable to import forum contents", e);
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

  @SuppressWarnings("unchecked")
  private void createActivities(File activitiesFile, String spacePrettyName) {
    log.info("Importing Forum activities");
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

    ExoSocialActivity topicActivity = null;
    ExoSocialActivity pollActivity = null;
    Topic topic = null;
    for (ExoSocialActivity exoSocialActivity : activitiesList) {
      try {
        exoSocialActivity.setId(null);
        // If poll activity
        if (exoSocialActivity.getTemplateParams().containsKey("PollLink")) {
          if (topic == null) {
            log.warn("A poll activity was found with non selected topic.");
            continue;
          }
          String topicId = exoSocialActivity.getTemplateParams().get("Id");
          if (!topicId.equals(topic.getId())) {
            log.warn("A poll activity was found for different topic.");
            continue;
          }
          topicActivity = null;
          saveActivity(exoSocialActivity, spacePrettyName);
          if (exoSocialActivity.getId() == null) {
            log.warn("Activity '" + exoSocialActivity.getTitle() + "' is not imported, id is null");
            continue;
          }
          String pollId = topicId.replace(Utils.TOPIC, Utils.POLL);
          Poll poll = pollService.getPoll(pollId);
          String pollPath = poll.getParentPath() + "/" + pollId;
          pollService.saveActivityIdForOwner(pollPath, exoSocialActivity.getId());
          pollActivity = exoSocialActivity;
          continue;
        }

        // Add poll comment
        if (pollActivity != null && topicActivity == null && exoSocialActivity.isComment()) {
          saveComment(pollActivity, exoSocialActivity);
          continue;
        }

        // In case of topic activity
        pollActivity = null;

        String catId = exoSocialActivity.getTemplateParams().get("CateId");
        if (catId == null) {
          if (exoSocialActivity.isComment() && topicActivity != null) {
            saveComment(topicActivity, exoSocialActivity);
          } else {
            log.warn("An activity that is not a topic nor a post nor a comment of a topic was found in Forum activities: " + exoSocialActivity.getTitle());
            topicActivity = null;
            continue;
          }
        } else {
          String forumId = exoSocialActivity.getTemplateParams().get("ForumId");
          String topicId = exoSocialActivity.getTemplateParams().get("TopicId");
          topic = forumService.getTopic(catId, forumId, topicId, userACL.getSuperUser());
          if (topic == null) {
            log.warn("Forum Topic not found. Cannot import activity '" + exoSocialActivity.getTitle() + "'.");
            topicActivity = null;
            continue;
          }
          if (exoSocialActivity.isComment()) {
            if (topicActivity == null) {
              String activityId = forumService.getActivityIdForOwnerId(topic.getId());
              if (activityId != null) {
                topicActivity = activityManager.getActivity(activityId);
              }
              if (topicActivity == null) {
                log.warn("Forum Topic activity is null. Cannot import activity '" + exoSocialActivity.getTitle() + "'.");
                continue;
              }
            }
            String postId = exoSocialActivity.getTemplateParams().get("PostId");
            if (postId != null) {
              Post post = forumService.getPost(catId, forumId, topicId, postId);
              if (post == null) {
                log.warn("Forum Post not found. Cannot import activity '" + exoSocialActivity.getTitle() + "'.");
                continue;
              }
              saveComment(topicActivity, exoSocialActivity);
              forumService.saveActivityIdForOwnerPath(post.getPath(), exoSocialActivity.getId());
            }
          } else {
            topicActivity = null;
            saveActivity(exoSocialActivity, spacePrettyName);
            if (exoSocialActivity.getId() == null) {
              log.warn("Activity '" + exoSocialActivity.getTitle() + "' is not imported, id is null");
              continue;
            }
            forumService.saveActivityIdForOwnerPath(topic.getPath(), exoSocialActivity.getId());
            topicActivity = exoSocialActivity;
          }
        }
      } catch (Exception e) {
        log.warn("Error while adding activity: " + exoSocialActivity.getTitle(), e);
      }
    }
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
          activityManager.deleteActivity(activity);
        }
      }
    }
  }

  /**
   * Extract data from zip
   * 
   * @param attachment
   * @return
   */
  public String extractDataFromZip(InputStream attachmentInputStream, Map<String, List<String>> contentsByOwner) throws Exception {
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
      tmpZipFile = File.createTempFile("staging-forum", ".zip");
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

  private void importNode(String id, String nodePath, String workspace, String tempFolderPath) throws Exception {
    if (!nodePath.startsWith("/")) {
      nodePath = "/" + nodePath;
    }
    String parentNodePath = nodePath.substring(0, nodePath.lastIndexOf("/"));
    parentNodePath = parentNodePath.replaceAll("//", "/");

    // Delete old node
    Session session = getSession(workspace);
    try {
      if (session.itemExists(nodePath) && session.getItem(nodePath) instanceof Node) {
        log.info("Deleting the node " + workspace + ":" + nodePath);

        Node oldNode = (Node) session.getItem(nodePath);
        oldNode.remove();
        session.save();
        session.refresh(false);
      }
    } catch (Exception e) {
      log.error("Error when trying to find and delete the node: '" + parentNodePath + "'. Ignore this node and continue.", e);
      return;
    } finally {
      if (session != null) {
        session.logout();
      }
    }

    // Import Node from Extracted Zip file
    session = getSession(workspace);
    FileInputStream fis = null;
    File xmlFile = null;
    try {
      log.info("Importing the node '" + nodePath + "'");

      // Create the parent path
      createJCRPath(session, parentNodePath);

      // Get XML file
      xmlFile = new File((tempFolderPath + "/" + replaceSpecialChars(ForumExportTask.getEntryPath(type, id, nodePath))).replaceAll("//", "/"));
      fis = new FileInputStream(xmlFile);

      session.refresh(false);
      session.importXML(parentNodePath, fis, ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
      session.save();
    } catch (Exception e) {
      log.error("Error when trying to import node: " + parentNodePath, e);
      // Revert changes
      session.refresh(false);
    } finally {
      if (session != null) {
        session.logout();
      }
      if (fis != null) {
        fis.close();
      }
      if (xmlFile != null) {
        xmlFile.delete();
      }
    }
  }

  private void extractFilesById(File tmpZipFile, String targetFolderPath, Map<String, List<String>> contentsByOwner) throws FileNotFoundException, IOException, Exception {
    // Open an input stream on local zip file
    NonCloseableZipInputStream zis = new NonCloseableZipInputStream(new FileInputStream(tmpZipFile));

    try {
      ZipEntry entry;
      while ((entry = zis.getNextEntry()) != null) {
        String filePath = entry.getName();
        // Skip entries not managed by this extension
        if (filePath.equals("") || !filePath.startsWith("forum/" + type + "/")) {
          continue;
        }

        // Skip directories
        if (entry.isDirectory()) {
          // Create directory in unzipped folder location
          createFile(new File(targetFolderPath + replaceSpecialChars(filePath)), true);
          continue;
        }

        // Skip non managed
        if (!filePath.endsWith(".xml") && !filePath.endsWith(SpaceMetadataExportTask.FILENAME) && !filePath.contains(ForumActivitiesExportTask.FILENAME)) {
          log.warn("Uknown file format found at location: '" + filePath + "'. Ignore it.");
          continue;
        }

        log.info("Receiving content " + filePath);

        // Put XML Export file in temp folder
        copyToDisk(zis, targetFolderPath + replaceSpecialChars(filePath));

        // Extract ID owner
        String id = extractIdFromPath(filePath);

        // Skip metadata file
        if (filePath.endsWith(SpaceMetadataExportTask.FILENAME)) {
          continue;
        }

        // Add nodePath by WikiOwner
        if (!contentsByOwner.containsKey(id)) {
          contentsByOwner.put(id, new ArrayList<String>());
        }
        if (filePath.contains(ForumActivitiesExportTask.FILENAME)) {
          // add activities metadata files
          contentsByOwner.get(id).add(targetFolderPath + replaceSpecialChars(filePath));
        } else {
          String nodePath = filePath.substring(filePath.indexOf(id + "/") + (id + "/").length(), filePath.lastIndexOf(".xml"));
          contentsByOwner.get(id).add(nodePath);
        }
      }
    } finally {
      if (zis != null) {
        zis.reallyClose();
      }
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
    int beginIndex = ("forum/" + type + "/").length();
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
