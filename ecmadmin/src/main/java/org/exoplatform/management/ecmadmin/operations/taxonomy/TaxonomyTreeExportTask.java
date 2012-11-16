package org.exoplatform.management.ecmadmin.operations.taxonomy;

import java.io.IOException;
import java.io.OutputStream;

import javax.jcr.Session;

import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
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
    SessionProvider sessionProvider = SessionProvider.createSystemProvider();
    try {
      Session session = sessionProvider.getSession(metaData.getTaxoTreeWorkspace(), repositoryService.getCurrentRepository());
      session.exportSystemView(metaData.getTaxoTreeHomePath(), outputStream, false, false);
    } catch (Exception exception) {
      exception.printStackTrace();
      throw new RuntimeException(exception);
    }
  }
}