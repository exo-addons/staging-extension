/*
 * Copyright (C) 2003-2017 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.exoplatform.management.common.importop;

import com.thoughtworks.xstream.XStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.exoplatform.management.common.AbstractOperationHandler;
import org.exoplatform.management.common.FileEntry;
import org.exoplatform.management.common.SpaceMetaData;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.activity.model.ActivityStream.Type;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.manager.ActivityManager;
import org.exoplatform.social.core.space.SpaceUtils;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.storage.ActivityStorageException;
import org.exoplatform.social.core.storage.api.ActivityStorage;
import org.exoplatform.social.core.storage.cache.CachedActivityStorage;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.operation.OperationAttachment;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.OperationNames;

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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * The Class AbstractImportOperationHandler.
 */
public abstract class AbstractImportOperationHandler extends AbstractOperationHandler {

  /** The Constant log. */
  protected static final Log log = ExoLogger.getLogger(AbstractImportOperationHandler.class);

  /** The Constant EMPTY_STRING_ARRAY. */
  protected static final String[] EMPTY_STRING_ARRAY = new String[0];

  /** The user ACL. */
  protected UserACL userACL;
  
  /** The activity manager. */
  protected ActivityManager activityManager;
  
  /** The activity storage. */
  protected ActivityStorage activityStorage;

  /** The activities by post time. */
  // This is used to test on duplicated activities
  protected Set<Long> activitiesByPostTime = new HashSet<Long>();

  /**
   * Delete activities.
   *
   * @param activities the activities
   */
  protected void deleteActivities(ExoSocialActivity[] activities) {
    for (ExoSocialActivity activity : activities) {
      deleteActivity(activity);
    }
  }

  /**
   * Delete activities.
   *
   * @param activities the activities
   */
  protected void deleteActivities(List<ExoSocialActivity> activities) {
    for (ExoSocialActivity activity : activities) {
      deleteActivity(activity);
    }
  }

  /**
   * Delete activities by id.
   *
   * @param activityIds the activity ids
   */
  protected void deleteActivitiesById(List<String> activityIds) {
    for (String activityId : activityIds) {
      deleteActivity(activityId);
    }
  }

  /**
   * Delete activity.
   *
   * @param activityId the activity id
   */
  protected final void deleteActivity(String activityId) {
    if (activityId == null || activityId.isEmpty()) {
      return;
    }
    ExoSocialActivity activity = activityManager.getActivity(activityId);
    deleteActivity(activity);
  }

  /**
   * Delete activity.
   *
   * @param activity the activity
   */
  protected final void deleteActivity(ExoSocialActivity activity) {
    if (activity == null) {
      return;
    }
    log.info("   Delete activity : '" + activity.getTitle() + "' and its comments.");
    // RealtimeListAccess<ExoSocialActivity> commentsListAccess =
    // activityManager.getCommentsWithListAccess(activity);
    // if (commentsListAccess.getSize() > 0) {
    // List<ExoSocialActivity> comments = commentsListAccess.loadAsList(0,
    // commentsListAccess.getSize());
    // for (ExoSocialActivity commentActivity : comments) {
    // try {
    // activityManager.deleteComment(activity, commentActivity);
    // } catch (Exception e) {
    // log.warn("Can't delete comment '" + commentActivity.getTitle() +
    // "' of activity '" + activity.getTitle() + "'");
    // }
    // }
    // }
    activityManager.deleteActivity(activity);
    if (activityStorage instanceof CachedActivityStorage) {
      ((CachedActivityStorage) activityStorage).clearActivityCached(activity.getId());
    }
  }

