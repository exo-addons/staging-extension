package org.exoplatform.management.content.operations.site.contents;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.jcr.ImportUUIDBehavior;
import javax.jcr.Node;
import javax.jcr.Session;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.exoplatform.commons.utils.ActivityTypeUtils;
import org.exoplatform.management.common.AbstractJCROperationHandler;
import org.exoplatform.management.content.operations.site.SiteUtil;
import org.exoplatform.management.content.operations.site.seo.SiteSEOExportTask;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.seo.PageMetadataModel;
import org.exoplatform.services.seo.SEOService;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.manager.ActivityManager;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.social.core.storage.api.ActivityStorage;
import org.exoplatform.social.core.storage.api.IdentityStorage;
import org.gatein.common.logging.Logger;
import org.gatein.common.logging.LoggerFactory;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.operation.OperationAttachment;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.NoResultModel;

import com.thoughtworks.xstream.XStream;

/**
 * @author <a href="mailto:soren.schmidt@exoplatform.com">Soren Schmidt</a>
 * @author <a href="mailto:thomas.delhomenie@exoplatform.com">Thomas
 *         Delhoménie</a>
 * @author <a href="mailto:boubaker.khanfir@exoplatform.com">Boubaker
 *         Khanfir</a>
 * @version $Revision$ usage: ssh -p 2000 john@localhost mgmt connect ls cd
 *          content import -f /acmeTest.zip
 */
public class SiteContentsImportResource extends AbstractJCROperationHandler {

  final private static Logger log = LoggerFactory.getLogger(SiteContentsImportResource.class);
  final private static int BUFFER = 2048000;

  private static SEOService seoService = null;

  private String importedSiteName = null;
  private String filePath = null;

  private Pattern contenLinkPattern = Pattern.compile("([a-zA-Z]*)/([a-zA-Z]*)/(.*)");

  public SiteContentsImportResource() {}

  public SiteContentsImportResource(String siteName, String filePath) {
    this.importedSiteName = siteName;
    this.filePath = filePath;
  }

