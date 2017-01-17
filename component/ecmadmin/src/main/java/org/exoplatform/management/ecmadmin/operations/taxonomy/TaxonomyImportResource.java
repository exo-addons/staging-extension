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
package org.exoplatform.management.ecmadmin.operations.taxonomy;

import com.thoughtworks.xstream.XStream;

import org.apache.commons.io.IOUtils;
import org.exoplatform.management.common.AbstractOperationHandler;
import org.exoplatform.management.ecmadmin.operations.ECMAdminImportResource;
import org.exoplatform.services.cms.taxonomy.TaxonomyService;
import org.exoplatform.services.jcr.RepositoryService;
import org.gatein.common.logging.Logger;
import org.gatein.common.logging.LoggerFactory;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.NoResultModel;

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

/**
 * The Class TaxonomyImportResource.
 *
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class TaxonomyImportResource extends ECMAdminImportResource {

  /** The Constant log. */
  final private static Logger log = LoggerFactory.getLogger(TaxonomyImportResource.class);

  /** The metadata map. */
  private static Map<String, TaxonomyMetaData> metadataMap = new HashMap<String, TaxonomyMetaData>();
  
  /** The export map. */
  private static Map<String, File> exportMap = new HashMap<String, File>();

  /** The taxonomy service. */
  private TaxonomyService taxonomyService;
  
  /** The repository service. */
  private RepositoryService repositoryService;

  /**
   * Instantiates a new taxonomy import resource.
   */
  public TaxonomyImportResource() {
    super(null);
  }

  /**
   * Instantiates a new taxonomy import resource.
   *
   * @param filePath the file path
   */
  public TaxonomyImportResource(String filePath) {
    super(filePath);
  }

  /**
   * {@inheritDoc}
   */
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
      try {
        ZipEntry ze = null;
        while ((ze = zin.getNextEntry()) != null) {
          try {
            if (!ze.getName().startsWith("ecmadmin/taxonomy/")) {
              continue;
            }
            if (ze.getName().endsWith("tree.xml")) {
              // Write JCR Content in XML Temp File
              File tempFile = File.createTempFile("jcr", "sysview");
              tempFile.deleteOnExit();
              FileOutputStream fout = new FileOutputStream(tempFile);
              IOUtils.copy(zin, fout);
              fout.close();
              String taxonomyName = extractTaxonomyName(ze.getName());
              // Put temp file location in Map
              exportMap.put(taxonomyName, tempFile);
            } else if (ze.getName().endsWith("metadata.xml")) {
              ByteArrayOutputStream fout = new ByteArrayOutputStream();
              IOUtils.copy(zin, fout);
              String taxonomyName = extractTaxonomyName(ze.getName());

              XStream xStream = new XStream();
              xStream.alias("metadata", TaxonomyMetaData.class);
              TaxonomyMetaData taxonomyMetaData = (TaxonomyMetaData) xStream.fromXML(fout.toString("UTF-8"));
              metadataMap.put(taxonomyName, taxonomyMetaData);
            }
          } finally {
            zin.closeEntry();
          }
        }
      } finally {
        zin.close();
      }

      for (Entry<String, TaxonomyMetaData> entry : metadataMap.entrySet()) {
        String taxonomyName = entry.getKey();
        TaxonomyMetaData metaData = entry.getValue();
        if (taxonomyService.hasTaxonomyTree(taxonomyName)) {
          if (!replaceExisting) {
            log.info("Ignore existing taxonomy tree '" + taxonomyName + "'");
            continue;
          } else {
            log.info("Overwrite existing taxonomy tree '" + taxonomyName + "'");
            taxonomyService.removeTaxonomyTree(taxonomyName);
          }
        }

        Session session = AbstractOperationHandler.getSession(repositoryService, metaData.getTaxoTreeWorkspace());
        if (session.itemExists(metaData.getTaxoTreeHomePath())) {
          // Delete the nodes defined here without testing on replaceExisting
          // because the nodes here aren't defind in a taxonomy tree
          Node node = (Node) session.getItem(metaData.getTaxoTreeHomePath());
          log.info("Delete old taxonomy node '" + node.getPath() + "'");
          node.remove();

          session.save();
        }

        int length = metaData.getTaxoTreeHomePath().lastIndexOf("/" + taxonomyName) + 1;
        String absolutePath = metaData.getTaxoTreeHomePath().substring(0, length);
        FileInputStream fis = new FileInputStream(exportMap.get(taxonomyName));

        log.info("Import taxonomy tree '" + metaData.getTaxoTreeHomePath() + "' into '" + absolutePath + "'.");

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
      throw new OperationException(OperationNames.IMPORT_RESOURCE, "Error while processing taxonomy import", exception);
    }
  }

  /**
   * Extract taxonomy name.
   *
   * @param name the name
   * @return the string
   */
  private static String extractTaxonomyName(String name) {
    return name.split("/")[2];
  }

}
