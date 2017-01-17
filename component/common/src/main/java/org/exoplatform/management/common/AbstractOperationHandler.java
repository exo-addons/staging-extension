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
package org.exoplatform.management.common;

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

/**
 * The Class AbstractOperationHandler.
 */
public abstract class AbstractOperationHandler implements OperationHandler {

  /** The Constant POLL_ACTIVITY_TYPE. */
  public static final String POLL_ACTIVITY_TYPE = "ks-poll:spaces";
  
  /** The Constant FORUM_ACTIVITY_TYPE. */
  public static final String FORUM_ACTIVITY_TYPE = "ks-forum:spaces";
  
  /** The Constant CALENDAR_ACTIVITY_TYPE. */
  public static final String CALENDAR_ACTIVITY_TYPE = "cs-calendar:spaces";
  
  /** The Constant WIKI_ACTIVITY_TYPE. */
  public static final String WIKI_ACTIVITY_TYPE = "ks-wiki:spaces";
  
  /** The Constant CONTENT_ACTIVITY_TYPE. */
  public static final String CONTENT_ACTIVITY_TYPE = "contents:spaces";
  
  /** The Constant FILE_ACTIVITY_TYPE. */
  public static final String FILE_ACTIVITY_TYPE = "files:spaces";

  /** The Constant log. */
  protected static final Log log = ExoLogger.getLogger(AbstractOperationHandler.class);

  /** The Constant EMPTY_STRING_ARRAY. */
  protected static final String[] EMPTY_STRING_ARRAY = new String[0];

  /** The Constant ONE_DAY_IN_SECONDS. */
  protected static final int ONE_DAY_IN_SECONDS = 86400;
  
  /** The Constant ONE_DAY_IN_MS. */
  protected static final long ONE_DAY_IN_MS = 86400000L;

  /** The default JCR session timeout. */
  protected static Long defaultJCRSessionTimeout = null;

  /** The repository service. */
  protected RepositoryService repositoryService = null;
  
  /** The identity storage. */
  protected IdentityStorage identityStorage = null;
  
  /** The space service. */
  protected SpaceService spaceService = null;

  /** The activities by post time. */
  // This is used to test on duplicated activities
  protected Set<Long> activitiesByPostTime = new HashSet<Long>();

  /**
   * Increase current transaction time out.
   *
   * @param operationContext the operation context
   */
  public static void increaseCurrentTransactionTimeOut(OperationContext operationContext) {
    TransactionService transactionService = operationContext.getRuntimeContext().getRuntimeComponent(TransactionService.class);
    increaseCurrentTransactionTimeOut(transactionService);
    RepositoryService repositoryService = operationContext.getRuntimeContext().getRuntimeComponent(RepositoryService.class);
    increaseCurrentTransactionTimeOut(repositoryService);
  }

  /**
   * Increase current transaction time out.
   *
   * @param portalContainer the portal container
   */
  public static void increaseCurrentTransactionTimeOut(PortalContainer portalContainer) {
    TransactionService transactionService = (TransactionService) portalContainer.getComponentInstanceOfType(TransactionService.class);
    increaseCurrentTransactionTimeOut(transactionService);
    RepositoryService repositoryService = (RepositoryService) portalContainer.getComponentInstanceOfType(RepositoryService.class);
    increaseCurrentTransactionTimeOut(repositoryService);
  }

  /**
   * Restore default transaction time out.
   *
   * @param operationContext the operation context
   */
  public void restoreDefaultTransactionTimeOut(OperationContext operationContext) {
    RepositoryService repositoryService = operationContext.getRuntimeContext().getRuntimeComponent(RepositoryService.class);
    restoreDefaultTransactionTimeOut(repositoryService);
  }

  /**
   * Restore default transaction time out.
   *
   * @param portalContainer the portal container
   */
  public void restoreDefaultTransactionTimeOut(PortalContainer portalContainer) {
    RepositoryService repositoryService = (RepositoryService) portalContainer.getComponentInstanceOfType(RepositoryService.class);
    restoreDefaultTransactionTimeOut(repositoryService);
  }

  /**
   * Increase current transaction time out.
   *
   * @param transactionService the transaction service
   */
  public static void increaseCurrentTransactionTimeOut(TransactionService transactionService) {
    try {
      transactionService.setTransactionTimeout(86400);
    } catch (SystemException e1) {
      log.warn("Cannot Change Transaction timeout");
    }
  }

  /**
   * Increase current transaction time out.
   *
   * @param repositoryService the repository service
   */
  public static void increaseCurrentTransactionTimeOut(RepositoryService repositoryService) {
    try {
      ManageableRepository repo = repositoryService.getCurrentRepository();
      if (defaultJCRSessionTimeout == null) {
        defaultJCRSessionTimeout = repo.getConfiguration().getSessionTimeOut();
      }
      repo.getConfiguration().setSessionTimeOut(ONE_DAY_IN_MS);
    } catch (Exception e) {
      log.warn("Cannot Change JCR Session timeout", e);
    }
  }

  /**
   * Restore default transaction time out.
   *
   * @param repositoryService the repository service
   */
  public void restoreDefaultTransactionTimeOut(RepositoryService repositoryService) {
    if (defaultJCRSessionTimeout == null) {
      return;
    }
    try {
      ManageableRepository repo = repositoryService.getCurrentRepository();
      repo.getConfiguration().setSessionTimeOut(defaultJCRSessionTimeout);
    } catch (Exception e) {
      log.warn("Cannot Change JCR Session timeout", e);
    }
  }

  /**
   * Delete temp file.
   *
   * @param fileToImport the file to import
   */
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

  /**
   * Gets the session.
   *
   * @param workspace the workspace
   * @return the session
   * @throws RepositoryException the repository exception
   * @throws LoginException the login exception
   * @throws NoSuchWorkspaceException the no such workspace exception
   */
  protected final Session getSession(String workspace) throws RepositoryException, LoginException, NoSuchWorkspaceException {
    return getSession(repositoryService, workspace);
  }

  /**
   * Gets the session.
   *
   * @param repositoryService the repository service
   * @param workspace the workspace
   * @return the session
   * @throws RepositoryException the repository exception
   * @throws LoginException the login exception
   * @throws NoSuchWorkspaceException the no such workspace exception
   */
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

  /**
   * Gets the identity.
   *
   * @param id the id
   * @return the identity
   */
  protected final Identity getIdentity(String id) {
    try {
      Identity userIdentity = identityStorage.findIdentity(OrganizationIdentityProvider.NAME, id);

      if (userIdentity != null) {
        return userIdentity;
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

  /**
   * Gets the portal containers.
   *
   * @return the portal containers
   */
  protected static List<PortalContainer> getPortalContainers() {
    return RootContainer.getInstance().getComponentInstancesOfType(PortalContainer.class);
  }

  /**
   * Gets the portal container.
   *
   * @param name the name
   * @return the portal container
   */
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
  /**
   * The Class NonCloseableZipInputStream.
   */
  // information.
  public static class NonCloseableZipInputStream extends ZipInputStream {
    
    /**
     * Instantiates a new non closeable zip input stream.
     *
     * @param inputStream the input stream
     */
    public NonCloseableZipInputStream(InputStream inputStream) {
      super(inputStream);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws IOException {}

    /**
     * Really close.
     *
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public void reallyClose() throws IOException {
      super.close();
    }
  }
}
