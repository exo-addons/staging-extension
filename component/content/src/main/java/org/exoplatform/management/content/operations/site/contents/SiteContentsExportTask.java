package org.exoplatform.management.content.operations.site.contents;

import java.io.IOException;
import java.io.OutputStream;

import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.exoplatform.management.content.operations.site.SiteUtil;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.model.ExportTask;

/**
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class SiteContentsExportTask implements ExportTask {
  private static final Log log = ExoLogger.getLogger(SiteContentsExportTask.class);

  private final RepositoryService repositoryService;
  private final String workspace;
  private final String absolutePath;
  private final String siteName;
  private final boolean recurse;

  public SiteContentsExportTask(RepositoryService repositoryService, String workspace, String siteName, String absolutePath, boolean recurse) {
    this.repositoryService = repositoryService;
    this.workspace = workspace;
    this.siteName = siteName;
    this.absolutePath = absolutePath;
    this.recurse = recurse;
  }

  @Override
  public String getEntry() {
    return SiteUtil.getSiteContentsBasePath(siteName) + absolutePath + ".xml";
  }

  @Override
  public void export(OutputStream outputStream) throws IOException {
    Session session = null;
    try {
      log.info("Export: " + workspace + ":" + absolutePath);

      session = getSession(workspace);
      session.exportDocumentView(absolutePath, outputStream, false, !recurse);
    } catch (RepositoryException exception) {
      throw new OperationException(OperationNames.EXPORT_RESOURCE, "Unable to export content from : " + absolutePath, exception);
    } finally {
      if (session != null) {
        session.logout();
      }
    }
  }

  private Session getSession(String workspace) throws RepositoryException, LoginException, NoSuchWorkspaceException {
    SessionProvider provider = SessionProvider.createSystemProvider();
    ManageableRepository repository = repositoryService.getCurrentRepository();
    Session session = provider.getSession(workspace, repository);
    return session;
  }

}
