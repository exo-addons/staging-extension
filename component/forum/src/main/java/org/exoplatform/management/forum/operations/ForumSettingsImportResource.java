package org.exoplatform.management.forum.operations;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.jcr.ImportUUIDBehavior;
import javax.jcr.Node;
import javax.jcr.Session;

import org.apache.commons.io.FileUtils;
import org.exoplatform.forum.common.jcr.KSDataLocation;
import org.exoplatform.forum.service.ForumService;
import org.exoplatform.forum.service.Utils;
import org.exoplatform.management.common.AbstractJCROperationHandler;
import org.exoplatform.services.jcr.RepositoryService;
import org.gatein.common.logging.Logger;
import org.gatein.common.logging.LoggerFactory;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.operation.OperationAttachment;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.NoResultModel;

public class ForumSettingsImportResource extends AbstractJCROperationHandler {

  final private static Logger log = LoggerFactory.getLogger(ForumSettingsImportResource.class);

  private KSDataLocation dataLocation;
  private ForumService forumService;

  @Override
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws OperationException {
    repositoryService = operationContext.getRuntimeContext().getRuntimeComponent(RepositoryService.class);
    dataLocation = operationContext.getRuntimeContext().getRuntimeComponent(KSDataLocation.class);
    forumService = operationContext.getRuntimeContext().getRuntimeComponent(ForumService.class);

    File tmpZipFile = null;
    Map<String, File> filesToImport = null;
    try {
      // Copy attachement to local temporary File
      tmpZipFile = File.createTempFile("staging-forum-settings", ".zip");
      filesToImport = extractDataFromZip(operationContext.getAttachment(false), tmpZipFile);
      Set<String> keys = filesToImport.keySet();

      String workspace = dataLocation.getWorkspace();
      for (String locationPath : keys) {
        if (locationPath.endsWith(ForumSettingsExportResource.SYSTEM_ADMINISTRATION)) {
          log.info("Importing forum system administration settings...");
          String targetPath = "/" + dataLocation.getAdministrationLocation();
          File fileToImport = filesToImport.get(locationPath);
          importSettingsContent(fileToImport, workspace, locationPath, targetPath);
        } else if (locationPath.endsWith(ForumSettingsExportResource.BANNED_IP)) {
          log.info("Importing forum ban IP settings...");
          String targetPath = "/" + dataLocation.getBanIPLocation();
          File fileToImport = filesToImport.get(locationPath);
          importSettingsContent(fileToImport, workspace, locationPath, targetPath);
        } else if (locationPath.endsWith(ForumSettingsExportResource.BB_CODES)) {
          log.info("Importing forum bb codes settings...");
          String targetPath = "/" + dataLocation.getBBCodesLocation();
          File fileToImport = filesToImport.get(locationPath);
          importSettingsContent(fileToImport, workspace, locationPath, targetPath);
        } else if (locationPath.endsWith(ForumSettingsExportResource.USER_PROFLES)) {
          log.info("Importing forum user profiles settings...");
          String targetPath = "/" + dataLocation.getUserProfilesLocation();
          File fileToImport = filesToImport.get(locationPath);
          importSettingsContent(fileToImport, workspace, locationPath, targetPath);
        } else if (locationPath.endsWith(ForumSettingsExportResource.USER_AVATARS)) {
          log.info("Importing forum user avatars settings...");
          String targetPath = "/" + dataLocation.getAvatarsLocation();
          File fileToImport = filesToImport.get(locationPath);
          importSettingsContent(fileToImport, workspace, locationPath, targetPath);
        } else if (locationPath.endsWith(ForumSettingsExportResource.TAGS)) {
          log.info("Importing forum tag settings...");
          String targetPath = "/" + dataLocation.getTagsLocation();
          File fileToImport = filesToImport.get(locationPath);
          importSettingsContent(fileToImport, workspace, locationPath, targetPath);
        }
      }
      forumService.calculateDeletedUser("fakeUser" + Utils.DELETED);
    } catch (Exception e) {
      throw new OperationException(OperationNames.IMPORT_RESOURCE, "Error while importing forum settings", e);
    } finally {
      if (tmpZipFile != null) {
        try {
          String tempFolderPath = tmpZipFile.getAbsolutePath().replaceAll("\\.zip$", "");
          File tempFolderFile = new File(tempFolderPath);
          if (tempFolderFile.exists()) {
            FileUtils.deleteDirectory(tempFolderFile);
          }
          FileUtils.forceDelete(tmpZipFile);
        } catch (Exception e) {
          log.warn("Unable to delete temp file: " + tmpZipFile.getAbsolutePath() + ". Not blocker.");
          tmpZipFile.deleteOnExit();
        }
      }
    }
    resultHandler.completed(NoResultModel.INSTANCE);
  }

