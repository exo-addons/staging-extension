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
import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.exoplatform.forum.common.jcr.KSDataLocation;
import org.exoplatform.forum.service.Category;
import org.exoplatform.forum.service.Forum;
import org.exoplatform.forum.service.ForumService;
import org.exoplatform.forum.service.Utils;
import org.exoplatform.management.forum.ForumExtension;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
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
public class ForumDataImportResource implements OperationHandler {

  final private static Logger log = LoggerFactory.getLogger(ForumDataImportResource.class);

  private RepositoryService repositoryService;
  private SpaceService spaceService;
  private ForumService forumService;
  private KSDataLocation dataLocation;
  private UserACL userACL;

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
          boolean spaceCreatedOrAlreadyExists = createSpaceIfNotExists(tempFolderPath, forumId, createSpace);
          if (!spaceCreatedOrAlreadyExists) {
            log.warn("Import of forum category '" + categoryId + "' is ignored. Turn on 'create-space:true' option if you want to automatically create the space.");
            continue;
          }
          
          Category spaceCategory = forumService.getCategoryIncludedSpace();
          Forum forum = forumService.getForum(spaceCategory.getId(), forumId);
          if (forum != null) {
            if (replaceExisting) {
              log.info("Overwrite existing Space Forum: '" + forum.getForumName() + "'  (replace-existing=true)");
            } else {
              log.info("Ignore existing Space Forum: '" + forum.getForumName() + "'  (replace-existing=false)");
              continue;
            }
          }

          List<String> paths = contentsByOwner.get(forumId);

          Collections.sort(paths);
          for (String nodePath : paths) {
            importNode(forumId, nodePath, workspace, tempFolderPath);
          }
        } else {
          Category category = forumService.getCategory(categoryId);
          if (category != null) {
            if (replaceExisting) {
              log.info("Overwrite existing Forum Category: '" + category.getCategoryName() + "'  (replace-existing=true)");
            } else {
              log.info("Ignore existing Forum Category: '" + category.getCategoryName() + "'  (replace-existing=false)");
              continue;
            }
          }

          List<String> paths = contentsByOwner.get(categoryId);

          Collections.sort(paths);
          for (String nodePath : paths) {
            importNode(categoryId, nodePath, workspace, tempFolderPath);
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
          log.warn("Unable to delete temp file: " + tmpZipFile.getAbsolutePath() + ". Not blocker.", e);
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
    Session session = getSession(workspace, repositoryService);
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
    session = getSession(workspace, repositoryService);
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
        if (!filePath.endsWith(".xml") && !filePath.endsWith(SpaceMetadataExportTask.FILENAME)) {
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
        String nodePath = filePath.substring(filePath.indexOf(id + "/") + (id + "/").length(), filePath.lastIndexOf(".xml"));
        contentsByOwner.get(id).add(nodePath);
      }
    } finally {
      if (zis != null) {
        zis.reallyClose();
      }
    }
  }

  private Node createJCRPath(Session session, String path) throws RepositoryException {
    String[] ancestors = path.split("/");
    Node current = session.getRootNode();
    for (int i = 0; i < ancestors.length; i++) {
      if (!"".equals(ancestors[i])) {
        if (current.hasNode(ancestors[i])) {
          current = current.getNode(ancestors[i]);
        } else {
          if (log.isInfoEnabled()) {
            log.info("Creating folder: " + ancestors[i] + " in node : " + current.getPath());
          }
          current = current.addNode(ancestors[i], "nt:unstructured");
          session.save();
        }
      }
    }
    return current;

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

  private boolean createSpaceIfNotExists(String tempFolderPath, String forumId, boolean createSpace) throws IOException {
    String spacePrettyName = forumId.replace(Utils.FORUM_SPACE_ID_PREFIX, "");
    Space space = spaceService.getSpaceByPrettyName(spacePrettyName);
    if (space == null && createSpace) {
      FileInputStream spaceMetadataFile = new FileInputStream(tempFolderPath + "/" + SpaceMetadataExportTask.getEntryPath(forumId));
      try {
        // Unmarshall metadata xml file
        XStream xstream = new XStream();
        xstream.alias("metadata", SpaceMetaData.class);
        SpaceMetaData spaceMetaData = (SpaceMetaData) xstream.fromXML(spaceMetadataFile);

        log.info("Automatically create new space: '" + spaceMetaData.getPrettyName() + "'.");
        space = new Space();
        space.setPrettyName(spaceMetaData.getPrettyName());
        space.setDisplayName(spaceMetaData.getDisplayName());
        space.setGroupId("/space/" + spacePrettyName);
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

  private Session getSession(String workspace, RepositoryService repositoryService) throws RepositoryException, LoginException, NoSuchWorkspaceException {
    SessionProvider provider = SessionProvider.createSystemProvider();
    ManageableRepository repository = repositoryService.getCurrentRepository();
    Session session = provider.getSession(workspace, repository);
    return session;
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
