package org.exoplatform.management.ecmadmin.operations.taxonomy;

import java.io.IOException;
import java.io.OutputStream;

import javax.jcr.Session;

import org.exoplatform.management.common.AbstractOperationHandler;
import org.exoplatform.services.jcr.RepositoryService;
import org.gatein.management.api.operation.model.ExportTask;

/**
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class TaxonomyTreeExportTask implements ExportTask {

  private TaxonomyMetaData metaData = null;
  private String path = null;
  private RepositoryService repositoryService = null;

  public TaxonomyTreeExportTask(RepositoryService repositoryService, TaxonomyMetaData metaData, String path) {
    this.metaData = metaData;
    this.path = path;
    this.repositoryService = repositoryService;
  }

  @Override
  public String getEntry() {
    return path + "/tree.xml";
  }

  @Override
  public void export(OutputStream outputStream) throws IOException {
    try {
      Session session = AbstractOperationHandler.getSession(repositoryService, metaData.getTaxoTreeWorkspace());

      // Workaround: use docview instead of sysview
      session.exportDocumentView(metaData.getTaxoTreeHomePath(), outputStream, false, false);
    } catch (Exception exception) {
      exception.printStackTrace();
      throw new RuntimeException(exception);
    }
  }
}