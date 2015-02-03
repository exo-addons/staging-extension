package org.exoplatform.management.common;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipInputStream;

import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.transaction.SystemException;

import org.apache.commons.io.FileUtils;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.impl.core.SessionImpl;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.transaction.TransactionService;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.OperationHandler;

public abstract class AbstractOperationHandler implements OperationHandler {

  protected static final Log log = ExoLogger.getLogger(AbstractOperationHandler.class);

  protected static final String[] EMPTY_STRING_ARRAY = new String[0];

  protected static final int ONE_DAY_IN_SECONDS = 86400;
  protected static final long ONE_DAY_IN_MS = 86400000L;

  protected RepositoryService repositoryService = null;
  protected SpaceService spaceService = null;

  // This is used to test on duplicated activities
  protected Set<Long> activitiesByPostTime = new HashSet<Long>();

  protected void increaseCurrentTransactionTimeOut(OperationContext operationContext) {
    TransactionService transactionService = operationContext.getRuntimeContext().getRuntimeComponent(TransactionService.class);
    try {
      transactionService.setTransactionTimeout(86400);
    } catch (SystemException e1) {
      log.warn("Cannot Change Transaction timeout");
    }
  }

  protected static void deleteTempFile(File fileToImport) {
    try {
      if (fileToImport != null && fileToImport.exists()) {
        FileUtils.forceDelete(fileToImport);
      }
    } catch (Exception e) {
      log.warn("Cannot delete temporary file from disk: " + fileToImport.getAbsolutePath() + ". It seems we have an opened InputStream. Anyway, it's not blocker.", e);
    }
  }

  protected final Session getSession(String workspace) throws RepositoryException, LoginException, NoSuchWorkspaceException {
    return getSession(repositoryService, workspace);
  }

  public static final Session getSession(RepositoryService repositoryService, String workspace) throws RepositoryException, LoginException, NoSuchWorkspaceException {
    SessionProvider provider = SessionProvider.createSystemProvider();
    ManageableRepository repository = repositoryService.getCurrentRepository();
    Session session = provider.getSession(workspace, repository);
    if (session instanceof SessionImpl) {
      ((SessionImpl) session).setTimeout(ONE_DAY_IN_MS);
    }
    return session;
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
    public void close() throws IOException {}

    public void reallyClose() throws IOException {
      super.close();
    }
  }
}
