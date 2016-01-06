package org.exoplatform.management.content.operations.site.contents;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.jcr.Node;
import javax.jcr.Session;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.commons.utils.ActivityTypeUtils;
import org.exoplatform.management.common.FileEntry;
import org.exoplatform.management.common.exportop.ActivitiesExportTask;
import org.exoplatform.management.common.exportop.JCRNodeExportTask;
import org.exoplatform.management.common.importop.AbstractJCRImportOperationHandler;
import org.exoplatform.management.common.importop.ActivityImportOperationInterface;
import org.exoplatform.management.common.importop.FileImportOperationInterface;
import org.exoplatform.management.content.operations.site.SiteUtil;
import org.exoplatform.management.content.operations.site.seo.SiteSEOExportTask;
import org.exoplatform.services.ecm.publication.PublicationService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.seo.PageMetadataModel;
import org.exoplatform.services.seo.SEOService;
import org.exoplatform.services.wcm.publication.WCMPublicationService;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.manager.ActivityManager;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.social.core.storage.api.ActivityStorage;
import org.exoplatform.social.core.storage.api.IdentityStorage;
import org.gatein.common.logging.Logger;
import org.gatein.common.logging.LoggerFactory;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.NoResultModel;

/**
 * @author <a href="mailto:soren.schmidt@exoplatform.com">Soren Schmidt</a>
 * @author <a href="mailto:thomas.delhomenie@exoplatform.com">Thomas
 *         Delhoménie</a>
 * @author <a href="mailto:boubaker.khanfir@exoplatform.com">Boubaker
 *         Khanfir</a>
 * @version $Revision$ usage: ssh -p 2000 john@localhost mgmt connect ls cd
 *          content import -f /acmeTest.zip
 */
public class SiteContentsImportResource extends AbstractJCRImportOperationHandler implements ActivityImportOperationInterface, FileImportOperationInterface {

  private final static Logger log = LoggerFactory.getLogger(SiteContentsImportResource.class);

  private final static Pattern CONTENT_LINK_PATTERN = Pattern.compile("([a-zA-Z]*)/([a-zA-Z]*)/(.*)");

  private static SEOService seoService = null;

  private String importedSiteName = null;
  private String filePath = null;

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
    seoService = operationContext.getRuntimeContext().getRuntimeComponent(SEOService.class);
    publicationService = operationContext.getRuntimeContext().getRuntimeComponent(PublicationService.class);
    wcmPublicationService = operationContext.getRuntimeContext().getRuntimeComponent(WCMPublicationService.class);

    if (importedSiteName == null) {
      importedSiteName = operationContext.getAddress().resolvePathTemplate("site-name");
    }

    List<String> filters = operationContext.getAttributes().getValues("filter");
    boolean isCleanPublication = filters.contains("cleanPublication:true");

    boolean errors = false;
    InputStream attachmentInputStream = null;