  @Override
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws OperationException {
    spaceService = operationContext.getRuntimeContext().getRuntimeComponent(SpaceService.class);
    activityManager = operationContext.getRuntimeContext().getRuntimeComponent(ActivityManager.class);
    activityStorage = operationContext.getRuntimeContext().getRuntimeComponent(ActivityStorage.class);
    identityStorage = operationContext.getRuntimeContext().getRuntimeComponent(IdentityStorage.class);
    repositoryService = operationContext.getRuntimeContext().getRuntimeComponent(RepositoryService.class);

    if (importedSiteName == null) {
      importedSiteName = operationContext.getAddress().resolvePathTemplate("site-name");
    }
    if (seoService == null) {
      seoService = operationContext.getRuntimeContext().getRuntimeComponent(SEOService.class);
    }

    // "uuidBehavior" attribute
    int uuidBehaviorValue = extractUuidBehavior(operationContext.getAttributes().getValue("uuidBehavior"));
    List<String> filters = operationContext.getAttributes().getValues("filter");

    boolean isCleanPublication = filters.contains("cleanPublication:true");

    InputStream attachmentInputStream = null;
    if (filePath != null) {
      try {
        // attachmentInputStream closed in finally block
        attachmentInputStream = new FileInputStream(filePath);
      } catch (FileNotFoundException exception) {
        throw new OperationException(OperationNames.IMPORT_RESOURCE, "File not found to import.");
      }
    } else {
      OperationAttachment attachment = operationContext.getAttachment(false);
      if (attachment == null) {
        throw new OperationException(OperationNames.IMPORT_RESOURCE, "No attachment available for Site Content import.");
      }

      attachmentInputStream = attachment.getStream();
      if (attachmentInputStream == null) {
        throw new OperationException(OperationNames.IMPORT_RESOURCE, "No data stream available for Site Content import.");
      }
    }

    Map<String, SiteData> sitesData = new HashMap<String, SiteData>();
    String tempParentFolderPath = null;
    try {
      // extract data from zip
      tempParentFolderPath = extractDataFromZip(attachmentInputStream, importedSiteName, sitesData);

      // import data of each site
      for (String site : sitesData.keySet()) {
        SiteData siteData = sitesData.get(site);
        if (siteData.getNodeExportFiles() == null || siteData.getNodeExportFiles().isEmpty()) {
          log.info("No data available to import for site: " + site);
          continue;
        }

        Map<String, String> metaDataOptions = siteData.getSiteMetadata().getOptions();
        String workspace = metaDataOptions.get("site-workspace");
        log.info("Reading metadata options for import: workspace: " + workspace);

        try {
          importContentNodes(operationContext, siteData.getSiteMetadata(), siteData.getNodeExportFiles(), siteData.getNodeExportHistoryFiles(), workspace, uuidBehaviorValue, isCleanPublication);
          log.info("Content import has been finished");
        } catch (Exception e) {
          log.error("Unable to create import task", e);
        }

        String activitiesFilePath = tempParentFolderPath + "/" + replaceSpecialChars(SiteContentsActivitiesExportTask.getEntryPath(site));
        File activitiesFile = new File(activitiesFilePath);
        // Import activities
        if (activitiesFile.exists() && activityManager != null) {
          createActivities(activitiesFile);
        }
      }

    } finally {
      if (sitesData != null && !sitesData.isEmpty()) {
        // import data of each site
        for (String site : sitesData.keySet()) {
          SiteData siteData = sitesData.get(site);
          if (siteData.getNodeExportHistoryFiles() != null && !siteData.getNodeExportHistoryFiles().isEmpty()) {
            for (String tempAbsPath : siteData.getNodeExportHistoryFiles().values()) {
              File file = new File(tempAbsPath);
              if (file.exists() && !file.isDirectory()) {
                try {
                  file.delete();
                } catch (Exception e) {
                  // Nothing to do, deleteOnExit is called
                }
              }
            }
          }
        }
      }
      if (attachmentInputStream != null) {
        try {
          attachmentInputStream.close();
        } catch (IOException e) {
          // Nothing to do
        }
      }
      if (tempParentFolderPath != null) {
        File tempFolderFile = new File(tempParentFolderPath);
        if (tempFolderFile.exists()) {
          try {
            FileUtils.deleteDirectory(tempFolderFile);
          } catch (IOException e) {
            log.warn("Unable to delete temp folder: " + tempParentFolderPath + ". Not blocker.");
          }
        }
      }
    }
    resultHandler.completed(NoResultModel.INSTANCE);
  }