  private void importSettingsContent(File fileToImport, String workspace, String locationPath, String targetPath) throws Exception {
    Session session = getSession(workspace);

    if (session.itemExists(targetPath)) {
      Node oldNode = (Node) session.getItem(targetPath);
      oldNode.remove();
      session.save();
      session.refresh(false);
    }

    if (targetPath.endsWith("/")) {
      targetPath = targetPath.substring(0, targetPath.lastIndexOf("/"));
    }
    String parentTargetPath = targetPath.substring(0, targetPath.lastIndexOf("/"));

    FileInputStream inputStream = new FileInputStream(fileToImport);
    session.importXML(parentTargetPath, inputStream, ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
    session.save();
    session.refresh(false);
    inputStream.close();
  }

  private Map<String, File> extractDataFromZip(OperationAttachment attachment, File tmpZipFile) throws OperationException {
    if (attachment == null || attachment.getStream() == null) {
      throw new OperationException(OperationNames.IMPORT_RESOURCE, "No attachment available for forum settings.");
    }
    InputStream inputStream = attachment.getStream();
    try {
      copyAttachementToLocalFolder(inputStream, tmpZipFile);

      // Organize File paths by id and extract files from zip to a temp
      // folder
      return extractFilesById(tmpZipFile);
    } catch (Exception e) {
      throw new OperationException(OperationNames.IMPORT_RESOURCE, "Error occured while handling attachement", e);
    } finally {
      if (inputStream != null) {
        try {
          inputStream.close();
        } catch (IOException e) {
          log.warn("Cannot close input stream.");
        }
      }
    }
  }

  private Map<String, File> extractFilesById(File tmpZipFile) throws Exception {
    // Get path of folder where to unzip files
    String targetFolderPath = tmpZipFile.getAbsolutePath().replaceAll("\\.zip$", "") + "/";

    // Open an input stream on local zip file
    NonCloseableZipInputStream zis = new NonCloseableZipInputStream(new FileInputStream(tmpZipFile));

    Map<String, File> filesToImport = new HashMap<String, File>();
    try {
      Map<String, ZipOutputStream> zipOutputStreamMap = new HashMap<String, ZipOutputStream>();
      String managedEntryPathPrefix = "forum/settings/";
      ZipEntry entry;
      while ((entry = zis.getNextEntry()) != null) {
        String zipEntryPath = entry.getName();
        // Skip entries not managed by this extension
        if (!zipEntryPath.startsWith(managedEntryPathPrefix)) {
          continue;
        }

        // Skip directories
        if (entry.isDirectory()) {
          // Create directory in unzipped folder location
          createFile(new File(targetFolderPath + replaceSpecialChars(zipEntryPath)), true);
          continue;
        }

        String localFilePath = targetFolderPath + replaceSpecialChars(zipEntryPath);
        filesToImport.put(zipEntryPath, new File(localFilePath));

        // Put file Export file in temp folder
        copyToDisk(zis, localFilePath);
        zis.closeEntry();
      }

      Collection<ZipOutputStream> zipOutputStreams = zipOutputStreamMap.values();
      for (ZipOutputStream zipOutputStream : zipOutputStreams) {
        zipOutputStream.close();
      }
    } finally {
      try {
        zis.reallyClose();
      } catch (Exception e) {
        log.warn("Cannot delete temporary file " + tmpZipFile + ". Ignore it.");
      }
    }

    return filesToImport;
  }

}
