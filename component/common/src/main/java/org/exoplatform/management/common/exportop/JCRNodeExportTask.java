package org.exoplatform.management.common.exportop;

import java.io.IOException;
import java.io.OutputStream;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.exoplatform.management.common.importop.AbstractJCRImportOperationHandler;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.model.ExportTask;

/**
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class JCRNodeExportTask implements ExportTask {
  public static final String JCR_DATA_SEPARATOR = "JCR_EXP_DATA";

  private static final Log log = ExoLogger.getLogger(JCRNodeExportTask.class);

  private final RepositoryService repositoryService;
  private final String workspace;
  private final String absolutePath;
  private final String entryPath;
  private final boolean recurse;

  public JCRNodeExportTask(RepositoryService repositoryService, String workspace, String absolutePath, String entryPath, boolean recurse, boolean isPrefix) {
    this.repositoryService = repositoryService;
    this.workspace = workspace;
    if (isPrefix) {
      this.entryPath = entryPath + (entryPath.endsWith("/") ? "" : "/") + JCR_DATA_SEPARATOR + absolutePath + ".xml";
    } else {
      this.entryPath = entryPath;
    }
    this.absolutePath = absolutePath;
    this.recurse = recurse;
  }

  @Override
  public String getEntry() {
    return entryPath;
  }

  @Override
  public void export(OutputStream outputStream) throws IOException {
    Session session = null;
    try {
      log.info("Export: " + workspace + ":" + absolutePath);

      session = AbstractJCRImportOperationHandler.getSession(repositoryService, workspace);
      session.exportDocumentView(absolutePath, outputStream, false, !recurse);
    } catch (RepositoryException exception) {
      throw new OperationException(OperationNames.EXPORT_RESOURCE, "Unable to export content from : " + absolutePath, exception);
    } finally {
      if (session != null) {
        session.logout();
      }
    }
  }
}