  /**
   * Extract data from zip
   * 
   * @param sitesData
   * 
   * @param attachment
   * @return
   */
  public static String extractDataFromZip(InputStream attachmentInputStream, String importedSiteName, Map<String, SiteData> sitesData) {

    String tempParentFolderPath = null;
    NonCloseableZipInputStream zis = null;
    File tmpZipFile = null;
    try {
      // Store attachement in local File
      tmpZipFile = File.createTempFile("staging-content", ".zip");
      tmpZipFile.deleteOnExit();

      FileOutputStream tmpFileOutputStream = new FileOutputStream(tmpZipFile);
      IOUtils.copy(attachmentInputStream, tmpFileOutputStream);
      tmpFileOutputStream.close();
      attachmentInputStream.close();

      String targetFolderPath = tmpZipFile.getAbsolutePath().replaceAll("\\.zip$", "") + "/";

      zis = new NonCloseableZipInputStream(new FileInputStream(tmpZipFile));

      ZipEntry entry;
      while ((entry = zis.getNextEntry()) != null) {
        // Skip directories
        if (entry.isDirectory()) {
          createFile(new File(targetFolderPath + replaceSpecialChars(entry.getName())), true);
          continue;
        }
        String filePath = entry.getName();
        // Skip empty entries (this allows empty zip files to not cause
        // exceptions).
        if (filePath.equals("") || !filePath.startsWith(SiteUtil.getSitesBasePath() + "/")) {
          continue;
        }

        String siteName = extractSiteNameFromPath(filePath);

        // if we are in a site (for example /content/sites/acme), take only
        // the files relative to this site
        if (importedSiteName != null && !importedSiteName.equals(siteName)) {
          continue;
        }

        log.info("Processing the node " + filePath);

        // metadata file ?
        if (filePath.endsWith(SiteMetaDataExportTask.FILENAME)) {
          // Unmarshall metadata xml file
          XStream xstream = new XStream();
          xstream.alias("metadata", SiteMetaData.class);
          InputStreamReader isr = new InputStreamReader(zis, "UTF-8");
          SiteMetaData siteMetadata = (SiteMetaData) xstream.fromXML(isr);

          // Save unmarshalled metadata
          SiteData siteData = sitesData.get(siteName);
          if (siteData == null) {
            siteData = new SiteData();
          }
          siteData.setSiteMetadata(siteMetadata);
          sitesData.put(siteName, siteData);
        }
        // seo file ?
        else if (filePath.endsWith(SiteSEOExportTask.FILENAME)) {
          String lang = filePath.substring(filePath.lastIndexOf("/") + 1, filePath.indexOf(SiteSEOExportTask.FILENAME));
          XStream xStream = new XStream();
          xStream.alias("seo", List.class);
          InputStreamReader isr = new InputStreamReader(zis, "UTF-8");

          @SuppressWarnings("unchecked")
          List<PageMetadataModel> models = (List<PageMetadataModel>) xStream.fromXML(isr);
          if (models != null && !models.isEmpty()) {
            for (PageMetadataModel pageMetadataModel : models) {
              seoService.storeMetadata(pageMetadataModel, siteName, false, lang);
            }
          }
        }
        // sysview file ?
        else if (filePath.endsWith(".xml")) {
          // Put XML Export file in temp folder
          if (!copyToDisk(zis, targetFolderPath + replaceSpecialChars(filePath))) {
            continue;
          }

          // Save unmarshalled sysview
          SiteData siteData = sitesData.get(siteName);
          if (siteData == null) {
            siteData = new SiteData();
          }
          siteData.getNodeExportFiles().put(filePath, targetFolderPath + replaceSpecialChars(filePath));
          sitesData.put(siteName, siteData);
        } else if (filePath.endsWith(SiteContentsVersionHistoryExportTask.VERSION_HISTORY_FILE_SUFFIX)) {
          // Put Version History file in temp folder
          if (!copyToDisk(zis, targetFolderPath + replaceSpecialChars(filePath))) {
            continue;
          }

          SiteData siteData = sitesData.get(siteName);
          if (siteData == null) {
            siteData = new SiteData();
          }
          siteData.getNodeExportHistoryFiles().put(filePath.replace(SiteContentsVersionHistoryExportTask.VERSION_HISTORY_FILE_SUFFIX, ".xml"), targetFolderPath + replaceSpecialChars(filePath));
        } else if (filePath.endsWith(SiteContentsActivitiesExportTask.FILENAME)) {
          // Put activities file in temp folder
          copyToDisk(zis, targetFolderPath + replaceSpecialChars(filePath));
        }
      }
    } catch (Exception e) {
      throw new OperationException(OperationNames.IMPORT_RESOURCE, "Exception when reading the underlying data stream from import.", e);
    } finally {
      if (zis != null) {
        try {
          zis.reallyClose();
        } catch (IOException e) {
          log.warn("Can't close inputStream of attachement.");
        }
      }
      if (tmpZipFile != null) {
        tempParentFolderPath = tmpZipFile.getAbsolutePath().replaceAll("\\.zip$", "");
        try {
          FileUtils.forceDelete(tmpZipFile);
        } catch (IOException e) {
          log.warn("Unable to delete temp file: " + tmpZipFile.getAbsolutePath() + ". Not blocker.");
          tmpZipFile.deleteOnExit();
        }
      }
    }
    return tempParentFolderPath;
  }