    increaseCurrentTransactionTimeOut(operationContext);
    try {
      if (filePath != null) {
        attachmentInputStream = new FileInputStream(filePath);
      } else {
        attachmentInputStream = getAttachementInputStream(operationContext);
      }

      // extract data from zip
      Map<String, List<FileEntry>> contentsByOwner = extractDataFromZip(attachmentInputStream);

      for (String site : contentsByOwner.keySet()) {
        List<FileEntry> fileEntries = contentsByOwner.get(site);

        FileEntry activitiesFile = getAndRemoveFileByPath(fileEntries, ActivitiesExportTask.FILENAME);
        List<FileEntry> seoFiles = getAndRemoveFilesStartsWith(fileEntries, SiteSEOExportTask.FILENAME);

        SiteMetaData siteMetaData = getSiteMetadata(fileEntries);

        Map<String, String> metaDataOptions = siteMetaData.getOptions();

        String workspace = metaDataOptions.get("site-workspace");
        log.info("Reading metadata options for import: workspace: " + workspace);

        try {
          if (fileEntries != null) {
            for (FileEntry fileEntry : fileEntries) {
              errors |= !importNode(fileEntry, workspace, isCleanPublication);
            }
          }
          log.info("Content import has been finished");
        } catch (Exception e) {
          log.error("Error while importing site content: " + site, e);
        }

        // Delete
        String removeNodes = metaDataOptions.get("removeNodes");
        if (!StringUtils.isEmpty(removeNodes)) {
          String[] removeNodePaths = removeNodes.split(";");
          removeNodes(workspace, removeNodePaths);
        }

        // Import activities
        if (activitiesFile != null && activitiesFile.getFile().exists() && activityManager != null) {
          log.info("Importing Site Content activities");
          importActivities(activitiesFile.getFile(), null, true);
        }

        if (seoFiles != null && !seoFiles.isEmpty()) {
          for (FileEntry fileEntry : seoFiles) {
            String lang = fileEntry.getNodePath().replace(SiteSEOExportTask.FILENAME, "");

            File seoFile = fileEntry.getFile();
            @SuppressWarnings("unchecked")
            List<PageMetadataModel> models = deserializeObject(seoFile, List.class, "seo");
            if (models != null && !models.isEmpty()) {
              for (PageMetadataModel pageMetadataModel : models) {
                seoService.storeMetadata(pageMetadataModel, site, false, lang);
              }
            }
          }
        }
      }
    } catch (Exception e) {
      throw new OperationException(OperationNames.IMPORT_RESOURCE, "Unable to import Site contents.", e);
    } finally {
      restoreDefaultTransactionTimeOut(repositoryService);
      if (attachmentInputStream != null) {
        try {
          attachmentInputStream.close();
        } catch (IOException e) {
          // Nothing to do
        }
      }
    }
    if (errors) {
      throw new OperationException(OperationNames.IMPORT_RESOURCE, "Some errors occured while importing contents.");
    } else {
      resultHandler.completed(NoResultModel.INSTANCE);
    }
  }

  private void removeNodes(String workspace, String[] removeNodePaths) throws Exception {
    Session session = getSession(workspace);

    for (String nodeToRemovePath : removeNodePaths) {
      if (StringUtils.isEmpty(nodeToRemovePath) || !nodeToRemovePath.startsWith("/")) {
        log.warn("Ignore node to delete: '" + nodeToRemovePath + "'");
        continue;
      }

      if (session.itemExists(nodeToRemovePath)) {
        log.info("Remove node  '" + nodeToRemovePath + "'");
        session.getItem(nodeToRemovePath).remove();
        session.save();
        session.refresh(false);
      } else {
        log.warn("Cannot remove node because it doesn't exist '" + nodeToRemovePath + "'");
      }
    }
  }

  public static SiteMetaData getSiteMetadata(List<FileEntry> fileEntries) throws Exception {
    FileEntry metadataFile = getAndRemoveFileByPath(fileEntries, SiteMetaDataExportTask.FILENAME);
    if (metadataFile == null) {
      throw new IllegalStateException("Cannot retrieve site metadata file for site");
    }

    SiteMetaData siteMetaData = deserializeObject(metadataFile.getFile(), SiteMetaData.class, "metadata");
    if (siteMetaData == null) {
      throw new IllegalStateException("Cannot retrieve site metadata for site");
    }
    return siteMetaData;
  }

  public String getManagedFilesPrefix() {
    return "content/sites/";
  }

  public boolean isUnKnownFileFormat(String filePath) {
    return !filePath.endsWith(".xml") && !filePath.endsWith(SiteMetaDataExportTask.FILENAME) && !filePath.endsWith(SiteSEOExportTask.FILENAME)
        && !filePath.endsWith(SiteContentsVersionHistoryExportTask.VERSION_HISTORY_FILE_SUFFIX) && !filePath.endsWith(ActivitiesExportTask.FILENAME);
  }

  public boolean addSpecialFile(List<FileEntry> fileEntries, String filePath, File file) {
    if (filePath.endsWith(SiteMetaDataExportTask.FILENAME)) {
      fileEntries.add(new FileEntry(SiteMetaDataExportTask.FILENAME, file));
      return true;
    } else if (filePath.endsWith(SiteSEOExportTask.FILENAME)) {
      String lang = filePath.substring(filePath.lastIndexOf("/") + 1, filePath.indexOf(SiteSEOExportTask.FILENAME));
      fileEntries.add(new FileEntry(SiteSEOExportTask.FILENAME + lang, file));
      return true;
    } else if (filePath.endsWith(ActivitiesExportTask.FILENAME)) {
      fileEntries.add(new FileEntry(ActivitiesExportTask.FILENAME, file));
      return true;
    } else if (filePath.endsWith(SiteContentsVersionHistoryExportTask.VERSION_HISTORY_FILE_SUFFIX)) {
      String path = filePath.replace(SiteContentsVersionHistoryExportTask.VERSION_HISTORY_FILE_SUFFIX, "");
      String[] fileParts = path.split(JCRNodeExportTask.JCR_DATA_SEPARATOR);
      if (fileParts.length != 2) {
        log.warn("Cannot parse Version History file: " + filePath);
        return true;
      }
      FileEntry fileEntry = getAndRemoveFileByPath(fileEntries, fileParts[1]);
      if (fileEntry == null) {
        log.error("Cannot parse file '" + filePath + "', no XML file found for this Version History file for node path: " + fileParts[1]);
        return true;
      }
      fileEntry.setHistoryFile(file);
      fileEntries.add(fileEntry);

      return true;
    }
    return false;
  }

  public String extractIdFromPath(String path) {
    int beginIndex = SiteUtil.getSitesBasePath().length() + 1;
    return path.substring(beginIndex, path.indexOf("/", beginIndex));
  }

  public void attachActivityToEntity(ExoSocialActivity activity, ExoSocialActivity comment) throws Exception {
    if (comment != null) {
      return;
    }
    String contentLink = activity.getTemplateParams().get("contenLink");
    Matcher matcher = CONTENT_LINK_PATTERN.matcher(contentLink);

    if (matcher.matches()) {
      String workspace = matcher.group(2);
      String contentPath = "/" + matcher.group(3);

      Session session = null;
      try {
        session = getSession(workspace);
        ActivityTypeUtils.attachActivityId(((Node) session.getItem(contentPath)), activity.getId());
        session.save();
      } finally {
        if (session != null) {
          session.logout();
        }
      }
    } else {
      log.warn("Cannot attach activity for content activity with contentLink = " + contentLink);
    }
  }

  public boolean isActivityNotValid(ExoSocialActivity activity, ExoSocialActivity comment) throws Exception {
    if (comment == null) {
      String contentLink = activity.getTemplateParams().get("contenLink");
      String workspace = null;
      String contentPath = null;
      if (contentLink != null && !contentLink.isEmpty()) {
        Matcher matcher = CONTENT_LINK_PATTERN.matcher(contentLink);
        if (matcher.matches() && matcher.groupCount() == 3) {
          workspace = matcher.group(2);
          contentPath = "/" + matcher.group(3);
        }
        if (workspace == null || workspace.isEmpty()) {
          log.warn("ContentLink param was found in activity params, but it doesn't refer to a correct path: " + contentLink);
          return true;
        }
        Session session = null;
        try {
          session = getSession(workspace);
          if (!session.itemExists(contentPath)) {
            log.warn("Document '" + contentPath + "' not found. Cannot import activity '" + activity.getTitle() + "'.");
            return true;
          } else {
            // delete old activity
            Node node = (Node) session.getItem(contentPath);
            if (node.isNodeType("exo:activityInfo") && activityManager != null) {
              String activityId = ActivityTypeUtils.getActivityId(node);
              deleteActivity(activityId);
            }
          }
        } finally {
          if (session != null) {
            session.logout();
          }
        }
      }
    }
    return false;
  }
}
