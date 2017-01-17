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
package org.exoplatform.management.content.operations.site.contents;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.management.common.exportop.JCRNodeExportTask;
import org.exoplatform.management.common.importop.AbstractJCRImportOperationHandler;
import org.exoplatform.management.content.operations.site.SiteUtil;
import org.exoplatform.services.compress.CompressData;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.model.ExportTask;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

/**
 * The Class SiteContentsVersionHistoryExportTask.
 */
public class SiteContentsVersionHistoryExportTask implements ExportTask {
  
  /** The Constant log. */
  private static final Log log = ExoLogger.getLogger(JCRNodeExportTask.class);

  /** The Constant VERSION_HISTORY_FILE_SUFFIX. */
  public static final String VERSION_HISTORY_FILE_SUFFIX = "_VersionHistory.zip";
  
  /** The Constant ROOT_SQL_QUERY. */
  public static final String ROOT_SQL_QUERY = "select * from mix:versionable order by exo:dateCreated DESC";
  
  /** The Constant VERSION_SQL_QUERY. */
  public static final String VERSION_SQL_QUERY = "select * from mix:versionable where jcr:path like '$0/%' " + "order by exo:dateCreated DESC";

  /** The temp files. */
  private static List<File> tempFiles = new ArrayList<File>();

  /** The repository service. */
  private final RepositoryService repositoryService;
  
  /** The workspace. */
  private final String workspace;
  
  /** The absolute path. */
  private final String absolutePath;
  
  /** The site name. */
  private final String siteName;
  
  /** The recurse. */
  private final boolean recurse;

