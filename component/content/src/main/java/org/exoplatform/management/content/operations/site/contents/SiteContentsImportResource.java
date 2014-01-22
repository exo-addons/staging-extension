package org.exoplatform.management.content.operations.site.contents;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.jcr.ImportUUIDBehavior;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.exoplatform.management.content.operations.site.SiteUtil;
import org.exoplatform.management.content.operations.site.seo.SiteSEOExportTask;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.seo.PageMetadataModel;
import org.exoplatform.services.seo.SEOService;
import org.gatein.common.logging.Logger;
import org.gatein.common.logging.LoggerFactory;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.operation.OperationAttachment;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.OperationHandler;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.NoResultModel;

import com.thoughtworks.xstream.XStream;

/**
 * @author <a href="mailto:soren.schmidt@exoplatform.com">Soren Schmidt</a>
 * @author <a href="mailto:thomas.delhomenie@exoplatform.com">Thomas
 *         Delhoménie</a>
 * @author <a href="mailto:boubaker.khanfir@exoplatform.com">Boubaker
 *         Khanfir</a>
 * @version $Revision$ usage: ssh -p 2000 john@localhost mgmt connect ls cd
 *          content import -f /acmeTest.zip
 */
public class SiteContentsImportResource implements OperationHandler {

  final private static Logger log = LoggerFactory.getLogger(SiteContentsImportResource.class);

  private SEOService seoService = null;
  private String operationName = null;

  private String importedSiteName = null;
  private String filePath = null;

  public SiteContentsImportResource() {
  }

  public SiteContentsImportResource(String siteName, String filePath) {
    this.importedSiteName = siteName;
    this.filePath = filePath;
  }

  @Override
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws OperationException {

    if (importedSiteName == null) {
      importedSiteName = operationContext.getAddress().resolvePathTemplate("site-name");
    }
    if (seoService == null) {
      seoService = operationContext.getRuntimeContext().getRuntimeComponent(SEOService.class);
    }

    // "uuidBehavior" attribute
    int uuidBehaviorValue = extractUuidBehavior(operationContext.getAttributes().getValue("uuidBehavior"));
    List<String> filters = operationContext.getAttributes().getValues("filter");

    boolean isCleanPublication = filters.contains("cleanPublication:true");

    InputStream attachmentInputStream = null;
    if (filePath != null) {
      try {
        attachmentInputStream = new FileInputStream(filePath);
      } catch (FileNotFoundException exception) {
        throw new OperationException(OperationNames.IMPORT_RESOURCE, "File not found to import.");
      }
    } else {
      OperationAttachment attachment = operationContext.getAttachment(false);
      if (attachment == null) {
        throw new OperationException(OperationNames.IMPORT_RESOURCE, "No attachment available for Site Content import.");
      }

      attachmentInputStream = attachment.getStream();
      if (attachmentInputStream == null) {
        throw new OperationException(OperationNames.IMPORT_RESOURCE, "No data stream available for Site Content import.");
      }
    }

    Map<String, SiteData> sitesData = null;
    try {
      // extract data from zip
      sitesData = extractDataFromZip(attachmentInputStream);

      // import data of each site
      for (String site : sitesData.keySet()) {
        SiteData siteData = sitesData.get(site);
        if (siteData.getNodeExportFiles() == null || siteData.getNodeExportFiles().isEmpty()) {
          log.info("No data available to import for site: " + site);
          continue;
        }

        Map<String, String> metaDataOptions = siteData.getSiteMetadata().getOptions();
        String workspace = metaDataOptions.get("site-workspace");
        log.info("Reading metadata options for import: workspace: " + workspace);

        try {
          importContentNodes(operationContext, siteData.getSiteMetadata(), siteData.getNodeExportFiles(), siteData.getNodeExportHistoryFiles(), workspace, uuidBehaviorValue, isCleanPublication);
          log.info("Content import has been finished");
        } catch (Exception e) {
          throw new OperationException(operationName, "Unable to create import task", e);
        }
      }
    } finally {
      if (sitesData != null && !sitesData.isEmpty()) {
        // import data of each site
        for (String site : sitesData.keySet()) {
          SiteData siteData = sitesData.get(site);
          if (siteData.getNodeExportHistoryFiles() != null && !siteData.getNodeExportHistoryFiles().isEmpty()) {
            for (String tempAbsPath : siteData.getNodeExportHistoryFiles().values()) {
              File file = new File(tempAbsPath);
              if (file.exists() && !file.isDirectory()) {
                try {
                  file.delete();
                } catch (Exception e) {
                  // Nothing to do, deleteOnExit is called
                }
              }
            }
          }
        }
      }
    }
    resultHandler.completed(NoResultModel.INSTANCE);
  }

