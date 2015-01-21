package org.exoplatform.management.wiki.operations;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.jcr.ImportUUIDBehavior;
import javax.jcr.Node;
import javax.jcr.Session;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.exoplatform.commons.utils.ActivityTypeUtils;
import org.exoplatform.management.common.AbstractJCROperationHandler;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.manager.ActivityManager;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.social.core.storage.api.ActivityStorage;
import org.exoplatform.social.core.storage.api.IdentityStorage;
import org.exoplatform.wiki.mow.api.Page;
import org.exoplatform.wiki.mow.api.Wiki;
import org.exoplatform.wiki.mow.api.WikiType;
import org.exoplatform.wiki.mow.core.api.MOWService;
import org.exoplatform.wiki.mow.core.api.wiki.PageImpl;
import org.exoplatform.wiki.service.WikiService;
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
 * Created by The eXo Platform SAS Author : eXoPlatform exo@exoplatform.com Mar
 * 5, 2014
 */
public class WikiDataImportResource extends AbstractJCROperationHandler {

  final private static Logger log = LoggerFactory.getLogger(WikiDataImportResource.class);

  private WikiType wikiType;

  private MOWService mowService;

  private WikiService wikiService;

  public WikiDataImportResource(WikiType wikiType) {
    this.wikiType = wikiType;
  }

