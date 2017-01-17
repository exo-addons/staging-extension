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
package org.exoplatform.management.backup.service.jcr;

import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.access.AccessManager;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.ext.backup.impl.rdbms.RdbmsWorkspaceInitializer;
import org.exoplatform.services.jcr.impl.core.LocationFactory;
import org.exoplatform.services.jcr.impl.core.NamespaceRegistryImpl;
import org.exoplatform.services.jcr.impl.core.nodetype.NodeTypeManagerImpl;
import org.exoplatform.services.jcr.impl.core.value.ValueFactoryImpl;
import org.exoplatform.services.jcr.impl.dataflow.persistent.CacheableWorkspaceDataManager;

import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

/**
 * The Class RestoreWorkspaceInitializer.
 *
 * @author <a href="mailto:boubaker.khanfir@exoplatform.com">Boubaker
 *         Khanfir</a>
 * @version $Revision$
 */
public class RestoreWorkspaceInitializer extends RdbmsWorkspaceInitializer {

  /** The restore in progress. */
  public static ThreadLocal<Boolean> RESTORE_IN_PROGRESS = new ThreadLocal<Boolean>();

  /**
   * Instantiates a new restore workspace initializer.
   *
   * @param config the config
   * @param repConfig the rep config
   * @param dataManager the data manager
   * @param namespaceRegistry the namespace registry
   * @param locationFactory the location factory
   * @param nodeTypeManager the node type manager
   * @param valueFactory the value factory
   * @param accessManager the access manager
   * @param repositoryService the repository service
   * @throws RepositoryConfigurationException the repository configuration exception
   * @throws PathNotFoundException the path not found exception
   * @throws RepositoryException the repository exception
   */
  public RestoreWorkspaceInitializer(WorkspaceEntry config, RepositoryEntry repConfig, CacheableWorkspaceDataManager dataManager, NamespaceRegistryImpl namespaceRegistry,
      LocationFactory locationFactory, NodeTypeManagerImpl nodeTypeManager, ValueFactoryImpl valueFactory, AccessManager accessManager, RepositoryService repositoryService)
      throws RepositoryConfigurationException, PathNotFoundException, RepositoryException {
    super(config, repConfig, dataManager, namespaceRegistry, locationFactory, nodeTypeManager, valueFactory, accessManager, repositoryService);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isWorkspaceInitialized() throws RepositoryException {
    return super.isWorkspaceInitialized() && !isRestoreInProgress();
  }

  /**
   * Sets the restore in progress.
   *
   * @param restoreInProgress the new restore in progress
   */
  public static void setRestoreInProgress(boolean restoreInProgress) {
    RestoreWorkspaceInitializer.RESTORE_IN_PROGRESS.set(restoreInProgress);
  }

  /**
   * Checks if is restore in progress.
   *
   * @return true, if is restore in progress
   */
  public static boolean isRestoreInProgress() {
    return RESTORE_IN_PROGRESS.get() != null && RESTORE_IN_PROGRESS.get();
  }

}