  /**
   * Creates the space if not exists.
   *
   * @param spaceMetadataFile the space metadata file
   * @param createSpace the create space
   * @return true, if successful
   * @throws Exception the exception
   */
  protected final boolean createSpaceIfNotExists(File spaceMetadataFile, boolean createSpace) throws Exception {
    if (spaceMetadataFile == null || !spaceMetadataFile.exists()) {
      return false;
    }
    SpaceMetaData spaceMetaData = deserializeObject(spaceMetadataFile, SpaceMetaData.class, "metadata");
    Space space = spaceService.getSpaceByGroupId(spaceMetaData.getGroupId());
    if (space == null && createSpace) {
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
    }
    return (space != null);
  }

  /**
   * Import activities.
   *
   * @param activitiesFile the activities file
   * @param spacePrettyName the space pretty name
   * @param clearImportedList the clear imported list
   */
  protected void importActivities(File activitiesFile, String spacePrettyName, boolean clearImportedList) {
    if (clearImportedList) {
      activitiesByPostTime.clear();
    }
    List<ExoSocialActivity> activitiesList = retrieveActivitiesFromFile(activitiesFile);

    boolean isParentActivityIgnored = true;
    String originialParentActivityId = null;
    ExoSocialActivity parentActivity = null;

    try {
      for (ExoSocialActivity activity : activitiesList) {
        if (activity == null) {
          continue;
        }
        if (activity.getId() == null) {
          log.warn("Attempt to import activity with null id.");
          continue;
        }
        try {
          if (activity.isComment()) {
            if (isParentActivityIgnored) {
              continue;
            }
            if (originialParentActivityId == null) {
              log.warn("Attempt to add a comment activity to a non existing page activity.");
              continue;
            }
            // FIXME getParentId not compatible with 4.0.7
            // if (activity.getParentId() == null) {
            // log.warn("Attempt to add a comment activity with null parent id.");
            // continue;
            // }
            // if (!activity.getParentId().equals(originialParentActivityId)) {
            // log.warn("Attempt to add a comment activity with different parent id from previous saved Activity.");
            // continue;
            // }
            if (parentActivity == null) {
              log.warn("Attempt to add a comment on null parent activity.");
              continue;
            }
            if (((ActivityImportOperationInterface) this).isActivityNotValid(parentActivity, activity)) {
              continue;
            }
            activity.setId(null);
            saveComment(parentActivity, activity);
            ((ActivityImportOperationInterface) this).attachActivityToEntity(parentActivity, activity);
          } else {
            isParentActivityIgnored = true;
            originialParentActivityId = null;
            parentActivity = null;

            if (((ActivityImportOperationInterface) this).isActivityNotValid(activity, null)) {
              continue;
            }

            activity.setId(null);
            saveActivity(activity, spacePrettyName);
            if (activity.getId() == null) {
              continue;
            } else {
              isParentActivityIgnored = false;
              originialParentActivityId = activity.getId();
              parentActivity = activity;
            }
            ((ActivityImportOperationInterface) this).attachActivityToEntity(activity, null);
          }
        } catch (Exception e) {
          log.warn("Error while adding activity: " + activity.getTitle(), e);
        }
      }
    } finally {
      deleteTempFile(activitiesFile);
    }
  }

  /**
   * Gets the attachement input stream.
   *
   * @param operationContext the operation context
   * @return the attachement input stream
   */
  protected InputStream getAttachementInputStream(OperationContext operationContext) {
    OperationAttachment attachment = operationContext.getAttachment(false);
    if (attachment == null) {
      throw new OperationException(OperationNames.IMPORT_RESOURCE, "No attachment available for import.");
    }

    InputStream attachmentInputStream = attachment.getStream();
    if (attachmentInputStream == null) {
      throw new OperationException(OperationNames.IMPORT_RESOURCE, "No data stream available for import.");
    }
    return attachmentInputStream;
  }

