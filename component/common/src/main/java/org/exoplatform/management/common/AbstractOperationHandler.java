package org.exoplatform.management.common;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipInputStream;

import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.transaction.SystemException;

import org.apache.commons.io.FileUtils;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.RootContainer;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.impl.core.SessionImpl;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.transaction.TransactionService;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.identity.provider.SpaceIdentityProvider;
import org.exoplatform.social.core.space.SpaceUtils;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.social.core.storage.api.IdentityStorage;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.OperationHandler;

public abstract class AbstractOperationHandler implements OperationHandler {

  public static final String POLL_ACTIVITY_TYPE = "ks-poll:spaces";
  public static final String FORUM_ACTIVITY_TYPE = "ks-forum:spaces";
  public static final String CALENDAR_ACTIVITY_TYPE = "cs-calendar:spaces";
  public static final String ANSWER_ACTIVITY_TYPE = "ks-answer:spaces";
  public static final String WIKI_ACTIVITY_TYPE = "ks-wiki:spaces";
  public static final String CONTENT_ACTIVITY_TYPE = "contents:spaces";
  public static final String FILE_ACTIVITY_TYPE = "files:spaces";

  protected static final Log log = ExoLogger.getLogger(AbstractOperationHandler.class);

  protected static final String[] EMPTY_STRING_ARRAY = new String[0];

  protected static final int ONE_DAY_IN_SECONDS = 86400;
  protected static final long ONE_DAY_IN_MS = 86400000L;

  protected RepositoryService repositoryService = null;
  protected IdentityStorage identityStorage = null;
  protected SpaceService spaceService = null;

  // This is used to test on duplicated activities
  protected Set<Long> activitiesByPostTime = new HashSet<Long>();

  public static void increaseCurrentTransactionTimeOut(OperationContext operationContext) {
    TransactionService transactionService = operationContext.getRuntimeContext().getRuntimeComponent(TransactionService.class);
    increaseCurrentTransactionTimeOut(transactionService);
  }

  public static void increaseCurrentTransactionTimeOut(PortalContainer portalContainer) {
    TransactionService transactionService = (TransactionService) portalContainer.getComponentInstanceOfType(TransactionService.class);
    increaseCurrentTransactionTimeOut(transactionService);
  }

  public static void increaseCurrentTransactionTimeOut(TransactionService transactionService) {
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
      if (log.isDebugEnabled()) {
        log.debug("Cannot delete temporary file from disk: " + fileToImport.getAbsolutePath() + ".Not blocker.", e);
      }
      fileToImport.deleteOnExit();
    }
  }

  protected final Session getSession(String workspace) throws RepositoryException, LoginException, NoSuchWorkspaceException {
    return getSession(repositoryService, workspace);
  }

  public static final Session getSession(RepositoryService repositoryService, String workspace) throws RepositoryException, LoginException, NoSuchWorkspaceException {
    SessionProvider provider = SessionProvider.createSystemProvider();
    ManageableRepository repository = repositoryService.getCurrentRepository();
    if (workspace == null) {
      workspace = repository.getConfiguration().getDefaultWorkspaceName();
    }
    Session session = provider.getSession(workspace, repository);
    if (session instanceof SessionImpl) {
      ((SessionImpl) session).setTimeout(ONE_DAY_IN_MS);
    }
    return session;
  }

  protected final Identity getIdentity(String id) {
    try {
      Identity identity = identityStorage.findIdentity(OrganizationIdentityProvider.NAME, id);

      if (identity != null) {
        return identity;
      } else {
        Identity spaceIdentity = identityStorage.findIdentity(SpaceIdentityProvider.NAME, id);

        if (spaceIdentity != null) {
          return spaceIdentity;
        }

        // Try to see if space was renamed
        Space space = spaceService.getSpaceByGroupId(id);
        if (space == null) {
          space = spaceService.getSpaceByGroupId(SpaceUtils.SPACE_GROUP + "/" + id);
          if (space == null) {
            space = spaceService.getSpaceByPrettyName(id);
          }
        }
        if (space != null) {
          spaceIdentity = identityStorage.findIdentity(SpaceIdentityProvider.NAME, space.getPrettyName());
        } else {
          log.warn("Space '" + id + "'was not found");
        }
        return spaceIdentity;
      }
    } catch (Exception e) {
      log.error("Error while retrieving identity: ", e);
    }
    return null;
  }

  @SuppressWarnings("unchecked")
  protected static List<PortalContainer> getPortalContainers() {
    return (List<PortalContainer>) RootContainer.getInstance().getComponentInstancesOfType(PortalContainer.class);
  }

  protected static PortalContainer getPortalContainer(String name) {
    List<PortalContainer> portalContainers = getPortalContainers();
    for (PortalContainer portalContainer : portalContainers) {
      if (name.equals(portalContainer.getName())) {
        return portalContainer;
      }
    }
    return null;
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