  /**
   * Import data of a site
   * 
   * @param operationContext
   * @param metaData
   * @param nodes
   * @param historyFiles
   * @param workspace
   * @param uuidBehaviorValue
   * @param isCleanPublication
   * @throws Exception
   */
  private void importContentNodes(OperationContext operationContext, SiteMetaData metaData, Map<String, String> nodes, Map<String, String> historyFiles, String workspace, int uuidBehaviorValue,
      boolean isCleanPublication) throws Exception {

    RepositoryService repositoryService = operationContext.getRuntimeContext().getRuntimeComponent(RepositoryService.class);
    Session session = repositoryService.getCurrentRepository().getSystemSession(workspace);

    for (Iterator<String> it = nodes.keySet().iterator(); it.hasNext();) {
      String name = (String) it.next();
      String path = metaData.getExportedFiles().get(name);

      String targetNodePath = path + name.substring(name.lastIndexOf("/"), name.lastIndexOf('.'));
      if(targetNodePath.contains("//")) {
        targetNodePath = targetNodePath.replaceAll("//", "/");
      }

      log.info("Deleting the node " + workspace + ":" + targetNodePath);

      try {
        if (session.itemExists(targetNodePath)) {
          Node oldNode = (Node) session.getItem(targetNodePath);
          oldNode.remove();
        }
      } catch (Exception e) {
        log.error("Error when trying to find and delete the node: " + targetNodePath, e);
        continue;
      }

      if (log.isInfoEnabled()) {
        log.info("Importing the node " + name + " to the node " + path);
      }

      // Create the parent path
      Node currentNode = createJCRPath(session, path);

      try {
        session.importXML(path, new ByteArrayInputStream(nodes.get(name).getBytes("UTF-8")), uuidBehaviorValue);
        session.save();

        if (historyFiles.containsKey(name)) {
          log.info("Importing history of the node " + path);
          String historyFilePath = historyFiles.get(name);

          Map<String, String> mapHistoryValue = org.exoplatform.services.cms.impl.Utils.getMapImportHistory(new FileInputStream(historyFilePath));
          org.exoplatform.services.cms.impl.Utils.processImportHistory(currentNode, new FileInputStream(historyFilePath), mapHistoryValue);
        } else if (isCleanPublication) {
          // Clean publication information
          cleanPublication(path, session);
        }
      } catch (Exception e) {
        // Revert changes
        session.refresh(false);
        throw e;
      }

      session.save();
    }
    // save at the end
    // TODO Can there be too much data? Big memory consumption...
    // TODO Transaction instead of a simple session?
    session.save();
  }

