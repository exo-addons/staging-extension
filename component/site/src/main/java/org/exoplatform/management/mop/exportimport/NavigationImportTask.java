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

package org.exoplatform.management.mop.exportimport;

import org.exoplatform.management.mop.operations.navigation.NavigationUtils;
import org.exoplatform.portal.config.DataStorage;
import org.exoplatform.portal.config.model.NavigationFragment;
import org.exoplatform.portal.config.model.PageNavigation;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.description.DescriptionService;
import org.exoplatform.portal.mop.importer.ImportMode;
import org.exoplatform.portal.mop.navigation.NavigationContext;
import org.exoplatform.portal.mop.navigation.NavigationService;
import org.exoplatform.portal.mop.navigation.NodeChangeListener;
import org.exoplatform.portal.mop.navigation.NodeContext;
import org.exoplatform.portal.mop.navigation.NodeState;
import org.exoplatform.portal.mop.navigation.Scope;
import org.gatein.common.logging.Logger;
import org.gatein.common.logging.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The Class NavigationImportTask.
 *
 * @author <a href="mailto:nscavell@redhat.com">Nick Scavelli</a>
 * @version $Revision$
 */
public class NavigationImportTask extends AbstractImportTask<PageNavigation> {
  
  /** The Constant log. */
  private static final Logger log = LoggerFactory.getLogger(NavigationImportTask.class);

  /** The navigation service. */
  private NavigationService navigationService;
  
  /** The description service. */
  private DescriptionService descriptionService;
  
  /** The data storage. */
  private DataStorage dataStorage;
  
  /** The rollback task. */
  private RollbackTask rollbackTask;