  /**
   * Import data of a site
   * 
   * @param operationContext
   * @param metaData
   * @param nodes
   * @param historyFiles
   * @param workspace
   * @param uuidBehaviorValue
   * @param isCleanPublication
   * @throws Exception
   */
  private void importContentNodes(OperationContext operationContext, SiteMetaData metaData, Map<String, String> nodes, Map<String, String> historyFiles, String workspace, int uuidBehaviorValue,
      boolean isCleanPublication) throws Exception {

    List<Map.Entry<String, String>> orderedEntries = new ArrayList<Map.Entry<String, String>>(nodes.entrySet());
    Collections.sort(orderedEntries, new Comparator<Map.Entry<String, String>>() {
      @Override
      public int compare(Entry<String, String> o1, Entry<String, String> o2) {
        if (o1.getKey().contains("/exo:actions") && !o2.getKey().contains("/exo:actions")) {
          return 1;
        }
        if (o2.getKey().contains("/exo:actions") && !o1.getKey().contains("/exo:actions")) {
          return -1;
        }
        return o1.getKey().compareTo(o2.getKey());
      }
    });
    for (Map.Entry<String, String> entry : orderedEntries) {
      String name = entry.getKey();
      String path = metaData.getExportedFiles().get(name);

      String tempFilePath = nodes.get(name);

      if (StringUtils.isEmpty(tempFilePath)) {
        log.warn("can't get temporary file for content: " + name + ". Ignore import operation for this file.");
        continue;
      }

      String targetParentNodeJCRPath = path + name.substring(name.lastIndexOf("/"), name.lastIndexOf('.'));
      if (targetParentNodeJCRPath.contains("//")) {
        targetParentNodeJCRPath = targetParentNodeJCRPath.replaceAll("//", "/");
      }

      List<String> proceededPaths = new ArrayList<String>();
      Session session = getSession(workspace);
      try {
        if (!proceededPaths.contains(targetParentNodeJCRPath) && session.itemExists(targetParentNodeJCRPath) && session.getItem(targetParentNodeJCRPath) instanceof Node) {
          log.info("Deleting the node " + workspace + ":" + targetParentNodeJCRPath);

          Node oldNode = (Node) session.getItem(targetParentNodeJCRPath);
          if (oldNode.isNodeType("exo:activityInfo") && activityManager != null) {
            String activityId = ActivityTypeUtils.getActivityId(oldNode);
            deleteActivity(activityId);
          }
          oldNode.remove();
          session.save();
          session.refresh(false);
        }
      } catch (Exception e) {
        log.error("Error when trying to find and delete the node: " + targetParentNodeJCRPath, e);
        continue;
      } finally {
        if (session != null) {
          session.logout();
        }
        session = getSession(workspace);
      }

      if (log.isInfoEnabled()) {
        log.info("Importing the node " + name + " to the node " + path);
      }

      // Create the parent path
      Node currentNode = createJCRPath(session, path);
      FileInputStream fis = null, historyFis1 = null, historyFis2 = null;
      File xmlFile = new File(tempFilePath);
      File historyFile = historyFiles.containsKey(name) ? new File(historyFiles.get(name)) : null;

      try {
        fis = new FileInputStream(tempFilePath);
        session.refresh(false);
        session.importXML(path, fis, uuidBehaviorValue);
        session.save();

        if (isCleanPublication) {
          // Clean publication information
          cleanPublication(targetParentNodeJCRPath, session);
        } else if (historyFiles.containsKey(name)) {
          log.info("Importing history of the node " + path);
          String historyFilePath = historyFiles.get(name);

          historyFis1 = new FileInputStream(historyFilePath);
          Map<String, String> mapHistoryValue = org.exoplatform.services.cms.impl.Utils.getMapImportHistory(historyFis1);

          historyFis2 = new FileInputStream(historyFilePath);
          org.exoplatform.services.cms.impl.Utils.processImportHistory(currentNode, historyFis2, mapHistoryValue);
        }
      } catch (Exception e) {
        log.error("Error when trying to import node: " + targetParentNodeJCRPath + ", revert changes", e);
        // Revert changes
        session.refresh(false);
      } finally {
        if (fis != null) {
          fis.close();
        }
        if (historyFis1 != null) {
          historyFis1.close();
        }
        if (historyFis2 != null) {
          historyFis2.close();
        }
        if (session != null) {
          session.logout();
        }
        if (xmlFile != null) {
          xmlFile.delete();
        }
        if (historyFile != null) {
          historyFile.delete();
        }
      }
    }
  }

  /**
   * JCR Import UUID Behavior enum
   * 
   * @author <a href="mailto:thomas.delhomenie@exoplatform.com">Thomas
   *         Delhoménie</a>
   */
  private enum ImportBehavior
  {
    THROW(ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW), //
    REMOVE(ImportUUIDBehavior.IMPORT_UUID_COLLISION_REMOVE_EXISTING), //
    REPLACE(ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING), //
    NEW(ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);

