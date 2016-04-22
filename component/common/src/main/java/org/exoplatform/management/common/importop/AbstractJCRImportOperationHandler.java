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
import javax.jcr.nodetype.NodeType;
import javax.transaction.UserTransaction;

import org.exoplatform.commons.utils.ActivityTypeUtils;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.management.common.FileEntry;
import org.exoplatform.management.common.exportop.JCRNodeExportTask;
import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.services.deployment.Utils;
import org.exoplatform.services.transaction.TransactionService;

public abstract class AbstractJCRImportOperationHandler extends AbstractImportOperationHandler {

  protected TemplateService templateService = null;

  protected final boolean importNode(FileEntry fileEntry, String workspace, boolean isCleanPublication) throws Exception {
    File xmlFile = fileEntry.getFile();
    if (xmlFile == null || !xmlFile.exists()) {
      log.warn("Cannot import file" + xmlFile);
      return false;
    }
    FileInputStream fis = new FileInputStream(xmlFile);
    try {
      return importNode(fileEntry.getNodePath(), workspace, fis, fileEntry.getHistoryFile(), isCleanPublication);
    } finally {
      if (fis != null) {
        fis.close();
      }
      if (xmlFile != null) {
        xmlFile.delete();
      }
    }
  }

  protected final boolean importNode(String nodePath, String workspace, InputStream inputStream, File historyFile, boolean isCleanPublication) throws Exception {
    String parentNodePath = nodePath.substring(0, nodePath.lastIndexOf("/"));
    parentNodePath = parentNodePath.replaceAll("//", "/");

    UserTransaction transaction = getTransaction();
    transaction.begin();

    String activityId = null;
    Session session = null;
    try {
      session = getSession(workspace);
      if (session.itemExists(nodePath) && session.getItem(nodePath) instanceof Node) {
        // Delete old node
        log.info("Deleting the node " + workspace + ":" + nodePath);

        Node oldNode = (Node) session.getItem(nodePath);
        if (oldNode.isNodeType("exo:activityInfo") && activityManager != null) {
          activityId = ActivityTypeUtils.getActivityId(oldNode);
        }
        remove(oldNode, session);
      }

      // Import Node from Extracted Zip file
      log.info("Importing the node '" + nodePath + "'");

      // Create the parent path
      createJCRPath(session, parentNodePath);
      if (parentNodePath.isEmpty()) {
        parentNodePath = "/";
      }

      // Import content
      session.importXML(parentNodePath, inputStream, ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
      session.save();

      if (!session.itemExists(nodePath)) {
        nodePath = nodePath.replaceAll("\\[[0-9]*\\]", "");
      }
      Node importedNode = (Node) session.getItem(nodePath);

      if (isCleanPublication) {
        if (importedNode.hasProperty("publication:liveRevision")) {
          // Clean publication information
          cleanPublication(importedNode);
        }
      } else if (historyFile != null) {
        // Import version history
        log.info("Importing history of the node " + nodePath);
        FileInputStream historyFis1 = null, historyFis2 = null;
        try {
          historyFis1 = new FileInputStream(historyFile);
          Map<String, String> mapHistoryValue = Utils.getMapImportHistory(historyFis1);

          historyFis2 = new FileInputStream(historyFile);
          Utils.processImportHistory(importedNode, historyFis2, mapHistoryValue);
        } finally {
          if (historyFis1 != null) {
            historyFis1.close();
          }
          if (historyFis2 != null) {
            historyFis2.close();
          }
        }
      }
      // Delete activity
      if (activityId != null) {
        deleteActivity(activityId);
      }
      session.save();
      transaction.commit();
      // Commit transaction
      return true;
    } catch (Throwable e) {
      transaction.rollback();
      log.error("Error when trying to import node: " + nodePath, e);
      // Revert changes
      if (session != null) {
        session.refresh(false);
      }
      return false;
    }
  }

  protected final void cleanPublication(Node node) throws Exception {
    if (node.hasProperty("publication:currentState")) {
      String state = node.getProperty("publication:currentState").getString();
      // Cleanup only already published nodes
      if (state.equals("published")) {
        log.info("\"" + node.getName() + "\" publication lifecycle has been cleaned up");
        // See in case the content is enrolled for the first time but
        // never
        // published in "source server", if yes, set manually
        // "published" state
        Value[] values = node.getProperty("publication:revisionData").getValues();
        if (values.length < 2) {
          String user = node.getProperty("publication:lastUser").getString();
          node.setProperty("publication:revisionData", new String[] { node.getUUID() + ",published," + user });
        }
        node.setProperty("publication:liveRevision", "");
        node.setProperty("publication:currentState", "published");
        node.save();
      }
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

  private void remove(Node node, Session session) throws Exception {
    if (node.hasNodes() && !isRecursiveDelete(node)) {
      NodeIterator subnodes = node.getNodes();
      while (subnodes.hasNext()) {
        Node subNode = subnodes.nextNode();
        remove(subNode, session);
      }
    }
    log.info("Delete node: " + node.getPath());
    if (node.isNodeType("mix:versionable")) {
      node.removeMixin("mix:versionable");
    }
    node.remove();
  }

  protected final boolean isRecursiveDelete(Node node) throws Exception {
    NodeType nodeType = node.getPrimaryNodeType();
    NodeType[] nodeTypes = node.getMixinNodeTypes();
    boolean recursive = isRecursiveNT(nodeType);
    if (!recursive && nodeTypes != null && nodeTypes.length > 0) {
      int i = 0;
      while (!recursive && i < nodeTypes.length) {
        recursive = isRecursiveNT(nodeTypes[i]);
        i++;
      }
    }
    return recursive;
  }

  private UserTransaction getTransaction() {
    TransactionService txService = (TransactionService) PortalContainer.getInstance().getComponentInstanceOfType(TransactionService.class);
    return txService.getUserTransaction();
  }

}