  private void cleanPublication(String path, Session session) throws Exception {
    QueryManager manager = session.getWorkspace().getQueryManager();
    String statement = "select * from nt:base where jcr:path LIKE '" + path + "/%' and publication:liveRevision IS NOT NULL";
    Query query = manager.createQuery(statement.toString(), Query.SQL);
    NodeIterator iter = query.execute().getNodes();
    while (iter.hasNext()) {
      Node node = iter.nextNode();
      if (node.hasProperty("publication:liveRevision") && node.hasProperty("publication:currentState")) {
        log.info("\"" + node.getName() + "\" publication lifecycle has been cleaned up");
        node.setProperty("publication:liveRevision", "");
        node.setProperty("publication:currentState", "published");
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
        }
      }
    }
    return current;

  }

  /**
   * JCR Import UUID Behavior enum
   * 
   * @author <a href="mailto:thomas.delhomenie@exoplatform.com">Thomas
   *         Delhoménie</a>
   */
  private enum ImportBehavior {
    THROW(ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW), //
    REMOVE(ImportUUIDBehavior.IMPORT_UUID_COLLISION_REMOVE_EXISTING), //
    REPLACE(ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING), //
    NEW(ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);

    final private int behavior;

    ImportBehavior(int behavior) {
      this.behavior = behavior;
    }

    public int getBehavior() {
      return this.behavior;
    }
  }

  /**
   * Convert UUID behavior string into int
   * 
   * @param uuidBehavior
   * @return
   */
  private int extractUuidBehavior(String uuidBehavior) {

    int uuidBehaviorValue;
    if (!StringUtils.isEmpty(uuidBehavior)) {
      try {
        uuidBehaviorValue = ImportBehavior.valueOf(uuidBehavior).getBehavior();
      } catch (Exception e) {
        throw new OperationException(this.operationName, "Unknown uuidBehavior " + uuidBehavior);
      }
    } else {
      uuidBehaviorValue = ImportBehavior.NEW.getBehavior();
    }

    return uuidBehaviorValue;
  }

  /**
   * Extract data from zip
   * 
   * @param attachment
   * @return
   */
  private Map<String, SiteData> extractDataFromZip(InputStream attachmentInputStream) {

    Map<String, SiteData> sitesData = new HashMap<String, SiteData>();

    final NonCloseableZipInputStream zis = new NonCloseableZipInputStream(attachmentInputStream);

    try {
      ZipEntry entry;
      while ((entry = zis.getNextEntry()) != null) {
        // Skip directories
        if (entry.isDirectory()) {
          continue;
        }
        String filePath = entry.getName();
        // Skip empty entries (this allows empty zip files to not cause
        // exceptions).
        if (filePath.equals("") || !filePath.startsWith(SiteUtil.getSitesBasePath() + "/")) {
          continue;
        }

        String siteName = extractSiteNameFromPath(filePath);

        // if we are in a site (for example /content/sites/acme), take only
        // the files relative to this site
        if (importedSiteName != null && !importedSiteName.equals(siteName)) {
          continue;
        }

        // metadata file ?
        if (filePath.endsWith(SiteMetaDataExportTask.FILENAME)) {
          // Unmarshall metadata xml file
          XStream xstream = new XStream();
          xstream.alias("metadata", SiteMetaData.class);
          InputStreamReader isr = new InputStreamReader(zis, "UTF-8");
          SiteMetaData siteMetadata = (SiteMetaData) xstream.fromXML(isr);

          // Save unmarshalled metadata
          SiteData siteData = sitesData.get(siteName);
          if (siteData == null) {
            siteData = new SiteData();
          }
          siteData.setSiteMetadata(siteMetadata);
          sitesData.put(siteName, siteData);
        }
        // seo file ?
        else if (filePath.endsWith(SiteSEOExportTask.FILENAME)) {
          String lang = filePath.substring(filePath.lastIndexOf("/") + 1, filePath.indexOf(SiteSEOExportTask.FILENAME));
          XStream xStream = new XStream();
          xStream.alias("seo", List.class);
          InputStreamReader isr = new InputStreamReader(zis, "UTF-8");

          @SuppressWarnings("unchecked")
          List<PageMetadataModel> models = (List<PageMetadataModel>) xStream.fromXML(isr);
          if (models != null && !models.isEmpty()) {
            for (PageMetadataModel pageMetadataModel : models) {
              seoService.storeMetadata(pageMetadataModel, siteName, false, lang);
            }
          }
        }
        // sysview file ?
        else if (filePath.endsWith(".xml")) {
          // Unmarshall sysview xml file to String
          log.info("Collecting the node " + filePath);
          String nodeContent = IOUtils.toString(zis, "UTF-8");

          // Save unmarshalled sysview
          SiteData siteData = sitesData.get(siteName);
          if (siteData == null) {
            siteData = new SiteData();
          }
          siteData.getNodeExportFiles().put(filePath, nodeContent);
          sitesData.put(siteName, siteData);
        } else if (filePath.endsWith(SiteContentsVersionHistoryExportTask.VERSION_HISTORY_FILE_SUFFIX)) {
          // Put Version History file in temp folder
          File tempFile = File.createTempFile("JCR", "-VersionHistory.zip");
          tempFile.deleteOnExit();
          FileOutputStream outputStream = new FileOutputStream(tempFile);
          IOUtils.copy(zis, outputStream);
          outputStream.flush();
          outputStream.close();
          SiteData siteData = sitesData.get(siteName);
          if (siteData == null) {
            siteData = new SiteData();
          }
          siteData.getNodeExportHistoryFiles().put(filePath.replace(SiteContentsVersionHistoryExportTask.VERSION_HISTORY_FILE_SUFFIX, ".xml"), tempFile.getAbsolutePath());
        }
      }

      zis.reallyClose();
    } catch (Exception e) {
      throw new OperationException(this.operationName, "Exception when reading the underlying data stream from import.", e);
    }

    return sitesData;

  }

  /**
   * Extract site name from the file path
   * 
   * @param path
   *          The path of the file
   * @return The site name
   */
  private String extractSiteNameFromPath(String path) {
    String siteName = null;

    int beginIndex = SiteUtil.getSitesBasePath().length() + 1;
    siteName = path.substring(beginIndex, path.indexOf("/", beginIndex));

    return siteName;
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
    public void close() throws IOException {
    }

    private void reallyClose() throws IOException {
      super.close();
    }
  }
}