  /**
   * Save comment.
   *
   * @param activity the activity
   * @param comment the comment
   */
  protected final void saveComment(ExoSocialActivity activity, ExoSocialActivity comment) {
    long updatedTime = activity.getUpdated().getTime();
    if (activity.getId() == null) {
      log.warn("Parent activity '" + activity.getTitle() + "' has a null ID, cannot import activity comment '" + comment.getTitle() + "'.");
      return;
    }
    activity = activityManager.getActivity(activity.getId());
    try {
      activityManager.saveComment(activity, comment);
    } catch (NullPointerException e) {
      log.warn("Error while importing comment: '" + comment.getTitle() + "'.");
      return;
    }
    if (comment.getId() == null) {
      log.warn("Error while importing comment, id is null: '" + comment.getTitle() + "'.");
      return;
    }
    activity = activityManager.getActivity(activity.getId());
    activity.setUpdated(updatedTime);
    activityManager.updateActivity(activity);
    log.info("Comment activity is imported: '" + comment.getTitle() + "'.");
  }

  /**
   * Save activity.
   *
   * @param activity the activity
   * @param spacePrettyName the space pretty name
   */
  protected final void saveActivity(ExoSocialActivity activity, String spacePrettyName) {
    long updatedTime = activity.getUpdated().getTime();
    if (spacePrettyName == null) {
      if (activity.getActivityStream() != null && activity.getActivityStream().getType().equals(Type.SPACE) && activity.getActivityStream().getPrettyId() != null) {
        spacePrettyName = activity.getActivityStream().getPrettyId();
      }
    }
    if (spacePrettyName == null) {
      try {
        activityManager.saveActivityNoReturn(activity);
        activity.setUpdated(updatedTime);
        activityManager.updateActivity(activity);
        if (activity.getId() == null) {
          log.warn("Activity '" + activity.getTitle() + "' is not imported, id is null");
        } else {
          log.info("Activity  is imported: '" + activity.getTitle() + "'");
        }
      } catch (ActivityStorageException e) {
        log.warn("Activity is not imported, it may already exist: '" + activity.getTitle() + "'.");
      }
    } else {
      Identity spaceIdentity = getIdentity(spacePrettyName);
      if (spaceIdentity == null) {
        log.warn("Activity is not imported: '" + activity.getTitle() + "'.");
        return;
      }
      try {
        activityManager.saveActivityNoReturn(spaceIdentity, activity);
        activity.setUpdated(updatedTime);
        activityManager.updateActivity(activity);
        if (activity.getId() == null) {
          log.warn("Activity '" + activity.getTitle() + "' is not imported, id is null");
        } else {
          log.info("Activity  is imported: '" + activity.getTitle() + "'");
        }
      } catch (ActivityStorageException e) {
        log.warn("Activity is not imported, it may already exist: '" + activity.getTitle() + "'.");
      }
    }
  }

  /**
   * Retrieve activities from file.
   *
   * @param activitiesFile the activities file
   * @return the list
   */
  @SuppressWarnings("unchecked")
  protected List<ExoSocialActivity> retrieveActivitiesFromFile(File activitiesFile) {
    List<ExoSocialActivity> activities = null;
    try {
      // Unmarshall metadata xml file
      activities = (List<ExoSocialActivity>) deserializeObject(activitiesFile, List.class, null);
    } catch (Exception e) {
      throw new OperationException(OperationNames.IMPORT_RESOURCE, "Cannot find extracted file: " + (activitiesFile != null ? activitiesFile.getAbsolutePath() : activitiesFile), e);
    }
    return sanitizeContent(activities);
  }