  @Override
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws OperationException {
    spaceService = operationContext.getRuntimeContext().getRuntimeComponent(SpaceService.class);
    userACL = operationContext.getRuntimeContext().getRuntimeComponent(UserACL.class);
    mowService = operationContext.getRuntimeContext().getRuntimeComponent(MOWService.class);
    repositoryService = operationContext.getRuntimeContext().getRuntimeComponent(RepositoryService.class);
    activityManager = operationContext.getRuntimeContext().getRuntimeComponent(ActivityManager.class);
    activityStorage = operationContext.getRuntimeContext().getRuntimeComponent(ActivityStorage.class);
    identityStorage = operationContext.getRuntimeContext().getRuntimeComponent(IdentityStorage.class);
    wikiService = operationContext.getRuntimeContext().getRuntimeComponent(WikiService.class);

    OperationAttributes attributes = operationContext.getAttributes();
    List<String> filters = attributes.getValues("filter");

    // "replace-existing" attribute. Defaults to false.
    boolean replaceExisting = filters.contains("replace-existing:true");

    // "create-space" attribute. Defaults to false.
    boolean createSpace = filters.contains("create-space:true");

    OperationAttachment attachment = operationContext.getAttachment(false);
    if (attachment == null) {
      throw new OperationException(OperationNames.IMPORT_RESOURCE, "No attachment available for Wiki import.");
    }

    InputStream attachmentInputStream = attachment.getStream();
    if (attachmentInputStream == null) {
      throw new OperationException(OperationNames.IMPORT_RESOURCE, "No data stream available for Wiki import.");
    }

    Map<String, List<String>> contentsByOwner = new HashMap<String, List<String>>();
    Map<String, File> templfilesByJCRPath = new HashMap<String, File>();
    try {
      // extract data from zip
      extractDataFromZip(attachmentInputStream, contentsByOwner, templfilesByJCRPath);

      for (String wikiOwner : contentsByOwner.keySet()) {
        if (WikiType.GROUP.equals(wikiType)) {
          File spaceMetadataFile = templfilesByJCRPath.get(wikiOwner + SpaceMetadataExportTask.FILENAME);
          if (spaceMetadataFile != null && spaceMetadataFile.exists()) {
            boolean spaceCreatedOrAlreadyExists = createSpaceIfNotExists(spaceMetadataFile, wikiOwner, createSpace);
            if (!spaceCreatedOrAlreadyExists) {
              log.warn("Import of wiki '" + wikiOwner + "' is ignored. Turn on 'create-space:true' option if you want to automatically create the space.");
              continue;
            }
          }
        }

        Wiki wiki = mowService.getModel().getWikiStore().getWikiContainer(wikiType).contains(wikiOwner);
        if (wiki != null) {
          if (replaceExisting) {
            log.info("Overwrite existing wiki for owner : '" + wikiType + ":" + wikiOwner + "' (replace-existing=true). Delete: " + wiki.getWikiHome().getJCRPageNode().getPath());
            deleteActivities(wiki.getType(), wiki.getOwner());
          } else {
            log.info("Ignore existing wiki for owner : '" + wikiType + ":" + wikiOwner + "' (replace-existing=false).");
            continue;
          }
        } else {
          wiki = mowService.getModel().getWikiStore().getWikiContainer(wikiType).addWiki(wikiOwner);
        }

        String workspace = mowService.getSession().getJCRSession().getWorkspace().getName();

        List<String> paths = contentsByOwner.get(wikiOwner);

        Collections.sort(paths);
        for (String nodePath : paths) {
          importNode(wikiOwner, nodePath, workspace, templfilesByJCRPath);
        }

        File activitiesFile = templfilesByJCRPath.get(wikiOwner + WikiActivitiesExportTask.FILENAME);
        // Import activities
        if (activitiesFile != null && activitiesFile.exists()) {
          String spacePrettyName = null;
          if (WikiType.GROUP.equals(wikiType)) {
            Space space = spaceService.getSpaceByGroupId(wikiOwner);
            spacePrettyName = space.getPrettyName();
          }
          createActivities(activitiesFile, spacePrettyName);
        }
      }
    } catch (Exception e) {
      throw new OperationException(OperationNames.IMPORT_RESOURCE, "Unable to import wiki content of type: " + wikiType, e);
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

  /**
   * Extract data from zip
   * 
   * @param templfilesByJCRPath
   * 
   * @param attachment
   * @return
   */
  public void extractDataFromZip(InputStream attachmentInputStream, Map<String, List<String>> contentsByOwner, Map<String, File> templfilesByJCRPath) throws Exception {
    File tmpZipFile = null;
    String targetFolderPath = null;
    try {
      tmpZipFile = copyAttachementToLocalFolder(attachmentInputStream);

      // Organize File paths by wikiOwner and extract files from zip to a temp
      // folder
      extractFilesByWikiOwner(tmpZipFile, targetFolderPath, contentsByOwner, templfilesByJCRPath);
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
  }

  private File copyAttachementToLocalFolder(InputStream attachmentInputStream) throws IOException, FileNotFoundException {
    NonCloseableZipInputStream zis = null;
    File tmpZipFile = null;
    try {
      // Copy attachement to local File
      tmpZipFile = File.createTempFile("staging-wiki", ".zip");
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

  private void importNode(String wikiOwner, String nodePath, String workspace, Map<String, File> templfilesByJCRPath) throws Exception {
    String parentNodePath = nodePath.substring(0, nodePath.lastIndexOf("/"));
    parentNodePath = parentNodePath.replaceAll("//", "/");

    File xmlFile = templfilesByJCRPath.get(nodePath);

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
    try {
      log.info("Importing the node '" + nodePath + "'");

      // Create the parent path
      createJCRPath(session, parentNodePath);

      // Get XML file
      fis = new FileInputStream(xmlFile);

      session.refresh(false);
      session.importXML(parentNodePath, fis, ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
      session.save();
    } catch (Exception e) {
      log.error("Error when trying to import node: " + nodePath, e);
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

  private void extractFilesByWikiOwner(File tmpZipFile, String targetFolderPath, Map<String, List<String>> contentsByOwner, Map<String, File> templfilesByJCRPath) throws FileNotFoundException,
      IOException, Exception {
    // Open an input stream on local zip file
    NonCloseableZipInputStream zis = new NonCloseableZipInputStream(new FileInputStream(tmpZipFile));

    try {
      ZipEntry entry;
      while ((entry = zis.getNextEntry()) != null) {
        String filePath = entry.getName();
        // Skip entries not managed by this extension
        if (filePath.equals("") || !filePath.startsWith("wiki/" + wikiType.name().toLowerCase() + "/")) {
          continue;
        }

        // Skip directories
        if (entry.isDirectory()) {
          continue;
        }

        // Skip non managed
        if (!filePath.endsWith(".xml") && !filePath.endsWith(SpaceMetadataExportTask.FILENAME) && !filePath.contains(WikiActivitiesExportTask.FILENAME)) {
          log.warn("Uknown file format found at location: '" + filePath + "'. Ignore it.");
          continue;
        }

        log.info("Receiving content " + filePath);

        File file = File.createTempFile("staging-wiki", ".xml");

        // Put XML Export file in temp folder
        copyToDisk(zis, file);

        // Extract wiki owner
        String wikiOwner = extractWikiOwnerFromPath(filePath);

        if (filePath.endsWith(SpaceMetadataExportTask.FILENAME)) {
          templfilesByJCRPath.put(wikiOwner + SpaceMetadataExportTask.FILENAME, file);
          continue;
        }

        if (filePath.endsWith(WikiActivitiesExportTask.FILENAME)) {
          templfilesByJCRPath.put(wikiOwner + WikiActivitiesExportTask.FILENAME, file);
          continue;
        }

        // Add nodePath by WikiOwner
        if (!contentsByOwner.containsKey(wikiOwner)) {
          contentsByOwner.put(wikiOwner, new ArrayList<String>());
        }
        String nodePath = filePath.substring(filePath.indexOf("---/") + 4, filePath.lastIndexOf(".xml"));
        if (!nodePath.startsWith("/")) {
          nodePath = "/" + nodePath;
        }
        contentsByOwner.get(wikiOwner).add(nodePath);
        templfilesByJCRPath.put(nodePath, file);
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
  private String extractWikiOwnerFromPath(String path) {
    int beginIndex = ("wiki/" + wikiType + "/___").length();
    int endIndex = path.indexOf("---/", beginIndex);
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

  private static void copyToDisk(InputStream input, File file) throws Exception {
    FileOutputStream fileOuput = null;
    try {
      fileOuput = new FileOutputStream(file);
      IOUtils.copy(input, fileOuput);
    } finally {
      if (fileOuput != null) {
        fileOuput.close();
      }
    }
  }

  @SuppressWarnings("unchecked")
  private void createActivities(File activitiesFile, String spacePrettyName) {
    log.info("Importing Wiki activities");

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

    ExoSocialActivity pageActivity = null;
    for (ExoSocialActivity activity : activitiesList) {
      try {
        activity.setId(null);
        String pageId = activity.getTemplateParams().get("page_id");
        String pageOwner = activity.getTemplateParams().get("page_owner");
        String pageType = activity.getTemplateParams().get("page_type");
        if (pageId == null) {
          if (activity.isComment()) {
            if (pageActivity == null) {
              log.warn("Attempt to add a comment activity to a non existing page activity.");
            } else {
              saveComment(pageActivity, activity);
            }
          } else {
            log.warn("An unknown activity for Wiki was found. Ignore it: " + activity.getTitle());
            pageActivity = null;
            continue;
          }
        } else {
          Page page = wikiService.getPageById(pageType, pageOwner, pageId);
          if (page == null) {
            log.warn("Wiki page not found. Cannot import activity '" + activity.getTitle() + "'.");
            pageActivity = null;
            continue;
          }
          if (activity.isComment()) {
            saveComment(pageActivity, activity);
          } else {
            pageActivity = null;
            saveActivity(activity, spacePrettyName);
            if (activity.getId() == null) {
              log.warn("Activity '" + activity.getTitle() + "' is not imported, id is null");
              continue;
            }
            ActivityTypeUtils.attachActivityId(page.getJCRPageNode(), activity.getId());
            page.getJCRPageNode().getSession().save();
            pageActivity = activity;
          }
        }
      } catch (Exception e) {
        log.warn("Error while adding activity: " + activity.getTitle(), e);
      }
    }
  }
  private List<ExoSocialActivity> sanitizeContent(List<ExoSocialActivity> activities) {
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
    return activitiesList;
  }

  private void deleteActivities(String wikiType, String wikiOwner) throws Exception {
    // Delete Forum activity stream
    Wiki wiki = wikiService.getWiki(wikiType, wikiOwner);
    if (wiki == null) {
      return;
    }
    PageImpl homePage = (PageImpl) wiki.getWikiHome();
    List<Page> pages = new ArrayList<Page>();
    pages.add(homePage);
    computeChildPages(pages, homePage);

    for (Page page : pages) {
      String activityId = ActivityTypeUtils.getActivityId(page.getJCRPageNode());
      ExoSocialActivity activity = activityManager.getActivity(activityId);
      if (activity != null) {
        activityManager.deleteActivity(activity);
      }
    }
  }

  private void computeChildPages(List<Page> pages, PageImpl homePage) throws Exception {
    Collection<PageImpl> chilPages = homePage.getChildPages().values();
    for (PageImpl childPageImpl : chilPages) {
      pages.add(childPageImpl);
      computeChildPages(pages, childPageImpl);
    }
  }

}
