package org.exoplatform.management.common.importop;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Map;

import javax.jcr.ImportUUIDBehavior;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;

import org.exoplatform.commons.utils.ActivityTypeUtils;
import org.exoplatform.management.common.FileEntry;
import org.exoplatform.management.common.exportop.JCRNodeExportTask;
import org.exoplatform.services.ecm.publication.PublicationService;
import org.exoplatform.services.wcm.publication.WCMPublicationService;

public abstract class AbstractJCRImportOperationHandler extends AbstractImportOperationHandler {

  protected PublicationService publicationService;
  protected WCMPublicationService wcmPublicationService;

  protected final void importNode(FileEntry fileEntry, String workspace, boolean isCleanPublication) throws Exception {
    File xmlFile = fileEntry.getFile();
    if (xmlFile == null || !xmlFile.exists()) {
      log.warn("Cannot import file" + xmlFile);
      return;
    }
    FileInputStream fis = new FileInputStream(xmlFile);
    try {
      importNode(fileEntry.getNodePath(), workspace, fis, fileEntry.getHistoryFile(), isCleanPublication);
    } finally {
      if (fis != null) {
        fis.close();
      }
      if (xmlFile != null) {
        xmlFile.delete();
      }
    }
  }

  protected final void importNode(String nodePath, String workspace, InputStream inputStream, File historyFile, boolean isCleanPublication) throws Exception {
    String parentNodePath = nodePath.substring(0, nodePath.lastIndexOf("/"));
    parentNodePath = parentNodePath.replaceAll("//", "/");

    // Delete old node
    Session session = getSession(workspace);
    try {
      if (session.itemExists(nodePath) && session.getItem(nodePath) instanceof Node) {
        log.info("Deleting the node " + workspace + ":" + nodePath);

        Node oldNode = (Node) session.getItem(nodePath);
        if (oldNode.isNodeType("exo:activityInfo") && activityManager != null) {
          String activityId = ActivityTypeUtils.getActivityId(oldNode);
          deleteActivity(activityId);
        }

        oldNode.remove();
        session.save();
        session.refresh(false);
      }
    } catch (Exception e) {
      log.error("Error when trying to find and delete the node: '" + nodePath + "'. Ignore this node and continue.", e);
      return;
    } finally {
      if (session != null) {
        session.logout();
      }
    }

    // Import Node from Extracted Zip file
    session = getSession(workspace);
    FileInputStream historyFis1 = null, historyFis2 = null;
    try {
      log.info("Importing the node '" + nodePath + "'");

      // Create the parent path
      Node currentNode = createJCRPath(session, parentNodePath);

      if (parentNodePath.isEmpty()) {
        parentNodePath = "/";
      }
      session.refresh(false);
      session.importXML(parentNodePath, inputStream, ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
      session.save();

      if (isCleanPublication) {
        // Clean publication information
        cleanPublication(parentNodePath, session);
      } else if (historyFile != null) {
        log.info("Importing history of the node " + nodePath);
        historyFis1 = new FileInputStream(historyFile);
        Map<String, String> mapHistoryValue = org.exoplatform.services.cms.impl.Utils.getMapImportHistory(historyFis1);

        historyFis2 = new FileInputStream(historyFile);
        org.exoplatform.services.cms.impl.Utils.processImportHistory(currentNode, historyFis2, mapHistoryValue);
      }
    } catch (Exception e) {
      log.error("Error when trying to import node: " + nodePath, e);
      // Revert changes
      session.refresh(false);
    } finally {
      if (session != null) {
        session.logout();
      }
      if (historyFis1 != null) {
        historyFis1.close();
      }
      if (historyFis2 != null) {
        historyFis2.close();
      }
    }
  }

  protected final void cleanPublication(String parentPath, Session session) throws Exception {
    QueryManager manager = session.getWorkspace().getQueryManager();
    String statement = "select * from nt:base where jcr:path LIKE '" + parentPath + "/%' and publication:liveRevision IS NOT NULL";
    Query query = manager.createQuery(statement.toString(), Query.SQL);
    NodeIterator iter = query.execute().getNodes();
    while (iter.hasNext()) {
      Node node = iter.nextNode();
      cleanPublication(node);
    }
    if (session.itemExists(parentPath)) {
      Node node = (Node) session.getItem(parentPath);
      cleanPublication(node);
    }
  }

  protected final void cleanPublication(Node node) throws Exception {
    if (node.hasProperty("publication:currentState")) {
      log.info("\"" + node.getName() + "\" publication lifecycle has been cleaned up");
      // See in case the content is enrolled for the first time but never
      // published in "source server", if yes, set manually "published" state
      if (node.hasProperty("publication:revisionData")) {
        Value[] values = node.getProperty("publication:revisionData").getValues();
        if (values.length < 2) {
          String user = node.hasProperty("publication:lastUser") ? node.getProperty("publication:lastUser").getString() : null;
          node.setProperty("publication:revisionData", new String[] { node.getUUID() + ",published," + user });
        }
      }
      node.setProperty("publication:liveRevision", "");
      node.setProperty("publication:currentState", "published");
      node.getSession().save();

      if (publicationService.isNodeEnrolledInLifecycle(node))
        publicationService.unsubcribeLifecycle(node);
      wcmPublicationService.updateLifecyleOnChangeContent(node, "default", "__system", "published");
      node.save();
    }
  }

  protected final Node createJCRPath(Session session, String path) throws RepositoryException {
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

  public String getNodePath(String filePath) {
    String[] fileParts = filePath.split(JCRNodeExportTask.JCR_DATA_SEPARATOR);
    if (fileParts.length != 2) {
      log.error("Cannot parse file: " + filePath);
      return null;
    }
    String nodePath = fileParts[1].trim().replaceFirst(".xml$", "");
    if (!nodePath.startsWith("/")) {
      nodePath = "/" + nodePath;
    }
    return nodePath;
  }
}