  /**
   * Sanitize content.
   *
   * @param activities the activities
   * @return the list
   */
  protected final List<ExoSocialActivity> sanitizeContent(List<ExoSocialActivity> activities) {
    List<ExoSocialActivity> activitiesList = new ArrayList<ExoSocialActivity>();
    Identity identity = null;
    boolean ignoreNextComments = false;
    for (ExoSocialActivity activity : activities) {
      if (ignoreNextComments) {
        if (activity.isComment()) {
          continue;
        } else {
          ignoreNextComments = false;
        }
      }
      if (activity.getPostedTime() != null && activitiesByPostTime.contains(activity.getPostedTime())) {
        log.info("Ignore duplicated Activity '" + activity.getTitle() + "'.");
        continue;
      } else {
        activitiesByPostTime.add(activity.getPostedTime());
      }
      identity = getIdentity(activity.getUserId());
      if (identity != null) {
        activity.setUserId(identity.getId());
        identity = getIdentity(activity.getPosterId());
        if (identity != null) {
          activitiesList.add(activity);

          activity.setPosterId(identity.getId());

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
          activity.setReplyToId(null);

          String[] commentedIds = activity.getCommentedIds();
          commentedIds = changeUsernameIdToIdentity(commentedIds);
          activity.setCommentedIds(commentedIds);

          String[] mentionedIds = activity.getMentionedIds();
          mentionedIds = changeUsernameIdToIdentity(mentionedIds);
          activity.setMentionedIds(mentionedIds);

          String[] likeIdentityIds = activity.getLikeIdentityIds();
          likeIdentityIds = changeUsernameIdToIdentity(likeIdentityIds);
          activity.setLikeIdentityIds(likeIdentityIds);
        }
      }
      if (identity == null) {
        ignoreNextComments = true;
        log.info("ACTIVITY IS NOT IMPORTED because the associated user '" + activity.getUserId() + "' wasn't found:  '" + activity.getTitle() + "'");
      }
    }
    return activitiesList;
  }

  /**
   * Change username id to identity.
   *
   * @param ids the ids
   * @return the string[]
   */
  private String[] changeUsernameIdToIdentity(String[] ids) {
    List<String> resultIds = new ArrayList<String>();
    if (ids != null && ids.length > 0) {
      for (int i = 0; i < ids.length; i++) {
        String[] id = ids[i].split("@");
        Identity identity = getIdentity(id[0]);
        if (identity != null) {
          id[0] = (String) identity.getId();
          if (id.length == 2) {
            ids[i] = id[0] + "@" + id[1];
          } else {
            ids[i] = id[0];
          }
          resultIds.add(ids[i]);
        }
      }
      ids = resultIds.toArray(EMPTY_STRING_ARRAY);
    }
    return ids;
  }

  /**
   * Copy attachement to local folder.
   *
   * @param attachmentInputStream the attachment input stream
   * @return the file
   * @throws IOException Signals that an I/O exception has occurred.
   * @throws FileNotFoundException the file not found exception
   */
  protected final File copyAttachementToLocalFolder(InputStream attachmentInputStream) throws IOException, FileNotFoundException {
    File tmpZipFile = File.createTempFile("staging", ".zip");
    copyAttachementToLocalFolder(attachmentInputStream, tmpZipFile);
    return tmpZipFile;
  }

