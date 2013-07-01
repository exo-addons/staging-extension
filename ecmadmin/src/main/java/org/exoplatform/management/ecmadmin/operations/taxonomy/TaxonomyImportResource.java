package org.exoplatform.management.ecmadmin.operations.taxonomy;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.jcr.ImportUUIDBehavior;
import javax.jcr.Node;
import javax.jcr.Session;

import org.apache.commons.io.IOUtils;
import org.exoplatform.management.ecmadmin.operations.ECMAdminImportResource;
import org.exoplatform.services.cms.taxonomy.TaxonomyService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.gatein.common.logging.Logger;
import org.gatein.common.logging.LoggerFactory;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.NoResultModel;

import com.thoughtworks.xstream.XStream;

/**
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class TaxonomyImportResource extends ECMAdminImportResource {

  final private static Logger log = LoggerFactory.getLogger(TaxonomyImportResource.class);

  private static Map<String, TaxonomyMetaData> metadataMap = new HashMap<String, TaxonomyMetaData>();
  private static Map<String, File> exportMap = new HashMap<String, File>();

  private TaxonomyService taxonomyService;
  private RepositoryService repositoryService;

  public TaxonomyImportResource() {
    super(null);
  }

  public TaxonomyImportResource(String filePath) {
    super(filePath);
  }

  @Override
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws OperationException {
    // get attributes and attachement inputstream
    super.execute(operationContext, resultHandler);

    metadataMap.clear();
    if (taxonomyService == null) {
      taxonomyService = operationContext.getRuntimeContext().getRuntimeComponent(TaxonomyService.class);
    }
    if (repositoryService == null) {
      repositoryService = operationContext.getRuntimeContext().getRuntimeComponent(RepositoryService.class);
    }

    try {
      ZipInputStream zin = new ZipInputStream(attachmentInputStream);
      ZipEntry ze = null;
      while ((ze = zin.getNextEntry()) != null) {
        if (!ze.getName().startsWith("taxonomy/")) {
          continue;
        }
        if (ze.getName().endsWith("tree.xml")) {
          // Write JCR Content in XML Temp File
          File tempFile = File.createTempFile("jcr", "sysview");
          tempFile.deleteOnExit();
          FileOutputStream fout = new FileOutputStream(tempFile);
          IOUtils.copy(zin, fout);
          zin.closeEntry();
          fout.close();
          String taxonomyName = extractTaxonomyName(ze.getName());
          // Put temp file location in Map
          exportMap.put(taxonomyName, tempFile);
        } else if (ze.getName().endsWith("metadata.xml")) {
          ByteArrayOutputStream fout = new ByteArrayOutputStream();
          IOUtils.copy(zin, fout);
          zin.closeEntry();
          String taxonomyName = extractTaxonomyName(ze.getName());

          XStream xStream = new XStream();
          xStream.alias("metadata", TaxonomyMetaData.class);
          TaxonomyMetaData taxonomyMetaData = (TaxonomyMetaData) xStream.fromXML(fout.toString("UTF-8"));
          metadataMap.put(taxonomyName, taxonomyMetaData);
        }
        zin.closeEntry();
      }
      zin.close();

      for (Entry<String, TaxonomyMetaData> entry : metadataMap.entrySet()) {
        String taxonomyName = entry.getKey();
        TaxonomyMetaData metaData = entry.getValue();
        if (taxonomyService.hasTaxonomyTree(taxonomyName)) {
          if (!replaceExisting) {
            log.info("Ignore existing taxonomy tree '" + taxonomyName + "'.");
            continue;
          } else {
            log.info("Overwrite existing taxonomy tree '" + taxonomyName + "'.");
            taxonomyService.removeTaxonomyTree(taxonomyName);
          }
        }

        SessionProvider sessionProvider = SessionProvider.createSystemProvider();
        Session session = sessionProvider.getSession(metaData.getTaxoTreeWorkspace(), repositoryService.getCurrentRepository());
        int length = metaData.getTaxoTreeHomePath().lastIndexOf("/" + taxonomyName) + 1;
        String absolutePath = metaData.getTaxoTreeHomePath().substring(0, length);
        FileInputStream fis = new FileInputStream(exportMap.get(taxonomyName));
        session.importXML(absolutePath, fis, ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
        session.save();

        // Closes the input stream
        fis.close();

        // Remove temp file
        exportMap.get(taxonomyName).delete();

        Node taxonomyNode = (Node) session.getItem(metaData.getTaxoTreeHomePath());
        taxonomyService.addTaxonomyTree(taxonomyNode);
      }
      resultHandler.completed(NoResultModel.INSTANCE);
    } catch (Throwable exception) {
      throw new OperationException(OperationNames.IMPORT_RESOURCE, "Error while proceeding taxonomy import.", exception);
    }
  }

  private static String extractTaxonomyName(String name) {
    return name.split("/")[1];
  }

}
