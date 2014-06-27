package org.exoplatform.management.social.operations;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.management.social.SocialExtension;
import org.exoplatform.portal.config.DataStorage;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.portal.config.model.Dashboard;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.identity.provider.SpaceIdentityProvider;
import org.exoplatform.social.core.manager.ActivityManager;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.model.AvatarAttachment;
import org.exoplatform.social.core.profile.ProfileFilter;
import org.exoplatform.social.core.space.SpaceUtils;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.gatein.common.logging.Logger;
import org.gatein.common.logging.LoggerFactory;
import org.gatein.management.api.ContentType;
import org.gatein.management.api.PathAddress;
import org.gatein.management.api.controller.ManagedRequest;
import org.gatein.management.api.controller.ManagedResponse;
import org.gatein.management.api.controller.ManagementController;
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
public class SocialDataImportResource implements OperationHandler {

  final private static Logger log = LoggerFactory.getLogger(SocialDataImportResource.class);

  final private static int BUFFER = 2048000;

  private SpaceService spaceService;
  private ActivityManager activityManager;
  private IdentityManager identityManager;
  private ManagementController managementController;
  private UserACL userACL;
  private DataStorage dataStorage;

  @Override
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws OperationException {
    spaceService = operationContext.getRuntimeContext().getRuntimeComponent(SpaceService.class);
    managementController = operationContext.getRuntimeContext().getRuntimeComponent(ManagementController.class);
    activityManager = operationContext.getRuntimeContext().getRuntimeComponent(ActivityManager.class);
    identityManager = operationContext.getRuntimeContext().getRuntimeComponent(IdentityManager.class);
    userACL = operationContext.getRuntimeContext().getRuntimeComponent(UserACL.class);
    dataStorage = operationContext.getRuntimeContext().getRuntimeComponent(DataStorage.class);

    OperationAttributes attributes = operationContext.getAttributes();
    List<String> filters = attributes.getValues("filter");

    // "replace-existing" attribute. Defaults to false.
    boolean replaceExisting = filters.contains("replace-existing:true");

    String spaceDisplayName = operationContext.getAddress().resolvePathTemplate("space-name");
    String spacePrettyName = null;
    if (spaceDisplayName != null) {
      Space space = spaceService.getSpaceByDisplayName(spaceDisplayName);
      if (space != null) {
        if (!replaceExisting) {
          resultHandler.completed(NoResultModel.INSTANCE);
          return;
        }
        spacePrettyName = space.getPrettyName();
      }
    }

    File tmpZipFile = null;
    try {
      // Copy attachement to local temporary File
      tmpZipFile = File.createTempFile("staging-social", ".zip");
      Map<String, Map<String, File>> fileToImportByOwner = extractDataFromZip(operationContext.getAttachment(false), spacePrettyName, tmpZipFile);
      Set<String> spacePrettyNames = fileToImportByOwner.keySet();
      for (String extractedSpacePrettyName : spacePrettyNames) {
        Space space = spaceService.getSpaceByPrettyName(extractedSpacePrettyName);
        if (space != null && !replaceExisting) {
          continue;
        }

        Map<String, File> spaceFiles = fileToImportByOwner.get(extractedSpacePrettyName);
        Set<String> filesKeys = spaceFiles.keySet();
        List<String> filesKeysList = new ArrayList<String>(filesKeys);
        Collections.sort(filesKeysList, new Comparator<String>() {
          @Override
          public int compare(String o1, String o2) {
            if (o1.contains(SocialDashboardExportTask.FILENAME)) {
              return 1;
            }
            if (o2.contains(SocialDashboardExportTask.FILENAME)) {
              return -1;
            }
            return o1.compareTo(o2);
          }
        });

        for (String fileKey : filesKeysList) {
          File fileToImport = spaceFiles.get(fileKey);
          if (fileKey.equals(SocialExtension.ANSWER_RESOURCE_PATH) || fileKey.equals(SocialExtension.CALENDAR_RESOURCE_PATH) || fileKey.equals(SocialExtension.CONTENT_RESOURCE_PATH)
              || fileKey.equals(SocialExtension.FAQ_RESOURCE_PATH) || fileKey.equals(SocialExtension.FORUM_RESOURCE_PATH) || fileKey.equals(SocialExtension.WIKI_RESOURCE_PATH)
              || fileKey.equals(SocialExtension.SITES_IMPORT_RESOURCE_PATH)) {
            importSubResource(fileToImport, fileKey);
          } else {
            if (fileToImport.getAbsolutePath().contains(SpaceActivitiesExportTask.FILENAME)) {
              createActivities(extractedSpacePrettyName, fileToImport);
            } else if (fileToImport.getAbsolutePath().contains(SocialDashboardExportTask.FILENAME)) {
              updateDashboard(space.getGroupId(), fileToImport);
            } else if (fileToImport.getAbsolutePath().contains(SpaceAvatarExportTask.FILENAME)) {
              updateAvatar(space, fileToImport);
            } else {
              log.warn("Cannot handle file: " + fileToImport.getAbsolutePath() + ". Ignore it.");
            }
          }
          try {
            fileToImport.delete();
          } catch (Exception e) {
            log.warn("Cannot delete temporary file from disk: " + fileToImport.getAbsolutePath() + ". It seems we have an opened InputStream. Anyway, it's not blocker.", e);
          }
        }
        log.info("Import operation finished.");
      }
    } catch (IOException e) {
      log.warn("Cannot create temporary file.", e);
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
          log.warn("Unable to delete temp file: " + tmpZipFile.getAbsolutePath() + ". Not blocker.", e);
          tmpZipFile.deleteOnExit();
        }
      }
    }

    resultHandler.completed(NoResultModel.INSTANCE);
  }

  private void updateAvatar(Space space, File fileToImport) {
    FileInputStream inputStream = null;
    try {
      inputStream = new FileInputStream(fileToImport);

      XStream xstream = new XStream();
      AvatarAttachment avatarAttachment = (AvatarAttachment) xstream.fromXML(inputStream);
      space.setAvatarAttachment(avatarAttachment);

      spaceService.updateSpaceAvatar(space);
    } catch (Exception e) {
      throw new OperationException(OperationNames.IMPORT_RESOURCE, "Error while updating Space '" + space.getDisplayName() + "' avatar.", e);
    } finally {
      if (inputStream != null) {
        try {
          inputStream.close();
        } catch (IOException e) {
          log.warn("Cannot close input stream: " + fileToImport.getAbsolutePath() + ". Ignore non blocking operation.");
        }
      }
    }
  }

  private void updateDashboard(String spaceGroupId, File fileToImport) {
    FileInputStream inputStream = null;
    try {
      Dashboard dashboard = SocialDashboardExportTask.getDashboard(dataStorage, spaceGroupId);
      if (dashboard == null) {
        return;
      }

      inputStream = new FileInputStream(fileToImport);

      XStream xstream = new XStream();
      Dashboard newDashboard = (Dashboard) xstream.fromXML(inputStream);

      dashboard.setAccessPermissions(newDashboard.getAccessPermissions());
      dashboard.setChildren(newDashboard.getChildren());
      dashboard.setDecorator(newDashboard.getDecorator());
      dashboard.setDescription(newDashboard.getDescription());
      dashboard.setFactoryId(newDashboard.getFactoryId());
      dashboard.setHeight(newDashboard.getHeight());
      dashboard.setIcon(newDashboard.getIcon());
      dashboard.setName(newDashboard.getName());
      dashboard.setTemplate(newDashboard.getTemplate());
      dashboard.setTitle(newDashboard.getTitle());
      dashboard.setWidth(newDashboard.getWidth());

      dataStorage.saveDashboard(newDashboard);
    } catch (Exception e) {
      throw new OperationException(OperationNames.IMPORT_RESOURCE, "Error while updating Space '" + spaceGroupId + "' dashbord.", e);
    } finally {
      if (inputStream != null) {
        try {
          inputStream.close();
        } catch (IOException e) {
          log.warn("Cannot close input stream: " + fileToImport.getAbsolutePath() + ". Ignore non blocking operation.");
        }
      }
    }
  }

  private void createActivities(String spacePrettyName, File activitiesFile) {
    FileInputStream inputStream = null;
    try {
      inputStream = new FileInputStream(activitiesFile);
      // Unmarshall metadata xml file
      XStream xstream = new XStream();
      ExoSocialActivity[] activities = (ExoSocialActivity[]) xstream.fromXML(inputStream);
      List<ExoSocialActivity> activitiesList = new ArrayList<ExoSocialActivity>();
      ProfileFilter profileFilter = new ProfileFilter();
      Identity identity = null;
      for (ExoSocialActivity activity : activities) {
        profileFilter.setName(activity.getUserId());
        identity = getIdentity(profileFilter);

        if (identity != null) {
          activity.setUserId(identity.getId());

          profileFilter.setName(activity.getPosterId());
          identity = getIdentity(profileFilter);

          if (identity != null) {
            activity.setPosterId(identity.getId());
            activitiesList.add(activity);
          }
        }
      }
      Identity spaceIdentity = identityManager.getOrCreateIdentity(SpaceIdentityProvider.NAME, spacePrettyName, false);
      for (ExoSocialActivity exoSocialActivity : activitiesList) {
        activityManager.saveActivityNoReturn(spaceIdentity, exoSocialActivity);
      }
    } catch (FileNotFoundException e) {
      throw new OperationException(OperationNames.IMPORT_RESOURCE, "Cannot find extracted file: " + activitiesFile.getAbsolutePath(), e);
    } finally {
      if (inputStream != null) {
        try {
          inputStream.close();
        } catch (IOException e) {
          log.warn("Cannot close input stream: " + activitiesFile.getAbsolutePath() + ". Ignore non blocking operation.");
        }
      }
    }
  }

  private Identity getIdentity(ProfileFilter profileFilter) {
    ListAccess<Identity> identities = identityManager.getIdentitiesByProfileFilter(OrganizationIdentityProvider.NAME, profileFilter, false);
    try {
      if (identities.getSize() > 0) {
        return identities.load(0, 1)[0];
      }
    } catch (Exception e) {
      log.error(e);
    }
    return null;
  }

  private void createOrReplaceSpace(String spacePrettyName, InputStream inputStream) throws IOException {
    Space space = spaceService.getSpaceByPrettyName(spacePrettyName);
    // Unmarshall metadata xml file
    XStream xstream = new XStream();
    xstream.alias("metadata", SpaceMetaData.class);
    SpaceMetaData spaceMetaData = (SpaceMetaData) xstream.fromXML(inputStream);

    if (space != null) {
      log.info("Delete space: '" + spaceMetaData.getPrettyName() + "'.");
      spaceService.deleteSpace(space);
    }
    log.info("Create new space: '" + spaceMetaData.getPrettyName() + "'.");
    space = new Space();
    space.setPrettyName(spaceMetaData.getPrettyName());
    space.setDisplayName(spaceMetaData.getDisplayName());
    space.setGroupId(SpaceUtils.SPACE_GROUP + "/" + spacePrettyName);
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
  }

  private void importSubResource(File tempFile, String subResourcePath) {
    Map<String, List<String>> attributesMap = new HashMap<String, List<String>>();
    attributesMap.put("filter", Collections.singletonList("replace-existing:true"));

    // This will be closed in sub resources, don't close it here
    InputStream inputStream = null;
    try {
      inputStream = new FileInputStream(tempFile);
      ManagedRequest request = ManagedRequest.Factory.create(OperationNames.IMPORT_RESOURCE, PathAddress.pathAddress(subResourcePath), attributesMap, inputStream, ContentType.ZIP);
      ManagedResponse response = managementController.execute(request);
      Object model = response.getResult();
      if (!(model instanceof NoResultModel)) {
        throw new OperationException(OperationNames.IMPORT_RESOURCE, "Unknown error while importing to path: " + subResourcePath);
      }
    } catch (FileNotFoundException e) {
      throw new OperationException(OperationNames.IMPORT_RESOURCE, "Cannot find extracted file: " + tempFile.getAbsolutePath(), e);
    } finally {
      if (inputStream != null) {
        try {
          inputStream.close();
        } catch (IOException e) {
          log.warn("Cannot close input stream: " + tempFile + ". Ignore non blocking operation.");
        }
      }
    }
  }

  private Map<String, Map<String, File>> extractDataFromZip(OperationAttachment attachment, String spacePrettyName, File tmpZipFile) throws OperationException {
    if (attachment == null || attachment.getStream() == null) {
      throw new OperationException(OperationNames.IMPORT_RESOURCE, "No attachment available for Social import.");
    }
    InputStream inputStream = attachment.getStream();
    try {
      copyAttachementToLocalFolder(inputStream, tmpZipFile);

      // Organize File paths by id and extract files from zip to a temp
      // folder
      return extractFilesById(tmpZipFile, spacePrettyName);
    } catch (Exception e) {
      throw new OperationException(OperationNames.IMPORT_RESOURCE, "Error occured while handling attachement", e);
    }
  }

  private void copyAttachementToLocalFolder(InputStream attachmentInputStream, File tmpZipFile) throws IOException, FileNotFoundException {
    NonCloseableZipInputStream zis = null;
    try {
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
  }

  private Map<String, Map<String, File>> extractFilesById(File tmpZipFile, String targetSpacePrettyName) throws Exception {
    // Get path of folder where to unzip files
    String targetFolderPath = tmpZipFile.getAbsolutePath().replaceAll("\\.zip$", "") + "/";

    // Open an input stream on local zip file
    NonCloseableZipInputStream zis = new NonCloseableZipInputStream(new FileInputStream(tmpZipFile));

    Map<String, Map<String, File>> filesToImportByOwner = new HashMap<String, Map<String, File>>();
    try {
      Map<String, ZipOutputStream> zipOutputStreamMap = new HashMap<String, ZipOutputStream>();
      String managedEntryPathPrefix = "social/space/" + (targetSpacePrettyName == null ? "" : targetSpacePrettyName);
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
        int idBeginIndex = ("social/space/").length();
        String spacePrettyName = zipEntryPath.substring(idBeginIndex, zipEntryPath.indexOf("/", idBeginIndex));

        if (!filesToImportByOwner.containsKey(spacePrettyName)) {
          filesToImportByOwner.put(spacePrettyName, new HashMap<String, File>());
        }
        Map<String, File> ownerFiles = filesToImportByOwner.get(spacePrettyName);

        log.info("Handling content " + zipEntryPath);

        if (zipEntryPath.contains(SocialExtension.ANSWER_RESOURCE_PATH)) {
          putSubResourceEntry(tmpZipFile, targetFolderPath, zis, zipOutputStreamMap, zipEntryPath, spacePrettyName, ownerFiles, SocialExtension.ANSWER_RESOURCE_PATH);
        } else if (zipEntryPath.contains(SocialExtension.CALENDAR_RESOURCE_PATH)) {
          putSubResourceEntry(tmpZipFile, targetFolderPath, zis, zipOutputStreamMap, zipEntryPath, spacePrettyName, ownerFiles, SocialExtension.CALENDAR_RESOURCE_PATH);
        } else if (zipEntryPath.contains(SocialExtension.CONTENT_RESOURCE_PATH)) {
          putSubResourceEntry(tmpZipFile, targetFolderPath, zis, zipOutputStreamMap, zipEntryPath, spacePrettyName, ownerFiles, SocialExtension.CONTENT_RESOURCE_PATH);
        } else if (zipEntryPath.contains(SocialExtension.FAQ_RESOURCE_PATH)) {
          putSubResourceEntry(tmpZipFile, targetFolderPath, zis, zipOutputStreamMap, zipEntryPath, spacePrettyName, ownerFiles, SocialExtension.FAQ_RESOURCE_PATH);
        } else if (zipEntryPath.contains(SocialExtension.FORUM_RESOURCE_PATH)) {
          putSubResourceEntry(tmpZipFile, targetFolderPath, zis, zipOutputStreamMap, zipEntryPath, spacePrettyName, ownerFiles, SocialExtension.FORUM_RESOURCE_PATH);
        } else if (zipEntryPath.contains(SocialExtension.WIKI_RESOURCE_PATH)) {
          putSubResourceEntry(tmpZipFile, targetFolderPath, zis, zipOutputStreamMap, zipEntryPath, spacePrettyName, ownerFiles, SocialExtension.WIKI_RESOURCE_PATH);
        } else if (zipEntryPath.contains(SocialExtension.GROUP_SITE_RESOURCE_PATH)) {
          putSubResourceEntry(tmpZipFile, targetFolderPath, zis, zipOutputStreamMap, zipEntryPath, spacePrettyName, ownerFiles, SocialExtension.SITES_IMPORT_RESOURCE_PATH);
        } else {
          String localFilePath = targetFolderPath + replaceSpecialChars(zipEntryPath);
          if (localFilePath.endsWith(SpaceMetadataExportTask.FILENAME)) {
            createOrReplaceSpace(spacePrettyName, zis);
          } else {
            ownerFiles.put(zipEntryPath, new File(localFilePath));

            // Put file Export file in temp folder
            copyToDisk(zis, localFilePath);
          }
        }
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

    return filesToImportByOwner;
  }

  private void putSubResourceEntry(File tmpZipFile, String targetFolderPath, NonCloseableZipInputStream zis, Map<String, ZipOutputStream> zipOutputStreamMap, String zipEntryPath,
      String spacePrettyName, Map<String, File> ownerFiles, String subResourcePath) throws IOException, FileNotFoundException {
    if (!ownerFiles.containsKey(subResourcePath)) {
      createFile(new File(targetFolderPath + "social/space/" + spacePrettyName + subResourcePath), true);
      String zipFilePath = targetFolderPath + "social/space/" + spacePrettyName + subResourcePath + ".zip";
      ownerFiles.put(subResourcePath, new File(zipFilePath));
      ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFilePath));
      zipOutputStreamMap.put(spacePrettyName + subResourcePath, zos);
    }
    ZipOutputStream zos = zipOutputStreamMap.get(spacePrettyName + subResourcePath);
    String subResourceZipEntryPath = zipEntryPath.replace("social/space/" + spacePrettyName + "/", "");
    zos.putNextEntry(new ZipEntry(subResourceZipEntryPath));
    IOUtils.copy(zis, zos);
    zos.closeEntry();
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

  private static File createFile(File file, boolean folder) throws IOException {
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

}