  /**
   * Instantiates a new site contents version history export task.
   *
   * @param repositoryService the repository service
   * @param workspace the workspace
   * @param siteName the site name
   * @param absolutePath the absolute path
   * @param recurse the recurse
   */
  public SiteContentsVersionHistoryExportTask(RepositoryService repositoryService, String workspace, String siteName, String absolutePath, boolean recurse) {
    this.repositoryService = repositoryService;
    this.workspace = workspace;
    this.siteName = siteName;
    this.absolutePath = absolutePath;
    this.recurse = recurse;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getEntry() {
    return SiteUtil.getSiteContentsBasePath(siteName) + "/" + JCRNodeExportTask.JCR_DATA_SEPARATOR + absolutePath + VERSION_HISTORY_FILE_SUFFIX;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void export(OutputStream outputStream) throws IOException {
    log.info("Export VersionHistory: " + workspace + ":" + absolutePath);

    List<OutputStream> ousList = new ArrayList<OutputStream>();
    List<InputStream> isList = new ArrayList<InputStream>();

    File exportedFile = null;
    File zipFile = null;
    File propertiesFile = getExportedFile("mapping", ".properties");
    OutputStream propertiesBOS = new BufferedOutputStream(new FileOutputStream(propertiesFile));
    InputStream propertiesBIS = new BufferedInputStream(new TempFileInputStream(propertiesFile));
    CompressData zipService = new CompressData();
    Session session = null;
    try {
      Node currentNode = getCurrentNode();
      String sysWsName = repositoryService.getCurrentRepository().getConfiguration().getSystemWorkspaceName();
      session = AbstractJCRImportOperationHandler.getSession(repositoryService, sysWsName);
      if (recurse) {
        // Export version history of sub nodes
        QueryResult queryResult = getQueryResult(currentNode);
        NodeIterator queryIter = queryResult.getNodes();
        while (queryIter.hasNext()) {
          Node node = queryIter.nextNode();
          exportedFile = getExportedFile("data", ".xml");
          OutputStream out = new BufferedOutputStream(new FileOutputStream(exportedFile));
          ousList.add(out);
          InputStream in = new BufferedInputStream(new TempFileInputStream(exportedFile));
          isList.add(in);
          String historyValue = getHistoryValue(node);
          propertiesBOS.write(historyValue.getBytes());
          propertiesBOS.write('\n');
          session.exportDocumentView(node.getVersionHistory().getPath(), out, false, false);
          out.flush();
          zipService.addInputStream(node.getUUID() + ".xml", in);
        }
      }
      if (currentNode.isNodeType(Utils.MIX_VERSIONABLE)) {
        // Export version history of current nodes
        exportedFile = getExportedFile("data", ".xml");
        OutputStream out = new BufferedOutputStream(new FileOutputStream(exportedFile));
        ousList.add(out);
        InputStream in = new BufferedInputStream(new TempFileInputStream(exportedFile));
        isList.add(in);
        String historyValue = getHistoryValue(currentNode);
        propertiesBOS.write(historyValue.getBytes());
        propertiesBOS.write('\n');
        session.exportDocumentView(currentNode.getVersionHistory().getPath(), out, false, false);
        out.flush();
        zipService.addInputStream(currentNode.getUUID() + ".xml", in);
      }
      propertiesBOS.flush();
      zipService.addInputStream("mapping.properties", propertiesBIS);
      zipFile = getExportedFile("data", ".zip");
      InputStream in = new BufferedInputStream(new TempFileInputStream(zipFile));
      isList.add(in);
      OutputStream out = new BufferedOutputStream(new FileOutputStream(zipFile));
      ousList.add(out);
      out.flush();
      zipService.createZip(out);
      IOUtils.copy(in, outputStream);
      outputStream.flush();
    } catch (OutOfMemoryError error) {
      throw new OperationException(OperationNames.EXPORT_RESOURCE, "OutOfMemoryError, Unable to export content from : " + absolutePath, error);
    } catch (Exception exception) {
      throw new OperationException(OperationNames.EXPORT_RESOURCE, "Unable to export content from : " + absolutePath, exception);
    } finally {
      propertiesBOS.close();
      propertiesBIS.close();
      for (InputStream inputStream : isList) {
        if (inputStream != null) {
          inputStream.close();
        }
      }
      for (OutputStream ous : ousList) {
        if (ous != null) {
          ous.close();
        }
      }
      if (session != null) {
        session.logout();
      }
      clearTempFiles();
    }
  }

  /**
   * Delete temp files created by GateIN management operations.
   */
  protected void clearTempFiles() {
    for (File tempFile : tempFiles) {
      if (tempFile != null && tempFile.exists()) {
        tempFile.delete();
      }
    }
  }

  /**
   * Gets the query result.
   *
   * @param currentNode the current node
   * @return the query result
   * @throws Exception the exception
   */
  private QueryResult getQueryResult(Node currentNode) throws Exception {
    QueryManager queryManager = currentNode.getSession().getWorkspace().getQueryManager();
    String queryStatement = "";
    if (currentNode.getPath().equals("/")) {
      queryStatement = ROOT_SQL_QUERY;
    } else {
      queryStatement = StringUtils.replace(VERSION_SQL_QUERY, "$0", currentNode.getPath());
    }
    Query query = queryManager.createQuery(queryStatement, Query.SQL);
    return query.execute();
  }

  /**
   * Gets the current node.
   *
   * @return the current node
   * @throws Exception the exception
   */
  private Node getCurrentNode() throws Exception {
    Session session = null;
    try {
      session = AbstractJCRImportOperationHandler.getSession(repositoryService, workspace);
      return (Node) session.getItem(absolutePath);
    } catch (RepositoryException exception) {
      throw new OperationException(OperationNames.EXPORT_RESOURCE, "Unable to export content from : " + absolutePath, exception);
    }
  }

  /**
   * Gets the history value.
   *
   * @param node the node
   * @return the history value
   * @throws Exception the exception
   */
  private String getHistoryValue(Node node) throws Exception {
    String versionHistory = node.getProperty("jcr:versionHistory").getValue().getString();
    String baseVersion = node.getProperty("jcr:baseVersion").getValue().getString();
    Value[] predecessors = node.getProperty("jcr:predecessors").getValues();
    StringBuilder historyValue = new StringBuilder();
    StringBuilder predecessorsBuilder = new StringBuilder();
    for (Value value : predecessors) {
      if (predecessorsBuilder.length() > 0)
        predecessorsBuilder.append(",");
      predecessorsBuilder.append(value.getString());
    }
    historyValue.append(node.getUUID()).append("=").append(versionHistory).append(";").append(baseVersion).append(";").append(predecessorsBuilder.toString());
    return historyValue.toString();
  }

  /**
   * Create temp file to allow download a big data.
   *
   * @param prefix the prefix
   * @param suffix the suffix
   * @return file
   * @throws IOException Signals that an I/O exception has occurred.
   */
  private static File getExportedFile(String prefix, String suffix) throws IOException {
    File tempFile = File.createTempFile(prefix.concat(UUID.randomUUID().toString()), suffix);
    tempFile.deleteOnExit();
    tempFiles.add(tempFile);
    return tempFile;
  }
}
