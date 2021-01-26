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

import org.exoplatform.commons.utils.ActivityTypeUtils;
import org.exoplatform.management.common.FileEntry;
import org.exoplatform.management.common.exportop.JCRNodeExportTask;
import org.exoplatform.services.cache.CacheService;
import org.exoplatform.services.cache.ExoCache;
import org.exoplatform.services.ecm.publication.PublicationService;
import org.exoplatform.services.wcm.publication.WCMPublicationService;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.ImportUUIDBehavior;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;

/**
 * The Class AbstractJCRImportOperationHandler.
 */
public abstract class AbstractJCRImportOperationHandler extends AbstractImportOperationHandler {

  /** The publication service. */
  protected PublicationService publicationService;
  
  /** The wcm publication service. */
  protected WCMPublicationService wcmPublicationService;

  /** The is NT recursive map. */
  private Map<String, Boolean> isNTRecursiveMap = new HashMap<String, Boolean>();

  /**
   * Import node.
   *
   * @param fileEntry the file entry
   * @param workspace the workspace
   * @param isCleanPublication the is clean publication
   * @return true, if successful
   * @throws Exception the exception
   */
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

  /**
   * Import node.
   *
   * @param nodePath the node path
   * @param workspace the workspace
   * @param inputStream the input stream
   * @param historyFile the history file
   * @param isCleanPublication the is clean publication
   * @return true, if successful
   * @throws Exception the exception
   */
  protected final boolean importNode(String nodePath, String workspace, InputStream inputStream, File historyFile, boolean isCleanPublication) throws Exception {
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

        remove(oldNode, session);
        session.save();
        session.refresh(false);
      }
    } catch (Exception e) {
      log.error("Error when trying to find and delete the node: '" + nodePath + "'. Ignore this node and continue.", e);
      return false;
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
      return true;
    } catch (Exception e) {
      log.error("Error when trying to import node: " + nodePath, e);
      // Revert changes
      session.refresh(false);
      return false;
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

  /**
   * Removes the.
   *
   * @param node the node
   * @param session the session
   * @throws Exception the exception
   */
  private void remove(Node node, Session session) throws Exception {
    if (node.hasNodes() && !isRecursiveDelete(node)) {
      NodeIterator subnodes = node.getNodes();
      while (subnodes.hasNext()) {
        Node subNode = subnodes.nextNode();
        remove(subNode, session);
      }
    }
    log.info("Delete sub node" + node.getPath());
    node.remove();
    session.save();
  }

  /**
   * Checks if is recursive delete.
   *
   * @param node the node
   * @return true, if is recursive delete
   * @throws Exception the exception
   */
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

  /**
   * Checks if is recursive NT.
   *
   * @param nodeType the node type
   * @return true, if is recursive NT
   * @throws Exception the exception
   */
  protected final boolean isRecursiveNT(NodeType nodeType) throws Exception {
    if (nodeType.getName().equals("exo:actionStorage")) {
      return true;
    }
    if (!isNTRecursiveMap.containsKey(nodeType.getName())) {
      boolean hasMandatoryChild = false;
      NodeDefinition[] nodeDefinitions = nodeType.getChildNodeDefinitions();
      if (nodeDefinitions != null) {
        int i = 0;
        while (!hasMandatoryChild && i < nodeDefinitions.length) {
          hasMandatoryChild = nodeDefinitions[i].isMandatory();
          i++;
        }
      }
      boolean recursive = hasMandatoryChild;
      isNTRecursiveMap.put(nodeType.getName(), recursive);
    }
    return isNTRecursiveMap.get(nodeType.getName());
  }

  /**
   * Clean publication.
   *
   * @param parentPath the parent path
   * @param session the session
   * @throws Exception the exception
   */
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

  /**
   * Clean publication.
   *
   * @param node the node
   * @throws Exception the exception
   */
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
      node.setProperty("publication:liveRevision", (javax.jcr.Value) null);
      node.setProperty("publication:currentState", "published");
      node.getSession().save();

      if (publicationService.isNodeEnrolledInLifecycle(node))
        publicationService.unsubcribeLifecycle(node);
      wcmPublicationService.updateLifecyleOnChangeContent(node, "default", "__system", "published");
      node.save();
    }
  }

  /**
   * Creates the JCR path.
   *
   * @param session the session
   * @param path the path
   * @return the node
   * @throws RepositoryException the repository exception
   */
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

  /**
   * Gets the node path.
   *
   * @param filePath the file path
   * @return the node path
   */
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

  /**
   * Clear caches.
   *
   * @param cacheService the cache service
   * @param namePattern the name pattern
   */
  @SuppressWarnings("rawtypes")
  public void clearCaches(CacheService cacheService, String namePattern) {
    for (Object o : cacheService.getAllCacheInstances()) {
      try {
        ExoCache exoCache = (ExoCache) o;
        if(exoCache.getName().contains(namePattern)) {
          exoCache.clearCache();
        }
      } catch (Exception e) {
        if (log.isTraceEnabled()) {
          log.trace("An exception occurred: " + e.getMessage());
        }
      }
    }
  }

}
