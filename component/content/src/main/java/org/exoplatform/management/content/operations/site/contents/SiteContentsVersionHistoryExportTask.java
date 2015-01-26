package org.exoplatform.management.content.operations.site.contents;

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

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.management.common.AbstractJCROperationHandler;
import org.exoplatform.management.common.JCRNodeExportTask;
import org.exoplatform.management.content.operations.site.SiteUtil;
import org.exoplatform.services.compress.CompressData;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.model.ExportTask;

public class SiteContentsVersionHistoryExportTask implements ExportTask {
  private static final Log log = ExoLogger.getLogger(JCRNodeExportTask.class);

  public static final String VERSION_HISTORY_FILE_SUFFIX = "_VersionHistory.zip";
  public static final String ROOT_SQL_QUERY = "select * from mix:versionable order by exo:dateCreated DESC";
  public static final String VERSION_SQL_QUERY = "select * from mix:versionable where jcr:path like '$0/%' " + "order by exo:dateCreated DESC";

  private static List<File> tempFiles = new ArrayList<File>();

  private final RepositoryService repositoryService;
  private final String workspace;
  private final String absolutePath;
  private final String siteName;
  private final boolean recurse;

  public SiteContentsVersionHistoryExportTask(RepositoryService repositoryService, String workspace, String siteName, String absolutePath, boolean recurse) {
    this.repositoryService = repositoryService;
    this.workspace = workspace;
    this.siteName = siteName;
    this.absolutePath = absolutePath;
    this.recurse = recurse;
  }

  @Override
  public String getEntry() {
    return SiteUtil.getSiteContentsBasePath(siteName) + absolutePath + VERSION_HISTORY_FILE_SUFFIX;
  }

  @Override
  public void export(OutputStream outputStream) throws IOException {
    log.info("Export VersionHistory: " + workspace + ":" + absolutePath);

    OutputStream out = null;
    InputStream in = null;
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
      session = AbstractJCROperationHandler.getSession(repositoryService, sysWsName);
      if (recurse) {
        // Export version history of sub nodes
        QueryResult queryResult = getQueryResult(currentNode);
        NodeIterator queryIter = queryResult.getNodes();
        while (queryIter.hasNext()) {
          Node node = queryIter.nextNode();
          exportedFile = getExportedFile("data", ".xml");
          out = new BufferedOutputStream(new FileOutputStream(exportedFile));
          in = new BufferedInputStream(new TempFileInputStream(exportedFile));
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
        out = new BufferedOutputStream(new FileOutputStream(exportedFile));
        in = new BufferedInputStream(new TempFileInputStream(exportedFile));
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
      in = new BufferedInputStream(new TempFileInputStream(zipFile));
      out = new BufferedOutputStream(new FileOutputStream(zipFile));
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
      if (out != null) {
        out.close();
      }
      if (in != null) {
        in.close();
      }
      if (session != null) {
        session.logout();
      }
      clearTempFiles();
    }
  }

  /**
   * Delete temp files created by GateIN management operations
   * 
   */
  protected void clearTempFiles() {
    for (File tempFile : tempFiles) {
      if (tempFile != null && tempFile.exists()) {
        tempFile.delete();
      }
    }
  }

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

  private Node getCurrentNode() throws Exception {
    Session session = null;
    try {
      session = AbstractJCROperationHandler.getSession(repositoryService, workspace);
      return (Node) session.getItem(absolutePath);
    } catch (RepositoryException exception) {
      throw new OperationException(OperationNames.EXPORT_RESOURCE, "Unable to export content from : " + absolutePath, exception);
    }
  }

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
   * Create temp file to allow download a big data
   * 
   * @return file
   */
  private static File getExportedFile(String prefix, String suffix) throws IOException {
    File tempFile = File.createTempFile(prefix.concat(UUID.randomUUID().toString()), suffix);
    tempFile.deleteOnExit();
    tempFiles.add(tempFile);
    return tempFile;
  }
}