  /**
   * Copy attachement to local folder.
   *
   * @param attachmentInputStream the attachment input stream
   * @param tmpZipFile the tmp zip file
   * @throws IOException Signals that an I/O exception has occurred.
   * @throws FileNotFoundException the file not found exception
   */
  protected final void copyAttachementToLocalFolder(InputStream attachmentInputStream, File tmpZipFile) throws IOException, FileNotFoundException {
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

  /**
   * Copy to disk.
   *
   * @param input the input
   * @param output the output
   * @return true, if successful
   * @throws Exception the exception
   */
  protected final static boolean copyToDisk(InputStream input, String output) throws Exception {
    return copyToDisk(input, new File(output));
  }

  /**
   * Copy to disk.
   *
   * @param input the input
   * @param file the file
   * @return true, if successful
   * @throws Exception the exception
   */
  protected final static boolean copyToDisk(InputStream input, File file) throws Exception {
    createFile(file, false);

    FileOutputStream fileOuput = null;
    try {
      fileOuput = new FileOutputStream(file);
      IOUtils.copy(input, fileOuput);
      return true;
    } catch (Exception e) {
      log.error("Error while copying file: " + file.getAbsolutePath(), e);
      return false;
    } finally {
      if (fileOuput != null) {
        fileOuput.close();
      }
    }
  }

  /**
   * Replace special chars.
   *
   * @param name the name
   * @return the string
   */
  protected final static String replaceSpecialChars(String name) {
    name = name.replaceAll(":", "_");
    return name.replaceAll("\\?", "_");
  }

  /**
   * Creates the file.
   *
   * @param file the file
   * @param folder the folder
   * @return the file
   * @throws Exception the exception
   */
  protected final static File createFile(File file, boolean folder) throws Exception {
    if (file.getParentFile() != null)
      createFile(file.getParentFile(), true);
    if (file.exists())
      return file;
    if (file.isDirectory() || folder)
      file.mkdir();
    else file.createNewFile();
    return file;
  }

  /**
   * Gets the and remove file by path.
   *
   * @param fileEntries the file entries
   * @param nodePath the node path
   * @return the and remove file by path
   */
  public final static FileEntry getAndRemoveFileByPath(List<FileEntry> fileEntries, String nodePath) {
    Iterator<FileEntry> iterator = fileEntries.iterator();
    while (iterator.hasNext()) {
      FileEntry fileEntry = (FileEntry) iterator.next();
      if (fileEntry.getNodePath().equals(nodePath)) {
        iterator.remove();
        return fileEntry;
      }
    }
    return null;
  }

  /**
   * Gets the and remove files starts with.
   *
   * @param fileEntries the file entries
   * @param nodePath the node path
   * @return the and remove files starts with
   */
  public final static List<FileEntry> getAndRemoveFilesStartsWith(List<FileEntry> fileEntries, String nodePath) {
    List<FileEntry> files = new ArrayList<FileEntry>();
    Iterator<FileEntry> iterator = fileEntries.iterator();
    while (iterator.hasNext()) {
      FileEntry fileEntry = (FileEntry) iterator.next();
      if (fileEntry.getNodePath().startsWith(nodePath)) {
        files.add(fileEntry);
        iterator.remove();
      }
    }
    return files;
  }

  /**
   * Extract data from zip.
   *
   * @param attachmentInputStream the attachment input stream
   * @return the map
   * @throws Exception the exception
   */
  public final Map<String, List<FileEntry>> extractDataFromZip(InputStream attachmentInputStream) throws Exception {
    Map<String, List<FileEntry>> contentsByOwner = new HashMap<String, List<FileEntry>>();
    File tmpZipFile = null;
    try {
      tmpZipFile = copyAttachementToLocalFolder(attachmentInputStream);

      // Organize File paths by wikiOwner and extract files from zip to a temp
      // folder
      extractFilesByOwner(tmpZipFile, contentsByOwner);
    } finally {
      if (tmpZipFile != null) {
        deleteTempFile(tmpZipFile);
      }
    }
    return contentsByOwner;
  }

  /**
   * Extract files by owner.
   *
   * @param tmpZipFile the tmp zip file
   * @param contentsByOwner the contents by owner
   * @throws FileNotFoundException the file not found exception
   * @throws IOException Signals that an I/O exception has occurred.
   * @throws Exception the exception
   */
  protected final void extractFilesByOwner(File tmpZipFile, Map<String, List<FileEntry>> contentsByOwner) throws FileNotFoundException, IOException, Exception {
    // Open an input stream on local zip file
    NonCloseableZipInputStream zis = new NonCloseableZipInputStream(new FileInputStream(tmpZipFile));

    try {
      ZipEntry entry;
      while ((entry = zis.getNextEntry()) != null) {
        String filePath = entry.getName();
        // Skip entries not managed by this extension
        if (filePath.equals("") || !filePath.startsWith(((FileImportOperationInterface) this).getManagedFilesPrefix())) {
          continue;
        }

        // Skip directories
        if (entry.isDirectory()) {
          continue;
        }

        // Skip non managed
        boolean isFileNotKnown = ((FileImportOperationInterface) this).isUnKnownFileFormat(filePath);
        if (isFileNotKnown) {
          log.warn("Uknown file format found at location: '" + filePath + "'. Ignore it.");
          continue;
        }

        log.info("Receiving content " + filePath);

        File file = File.createTempFile("staging", ".xml");

        // Put XML Export file in temp folder
        copyToDisk(zis, file);

        // Extract wiki owner
        String owner = ((FileImportOperationInterface) this).extractIdFromPath(filePath);

        List<FileEntry> fileEntries = contentsByOwner.get(owner);
        // Add nodePath by Owner
        if (fileEntries == null) {
          fileEntries = new ArrayList<FileEntry>();
          contentsByOwner.put(owner, fileEntries);
        }

        // Treat special files
        boolean isSpecialFile = ((FileImportOperationInterface) this).addSpecialFile(fileEntries, filePath, file);
        if (isSpecialFile) {
          continue;
        }

        String nodePath = ((FileImportOperationInterface) this).getNodePath(filePath);
        if (nodePath == null) {
          continue;
        }
        fileEntries.add(new FileEntry(nodePath, file));
      }
    } finally {
      if (zis != null) {
        zis.reallyClose();
      }
    }
    Collection<List<FileEntry>> lists = contentsByOwner.values();
    for (List<FileEntry> list : lists) {
      Collections.sort(list);
    }
  }

  /**
   * Delete temp file.
   *
   * @param file the file
   */
  protected static void deleteTempFile(File file) {
    try {
      if (file != null && file.exists()) {
        if (file.isDirectory()) {
          FileUtils.deleteDirectory(file);
        } else {
          FileUtils.forceDelete(file);
        }
      }
    } catch (Exception e) {
      if (log.isDebugEnabled()) {
        log.debug("Cannot delete temporary file from disk: " + file.getAbsolutePath() + ". Not blocker.", e);
      }
    }
  }

  /**
   * Deserialize object.
   *
   * @param <T> the generic type
   * @param file the file
   * @param objectClass the object class
   * @param alias the alias
   * @return the t
   * @throws Exception the exception
   */
  protected static final <T> T deserializeObject(File file, Class<T> objectClass, String alias) throws Exception {
    FileInputStream inputStream = null;
    try {
      inputStream = new FileInputStream(file);
      return deserializeObject(inputStream, objectClass, alias);
    } finally {
      if (inputStream != null) {
        inputStream.close();
        deleteTempFile(file);
      }
    }
  }

  /**
   * Deserialize object.
   *
   * @param <T> the generic type
   * @param zin the zin
   * @param objectClass the object class
   * @param alias the alias
   * @return the t
   */
  protected static final <T> T deserializeObject(final InputStream zin, Class<T> objectClass, String alias) {
    XStream xStream = new XStream();
    if (objectClass != null && alias != null) {
      xStream.alias(alias, objectClass);
    }
    @SuppressWarnings("unchecked")
    T object = (T) xStream.fromXML(zin);
    return object;
  }

  // Bug in SUN's JDK XMLStreamReader implementation closes the underlying
  // stream when
  // it finishes reading an XML document. This is no good when we are using
  // a
  // ZipInputStream.
  // See http://bugs.sun.com/view_bug.do?bug_id=6539065 for more
  /**
   * The Class NonCloseableZipInputStream.
   */
  // information.
  public static class NonCloseableZipInputStream extends ZipInputStream {
    
    /**
     * Instantiates a new non closeable zip input stream.
     *
     * @param inputStream the input stream
     */
    public NonCloseableZipInputStream(InputStream inputStream) {
      super(inputStream);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws IOException {}

    /**
     * Really close.
     *
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public void reallyClose() throws IOException {
      super.close();
    }
  }
}