  /**
   * Instantiates a new navigation import task.
   *
   * @param data the data
   * @param siteKey the site key
   * @param navigationService the navigation service
   * @param descriptionService the description service
   * @param dataStorage the data storage
   */
  public NavigationImportTask(PageNavigation data, SiteKey siteKey, NavigationService navigationService, DescriptionService descriptionService, DataStorage dataStorage) {
    super(data, siteKey);
    this.navigationService = navigationService;
    this.descriptionService = descriptionService;
    this.dataStorage = dataStorage;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void importData(ImportMode importMode) throws Exception {
    PortalConfig portalConfig = dataStorage.getPortalConfig(siteKey.getTypeName(), siteKey.getName());
    if (portalConfig == null)
      throw new Exception("Cannot import navigation because site does not exist for " + siteKey);

    Locale locale = (portalConfig.getLocale() == null) ? Locale.ENGLISH : new Locale(portalConfig.getLocale());

    final NavigationContext navContext = navigationService.loadNavigation(siteKey);
    if (navContext == null) {
      rollbackTask = new RollbackTask() {
        @Override
        public String getDescription() {
          return "Deleting navigation for site " + siteKey;
        }

        @Override
        public void rollback() throws Exception {
          log.debug("Rollback: " + getDescription());
          navigationService.destroyNavigation(navContext);
        }
      };
    } else {

      final List<NodeContext<NodeContext<?>>> snapshots = new ArrayList<NodeContext<NodeContext<?>>>(data.getFragments().size());
      for (NavigationFragment fragment : data.getFragments()) {
        snapshots.add(NavigationUtils.loadNode(navigationService, navContext, fragment.getParentURI()));
      }

      rollbackTask = new RollbackTask() {
        @Override
        public String getDescription() {
          return "Rolling back navigation changes...";
        }

        @Override
        public void rollback() throws Exception {
          log.debug(getDescription());

          for (NodeContext<NodeContext<?>> snapshot : snapshots) {
            RollbackChangeListener listener = new RollbackChangeListener();
            navigationService.updateNode(snapshot, Scope.ALL, listener);

            // Rollback...
            listener.rollback();

            // If any errors, throw exception
            if (listener.errors) {
              throw new Exception("Error rolling back navigation snapshot '" + snapshot.getName() + "'");
            }

            navigationService.saveNode(snapshot, null);
          }

          log.debug("Successfully rolled back navigation changes.");
        }
      };
    }

    // Import navigation using gatein navigation importer.
    NavigationImporter importer = new NavigationImporter(locale, importMode, data, navigationService, descriptionService);
    importer.perform();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void rollback() throws Exception {
    if (rollbackTask != null) {
      rollbackTask.rollback();
    }
  }

  /**
   * The Interface RollbackTask.
   */
  private interface RollbackTask {
    
    /**
     * Gets the description.
     *
     * @return the description
     */
    String getDescription();

    /**
     * Rollback.
     *
     * @throws Exception the exception
     */
    void rollback() throws Exception;
  }

  /**
   * The listener interface for receiving rollbackChange events.
   * The class that is interested in processing a rollbackChange
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addRollbackChangeListener</code> method. When
   * the rollbackChange event occurs, that object's appropriate
   * method is invoked.
   *
   * @see RollbackChangeEvent
   */
  private static class RollbackChangeListener implements NodeChangeListener<NodeContext<NodeContext<?>>> {
    
    /** The tasks. */
    private List<RollbackTask> tasks = new ArrayList<RollbackTask>();
    
    /** The errors. */
    private boolean errors;

    /**
     * {@inheritDoc}
     */
    @Override
    public void onAdd(final NodeContext<NodeContext<?>> target, final NodeContext<NodeContext<?>> parent, NodeContext<NodeContext<?>> previous) {
      tasks.add(new RollbackTask() {
        @Override
        public String getDescription() {
          return "Removing node " + target.getName() + " from parent " + parent.getName();
        }

        @Override
        public void rollback() throws Exception {
          parent.removeNode(target.getName());
        }
      });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreate(final NodeContext<NodeContext<?>> target, final NodeContext<NodeContext<?>> parent, final NodeContext<NodeContext<?>> previous, final String name) {
      tasks.add(new RollbackTask() {
        @Override
        public String getDescription() {
          return "Removing node " + name + " from parent " + parent.getName();
        }

        @Override
        public void rollback() throws Exception {
          parent.removeNode(name);
        }
      });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onRemove(final NodeContext<NodeContext<?>> target, final NodeContext<NodeContext<?>> parent) {
      tasks.add(new RollbackTask() {
        // Copy all state for rollback
        private String name = target.getName();
        private Integer index = target.getIndex();
        boolean hidden = target.isHidden();
        private NodeState state = target.getState();

        @Override
        public String getDescription() {
          return "Adding node " + target.getName() + " to parent " + parent.getName();
        }

        @Override
        public void rollback() throws Exception {
          NodeContext<?> node = parent.add(index, name);
          node.setState(state);
          node.setHidden(hidden);
        }
      });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onDestroy(final NodeContext<NodeContext<?>> target, final NodeContext<NodeContext<?>> parent) {
      tasks.add(new RollbackTask() {
        @Override
        public String getDescription() {
          return "Adding node " + target.getName() + " from parent " + parent.getName();
        }

        @Override
        public void rollback() throws Exception {
          parent.add(null, target);
        }
      });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onRename(final NodeContext<NodeContext<?>> target, final NodeContext<NodeContext<?>> parent, final String name) {
      tasks.add(new RollbackTask() {
        // Copy previous name for rollback
        private String targetName = target.getName();

        @Override
        public String getDescription() {
          return "Renaming node " + name + " to " + targetName + " for parent " + parent.getName();
        }

        @Override
        public void rollback() throws Exception {
          target.setName(targetName);
        }
      });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onUpdate(final NodeContext<NodeContext<?>> target, final NodeState state) {
      tasks.add(new RollbackTask() {
        // Copy state for rollback
        private NodeState targetState = target.getState();

        @Override
        public String getDescription() {
          return "Setting node " + target.getName() + " back to previous state " + targetState;
        }

        @Override
        public void rollback() throws Exception {
          target.setState(targetState);
        }
      });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onMove(final NodeContext<NodeContext<?>> target, final NodeContext<NodeContext<?>> from, final NodeContext<NodeContext<?>> to, NodeContext<NodeContext<?>> previous) {
      tasks.add(new RollbackTask() {
        @Override
        public String getDescription() {
          return "Moving node " + target.getName() + " from " + to.getName() + " to " + from.getName();
        }

        @Override
        public void rollback() throws Exception {
          from.add(target.getIndex(), target);
          to.removeNode(target.getName());
        }
      });
    }

    /**
     * Rollback.
     */
    public void rollback() {
      boolean debug = log.isDebugEnabled();
      for (RollbackTask task : tasks) {
        try {
          if (debug) {
            log.debug("Rollback: " + task.getDescription());
          }
          task.rollback();
        } catch (Exception e) {
          log.error("Exception during NodeChangeListener's rollback task: " + task.getDescription(), e);
          errors = true;
        }
      }
    }
  }
}