    final private int behavior;

    ImportBehavior(int behavior) {
      this.behavior = behavior;
    }

    public int getBehavior() {
      return this.behavior;
    }
  }

  /**
   * Convert UUID behavior string into int
   * 
   * @param uuidBehavior
   * @return
   */
  private int extractUuidBehavior(String uuidBehavior) {

    int uuidBehaviorValue;
    if (!StringUtils.isEmpty(uuidBehavior)) {
      try {
        uuidBehaviorValue = ImportBehavior.valueOf(uuidBehavior).getBehavior();
      } catch (Exception e) {
        throw new OperationException(OperationNames.IMPORT_RESOURCE, "Unknown uuidBehavior " + uuidBehavior);
      }
    } else {
      uuidBehaviorValue = ImportBehavior.NEW.getBehavior();
    }

    return uuidBehaviorValue;
  }

  /**
   * Extract site name from the file path
   * 
   * @param path
   *          The path of the file
   * @return The site name
   */
  private static String extractSiteNameFromPath(String path) {
    String siteName = null;

    int beginIndex = SiteUtil.getSitesBasePath().length() + 1;
    siteName = path.substring(beginIndex, path.indexOf("/", beginIndex));

    return siteName;
  }

  private static boolean copyToDisk(InputStream input, String output) throws Exception {
    byte data[] = new byte[BUFFER];
    BufferedOutputStream dest = null;
    try {
      FileOutputStream fileOuput = new FileOutputStream(createFile(new File(output), false));
      dest = new BufferedOutputStream(fileOuput, BUFFER);
      int count = 0;
      while ((count = input.read(data, 0, BUFFER)) != -1)
        dest.write(data, 0, count);
      return true;
    } catch (Exception e) {
      log.error("Error while copying file: " + output, e);
      return false;
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
    if (file.exists()) {
      return file;
    }
    if (file.isDirectory() || folder) {
      file.mkdir();
    } else {
      file.createNewFile();
    }
    return file;
  }

  @SuppressWarnings("unchecked")
  private void createActivities(File activitiesFile) {
    log.info("Importing Documents activities");

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

    ExoSocialActivity documentActivity = null;
    for (ExoSocialActivity activity : activitiesList) {
      try {
        activity.setId(null);

        String contentLink = activity.getTemplateParams().get("contenLink");
        String workspace = null;
        String contentPath = null;
        if (contentLink != null && !contentLink.isEmpty()) {
          Matcher matcher = contenLinkPattern.matcher(contentLink);
          if (matcher.matches() && matcher.groupCount() == 3) {
            workspace = matcher.group(2);
            contentPath = "/" + matcher.group(3);
          } else {
            log.warn("ContentLink param was found in activity params, but it doesn't refer to a correct path");
            documentActivity = null;
            continue;
          }
        }
        if (contentLink == null) {
          if (activity.isComment()) {
            if (documentActivity == null) {
              log.warn("Attempt to add a comment activity to a non existing document activity.");
            } else {
              saveComment(documentActivity, activity);
            }
          } else {
            log.warn("An unknown Document activity for was found. Ignore it: " + activity.getTitle());
            documentActivity = null;
            continue;
          }
        } else {
          Session session = getSession(workspace);
          if (!session.itemExists(contentPath)) {
            log.warn("Document '" + contentPath + "' not found. Cannot import activity '" + activity.getTitle() + "'.");
            documentActivity = null;
            continue;
          }
          if (activity.isComment()) {
            if (documentActivity == null) {
              log.warn("Attempt to add a comment activity to a non existing document activity.");
            } else {
              saveComment(documentActivity, activity);
            }
          } else {
            documentActivity = null;
            saveActivity(activity);
            if (activity.getId() == null) {
              log.warn("Activity '" + activity.getTitle() + "' is not imported, id is null");
              continue;
            }
            ActivityTypeUtils.attachActivityId(((Node) session.getItem(contentPath)), activity.getId());
            session.save();
            documentActivity = activity;
          }
        }
      } catch (Exception e) {
        log.warn("Error while adding activity: " + activity.getTitle(), e);
      }
    }
  }
}
