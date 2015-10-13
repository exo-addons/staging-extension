package org.exoplatform.management.backup.service;

import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

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

/**
 * @author <a href="mailto:boubaker.khanfir@exoplatform.com">Boubaker
 *         Khanfir</a>
 * @version $Revision$
 */
public class CustomWorkspaceInitializer extends RdbmsWorkspaceInitializer {

  public static ThreadLocal<Boolean> RESTORE_IN_PROGRESS = new ThreadLocal<Boolean>();

  public CustomWorkspaceInitializer(WorkspaceEntry config, RepositoryEntry repConfig, CacheableWorkspaceDataManager dataManager, NamespaceRegistryImpl namespaceRegistry,
      LocationFactory locationFactory, NodeTypeManagerImpl nodeTypeManager, ValueFactoryImpl valueFactory, AccessManager accessManager, RepositoryService repositoryService)
      throws RepositoryConfigurationException, PathNotFoundException, RepositoryException {
    super(config, repConfig, dataManager, namespaceRegistry, locationFactory, nodeTypeManager, valueFactory, accessManager, repositoryService);
  }

  @Override
  public boolean isWorkspaceInitialized() {
    return super.isWorkspaceInitialized() && !isRestoreInProgress();
  }

  public static void setRestoreInProgress(boolean restoreInProgress) {
    CustomWorkspaceInitializer.RESTORE_IN_PROGRESS.set(restoreInProgress);
  }

  public static boolean isRestoreInProgress() {
    return RESTORE_IN_PROGRESS.get() != null && RESTORE_IN_PROGRESS.get();
  }

}
