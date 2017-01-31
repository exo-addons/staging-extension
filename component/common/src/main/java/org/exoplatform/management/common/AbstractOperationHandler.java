package org.exoplatform.management.common;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipInputStream;

import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.transaction.SystemException;

import org.apache.commons.io.FileUtils;
import org.exoplatform.services.cms.templates.TemplateService;
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
  protected Long defaultJCRSessionTimeout = null;
  protected TemplateService templateService = null;

  private Map<String, Boolean> isNTRecursiveMap = new HashMap<String, Boolean>();

  // This is used to test on duplicated activities
  protected Set<Long> activitiesByPostTime = new HashSet<Long>();

  protected void increaseCurrentTransactionTimeOut(OperationContext operationContext) {
    TransactionService transactionService = operationContext.getRuntimeContext().getRuntimeComponent(TransactionService.class);
    try {
      transactionService.setTransactionTimeout(86400);
    } catch (SystemException e1) {
      log.warn("Cannot Change Transaction timeout");
    }

    RepositoryService repositoryService = operationContext.getRuntimeContext().getRuntimeComponent(RepositoryService.class);
    ManageableRepository repo;
    try {
      repo = repositoryService.getCurrentRepository();
      if (defaultJCRSessionTimeout == null) {
        defaultJCRSessionTimeout = repo.getConfiguration().getSessionTimeOut();
      }
      repo.getConfiguration().setSessionTimeOut(ONE_DAY_IN_MS);
    } catch (Exception e) {
      log.warn("Cannot Change JCR Session timeout", e);
    }
  }

  protected void restoreDefaultTransactionTimeOut(OperationContext operationContext) {
    if (defaultJCRSessionTimeout == null) {
      return;
    }
    RepositoryService repositoryService = operationContext.getRuntimeContext().getRuntimeComponent(RepositoryService.class);
    ManageableRepository repo;
    try {
      repo = repositoryService.getCurrentRepository();
      repo.getConfiguration().setSessionTimeOut(defaultJCRSessionTimeout);
    } catch (Exception e) {
      log.warn("Cannot Change JCR Session timeout", e);
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
    Session session = provider.getSession(workspace, repository);
    if (session instanceof SessionImpl) {
      ((SessionImpl) session).setTimeout(ONE_DAY_IN_MS);
    }
    return session;
  }

  protected final Identity getIdentity(String id) {
    try {
      Identity userIdentity = identityStorage.findIdentity(OrganizationIdentityProvider.NAME, id);

      if (userIdentity != null) {
        return userIdentity;
      } else {
        Identity spaceIdentity = null;
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
        }
        return spaceIdentity;
      }
    } catch (Exception e) {
      // issue : We got a log error with a stack trace of the exception.
      log.warn("Failed to retrieve identity from id : "+id);
    }
    return null;
  }

  protected final boolean isRecursiveNT(NodeType nodeType) throws Exception {
    if (nodeType.getName().equals("exo:actionStorage")) {
      return true;
    }
    isNTRecursiveMap = new HashMap<String, Boolean>();
    if (!isNTRecursiveMap.containsKey(nodeType.getName())) {
      boolean hasMandatoryChild = false;
      NodeDefinition[] nodeDefinitions = nodeType.getChildNodeDefinitions();
      if (nodeDefinitions != null) {
        int i = 0;
        while (!hasMandatoryChild && i < nodeDefinitions.length) {
          hasMandatoryChild = nodeDefinitions[i].isMandatory();
          i++;
        }
      }
      boolean recursive = hasMandatoryChild;
      if (templateService != null) {
        recursive |= templateService.isManagedNodeType(nodeType.getName());
      }
      isNTRecursiveMap.put(nodeType.getName(), recursive);
    }
    return isNTRecursiveMap.get(nodeType.getName());
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
