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
package org.exoplatform.management.uiextension;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.exoplatform.commons.utils.LazyPageList;
import org.exoplatform.commons.utils.ListAccessImpl;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.management.service.api.Resource;
import org.exoplatform.management.service.api.ResourceHandler;
import org.exoplatform.management.service.api.StagingService;
import org.exoplatform.management.service.api.SynchronizationService;
import org.exoplatform.management.service.api.TargetServer;
import org.exoplatform.management.service.handler.ResourceHandlerLocator;
import org.exoplatform.management.uiextension.comparison.NodeComparison;
import org.exoplatform.management.uiextension.comparison.NodeComparisonState;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.ComponentConfigs;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIGrid;
import org.exoplatform.webui.core.UIPopupComponent;
import org.exoplatform.webui.core.UIPopupContainer;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.core.model.SelectItemOption;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormInputInfo;
import org.exoplatform.webui.form.UIFormSelectBox;

import java.net.ConnectException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * The Class PushContentPopupComponent.
 *
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */

@ComponentConfigs({
    @ComponentConfig(type = UIGrid.class, id = "selectedNodesGrid", template = "classpath:groovy/webui/component/explorer/popup/staging/UISelectedNodesGrid.gtmpl"),
    @ComponentConfig(lifecycle = UIFormLifecycle.class, template = "classpath:groovy/webui/component/explorer/popup/staging/PushContent.gtmpl", events = {
        @EventConfig(listeners = PushContentPopupComponent.CloseActionListener.class), @EventConfig(listeners = PushContentPopupComponent.PushActionListener.class),
        @EventConfig(listeners = PushContentPopupComponent.SelectActionListener.class) }) })
public class PushContentPopupComponent extends UIForm implements UIPopupComponent {
  
  /** The Constant LOG. */
  private static final Log LOG = ExoLogger.getLogger(PushContentPopupComponent.class.getName());

  /** The contents handler. */
  protected static ResourceHandler CONTENTS_HANDLER = ResourceHandlerLocator.getResourceHandler(StagingService.CONTENT_SITES_PATH);;

  /** The synchronization started. */
  protected boolean synchronizationStarted = false;
  
  /** The synchronization finished. */
  protected boolean synchronizationFinished = true;
  
  /** The synchronization error. */
  protected Throwable synchronizationError = null;

  /** The Constant TARGET_SERVER_NAME_FIELD_NAME. */
  public static final String TARGET_SERVER_NAME_FIELD_NAME = "targetServer";

  // public static final String PUBLISH_FIELD_NAME = "publishOnTarget";

  /** The Constant INFO_FIELD_NAME. */
  public static final String INFO_FIELD_NAME = "info";
  
  /** The Constant CLEANUP_PUBLICATION. */
  private static final String CLEANUP_PUBLICATION = "exo.staging.explorer.content.noVersion";

  /** The default selection. */
  private List<NodeComparison> defaultSelection = new ArrayList<NodeComparison>();
  
  /** The synchronization service. */
  private SynchronizationService synchronizationService_;

  /** The message type. */
  String messageType = "info";

  /** The state string. */
  String stateString = NodeComparisonState.MODIFIED_ON_SOURCE.getKey();
  
  /** The modified date filter. */
  Calendar modifiedDateFilter = null;
  
  /** The filter string. */
  String filterString = null;
  
  /** The published content only. */
  boolean publishedContentOnly = true;

  /** The target server input. */
  private final UIFormSelectBox targetServerInput;
  
  /** The info field. */
  private final UIFormInputInfo infoField;
  
  /** The select nodes component. */
  private final SelectNodesComponent selectNodesComponent;

  /** The target servers. */
  private List<TargetServer> targetServers;
  
  /** The current path. */
  private String currentPath;
  
  /** The workspace. */
  private String workspace;

  /** The selected nodes. */
  List<NodeComparison> selectedNodes = new ArrayList<NodeComparison>();

  /**
   * Instantiates a new push content popup component.
   *
   * @throws Exception the exception
   */
  public PushContentPopupComponent() throws Exception {
    infoField = new UIFormInputInfo(INFO_FIELD_NAME, INFO_FIELD_NAME, "");
    addUIFormInput(infoField);

    targetServerInput = new UIFormSelectBox(TARGET_SERVER_NAME_FIELD_NAME, TARGET_SERVER_NAME_FIELD_NAME, new ArrayList<SelectItemOption<String>>());
    addUIFormInput(targetServerInput);

    // addUIFormInput(new UICheckBoxInput(PUBLISH_FIELD_NAME,
    // PUBLISH_FIELD_NAME, false));

    selectNodesComponent = addChild(SelectNodesComponent.class, null, "SelectNodesComponent");
    selectNodesComponent.setPushContentPopupComponent(this);
    selectNodesComponent.setRendered(false);
  }

  /**
   * Inits the.
   *
   * @throws Exception the exception
   */
  public void init() throws Exception {
    List<SelectItemOption<String>> itemOptions = getChild(UIFormSelectBox.class).getOptions();
    itemOptions.clear();
    try {
      targetServers = synchronizationService_.getSynchonizationServers();
    } catch (Exception e) {
      LOG.warn(e);
      targetServers = new ArrayList<TargetServer>();
    }
    for (TargetServer targetServer : targetServers) {
      SelectItemOption<String> selectItemOption = new SelectItemOption<String>(targetServer.getName(), targetServer.getId());
      itemOptions.add(selectItemOption);
    }
    NodeComparison nodeComparison = new NodeComparison();
    nodeComparison.setTitle(getResourceBundle().getString("PushContentPopupComponent.label.currentContent"));
    nodeComparison.setPath(getCurrentPath());
    nodeComparison.setState(NodeComparisonState.UNKNOWN);
    defaultSelection.add(nodeComparison);

    selectNodesComponent.getSelectedNodesGrid().getUIPageIterator().setPageList(new LazyPageList<NodeComparison>(new ListAccessImpl<NodeComparison>(NodeComparison.class, defaultSelection), 5));
  }

  /**
   * Adds the selection.
   *
   * @param nodeComparison the node comparison
   */
  public void addSelection(NodeComparison nodeComparison) {
    selectedNodes.add(nodeComparison);
  }

  /**
   * Checks if is synchronization started.
   *
   * @return true, if is synchronization started
   */
  public boolean isSynchronizationStarted() {
    return synchronizationStarted;
  }

  /**
   * Checks if is default entry.
   *
   * @param path the path
   * @return true, if is default entry
   */
  public boolean isDefaultEntry(String path) {
    for (NodeComparison comparison : defaultSelection) {
      if (path.equals(comparison.getPath())) {
        return true;
      }
    }
    return false;
  }

  /**
   * Sets the message.
   *
   * @param message the message
   * @param type the type
   */
  public void setMessage(String message, String type) {
    getUIFormInputInfo(INFO_FIELD_NAME).setValue(message);
    messageType = type;
  }

  /**
   * The listener interface for receiving selectAction events.
   * The class that is interested in processing a selectAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addSelectActionListener</code> method. When
   * the selectAction event occurs, that object's appropriate
   * method is invoked.
   *
   */
  static public class SelectActionListener extends EventListener<UIForm> {
    
    /**
     * {@inheritDoc}
     */
    public void execute(Event<UIForm> event) throws Exception {
      UIForm uiForm = event.getSource();
      PushContentPopupComponent pushContentPopupComponent = null;
      if (uiForm instanceof PushContentPopupComponent) {
        pushContentPopupComponent = (PushContentPopupComponent) uiForm;
      } else if (uiForm instanceof SelectNodesComponent) {
        pushContentPopupComponent = ((SelectNodesComponent) uiForm).getPushContentPopupComponent();
      }
      pushContentPopupComponent.setMessage(null, "info");
      try {
        // get target server
        TargetServer targetServer = null;
        String targetServerId = pushContentPopupComponent.getUIFormSelectBox(TARGET_SERVER_NAME_FIELD_NAME).getValue();
        Iterator<TargetServer> iterator = pushContentPopupComponent.getTargetServers().iterator();
        while (iterator.hasNext() && targetServer == null) {
          TargetServer itTargetServer = iterator.next();
          if (itTargetServer.getId().equals(targetServerId)) {
            targetServer = itTargetServer;
          }
        }
        if (targetServer == null) {
          ApplicationMessage message = new ApplicationMessage("PushContent.msg.targetServerMandatory", null, ApplicationMessage.ERROR);
          message.setResourceBundle(getResourceBundle());
          pushContentPopupComponent.setMessage(message.getMessage(), "error");
          event.getRequestContext().addUIComponentToUpdateByAjax(pushContentPopupComponent.getParent());
          return;
        }

        List<NodeComparison> comparisons = Utils.compareLocalNodesWithTargetServer(pushContentPopupComponent.getWorkspace(), pushContentPopupComponent.getCurrentPath(), targetServer);
        pushContentPopupComponent.getSelectNodesComponent().init();
        pushContentPopupComponent.getSelectNodesComponent().setComparisons(comparisons);
        pushContentPopupComponent.getSelectNodesComponent().setRendered(true);
        event.getRequestContext().addUIComponentToUpdateByAjax(pushContentPopupComponent.getParent());
      } catch (Exception ex) {
        ApplicationMessage message;
        if (isConnectionException(ex)) {
          message = new ApplicationMessage("PushContent.msg.unableToConnect", null, ApplicationMessage.ERROR);
        } else {
          message = new ApplicationMessage("PushContent.msg.synchronizationError", null, ApplicationMessage.ERROR);
        }
        message.setResourceBundle(getResourceBundle());
        pushContentPopupComponent.setMessage(message.getMessage(), "error");
        LOG.error("Comparison of '" + pushContentPopupComponent.getCurrentPath() + "' failed:", ex);
      }
    }
  }

  /**
   * The listener interface for receiving pushAction events.
   * The class that is interested in processing a pushAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addPushActionListener</code> method. When
   * the pushAction event occurs, that object's appropriate
   * method is invoked.
   *
   */
  static public class PushActionListener extends EventListener<PushContentPopupComponent> {
    
    /**
     * {@inheritDoc}
     */
    public void execute(Event<PushContentPopupComponent> event) throws Exception {
      final PushContentPopupComponent pushContentPopupComponent = event.getSource();
      pushContentPopupComponent.setMessage(null, "info");

      UIPopupContainer uiPopupContainer = (UIPopupContainer) pushContentPopupComponent.getAncestorOfType(UIPopupContainer.class);
      UIApplication uiApp = pushContentPopupComponent.getAncestorOfType(UIApplication.class);
      try {
        // get cleanupPublication checkbox value
        // boolean cleanupPublication =
        // pushContentPopupComponent.getUICheckBoxInput(PUBLISH_FIELD_NAME).getValue();
        final boolean cleanupPublication = false;

        // get target server
        TargetServer targetServer = null;
        String targetServerId = pushContentPopupComponent.getUIFormSelectBox(TARGET_SERVER_NAME_FIELD_NAME).getValue();
        Iterator<TargetServer> iterator = pushContentPopupComponent.getTargetServers().iterator();
        while (iterator.hasNext() && targetServer == null) {
          TargetServer itTargetServer = iterator.next();
          if (itTargetServer.getId().equals(targetServerId)) {
            targetServer = itTargetServer;
          }
        }
        final TargetServer selectedServer = targetServer;
        if (targetServer == null) {
          ApplicationMessage message = new ApplicationMessage("PushContent.msg.targetServerMandatory", null, ApplicationMessage.ERROR);
          message.setResourceBundle(getResourceBundle());
          pushContentPopupComponent.setMessage(message.getMessage(), "error");
          event.getRequestContext().addUIComponentToUpdateByAjax(uiPopupContainer);
          return;
        }

        ResourceBundle resourceBundle = PushContentPopupComponent.getResourceBundle();
        pushContentPopupComponent.setMessage(resourceBundle.getString("PushNavigation.msg.synchronizationInProgress"), "info");
        if (pushContentPopupComponent.synchronizationFinished && !pushContentPopupComponent.synchronizationStarted) {
          pushContentPopupComponent.synchronizationStarted = true;
          pushContentPopupComponent.synchronizationFinished = false;

          Thread synchronizeThread = new Thread(new Runnable() {
            @Override
            public void run() {
              // Make sure that current container is of type
              // "PortalContainer"
              ExoContainerContext.setCurrentContainer(PortalContainer.getInstance());

              // Use "PortalContainer" in current transaction
              RequestLifeCycle.begin(ExoContainerContext.getCurrentContainer());
              try {
                List<NodeComparison> selectedComparisons = (List<NodeComparison>) pushContentPopupComponent.getSelectedNodes();
                // If default selection
                if (selectedComparisons.isEmpty()) {
                  List<Resource> resources = new ArrayList<Resource>();
                  resources.add(new Resource(StagingService.CONTENT_SITES_PATH + "/shared", "shared", "shared"));

                  Map<String, String> exportOptions = new HashMap<String, String>();
                  Map<String, String> importOptions = new HashMap<String, String>();

                  String sqlQueryFilter = "query:select * from nt:base where jcr:path like '" + pushContentPopupComponent.getCurrentPath() + "'";
                  exportOptions.put("filter/query", StringEscapeUtils.unescapeHtml(URLDecoder.decode(sqlQueryFilter, "UTF-8")));
                  exportOptions.put("filter/taxonomy", "false");
                  exportOptions.put("filter/no-history", "" + cleanupPublication);
                  exportOptions.put("filter/workspace", pushContentPopupComponent.getWorkspace());

                  boolean noVersion = false;
                  String noVersionString = System.getProperty(CLEANUP_PUBLICATION, null);
                  if (!StringUtils.isEmpty(noVersionString)) {
                    noVersion = noVersionString.trim().equals("true");
                  }

                  if (noVersion) {
                    exportOptions.put("filter/no-history", "true");
                    importOptions.put("filter/cleanPublication", "true");
                  }

                  // importOptions.put("filter/cleanPublication",
                  // "" +
                  // cleanupPublication);

                  CONTENTS_HANDLER.synchronize(resources, exportOptions, importOptions, selectedServer);
                } else {
                  // Multiple contents was selected,
                  // synchronize one by one

                  for (NodeComparison nodeComparison : selectedComparisons) {
                    List<Resource> resources = new ArrayList<Resource>();
                    resources.add(new Resource(StagingService.CONTENT_SITES_PATH + "/shared/contents", "shared", "shared"));

                    Map<String, String> exportOptions = new HashMap<String, String>();
                    Map<String, String> importOptions = new HashMap<String, String>();

                    if (nodeComparison.getState().equals(NodeComparisonState.NOT_FOUND_ON_SOURCE)) {
                      exportOptions.put("filter/removeNodes", nodeComparison.getPath());
                    } else {

                      boolean noVersion = false;
                      String noVersionString = System.getProperty(CLEANUP_PUBLICATION, null);
                      if (!StringUtils.isEmpty(noVersionString)) {
                        noVersion = noVersionString.trim().equals("true");
                      }

                      if (noVersion && nodeComparison.isPublished()) {
                        exportOptions.put("filter/no-history", "true");
                        importOptions.put("filter/cleanPublication", "true");
                      }

                      String sqlQueryFilter = "query:select * from nt:base where jcr:path like '" + nodeComparison.getPath() + "'";
                      exportOptions.put("filter/query", StringEscapeUtils.unescapeHtml(URLDecoder.decode(sqlQueryFilter, "UTF-8")));
                      exportOptions.put("filter/taxonomy", "false");
                      exportOptions.put("filter/no-history", "" + cleanupPublication);
                      exportOptions.put("filter/workspace", pushContentPopupComponent.getWorkspace());
                    }
                    CONTENTS_HANDLER.synchronize(resources, exportOptions, importOptions, selectedServer);
                  }
                }
                LOG.info("Synchronization of '" + pushContentPopupComponent.getCurrentPath() + "' done.");

              } catch (Exception e) {
                pushContentPopupComponent.synchronizationError = e;
              } finally {
                pushContentPopupComponent.synchronizationFinished = true;
                RequestLifeCycle.end();
              }
            }
          });
          synchronizeThread.start();
        } else {
          if (pushContentPopupComponent.synchronizationStarted) {
            if (pushContentPopupComponent.synchronizationFinished) {
              if (pushContentPopupComponent.synchronizationError == null) {
                pushContentPopupComponent.synchronizationStarted = false;
                // Update UI
                uiPopupContainer.deActivate();
                uiApp.addMessage(new ApplicationMessage("PushContent.msg.synchronizationDone", null, ApplicationMessage.INFO));
              } else {
                Throwable tempException = pushContentPopupComponent.synchronizationError;
                pushContentPopupComponent.synchronizationStarted = false;
                pushContentPopupComponent.synchronizationError = null;
                throw tempException;
              }
            }
          }
        }
      } catch (Throwable ex) {
        ApplicationMessage message = null;
        if (isConnectionException(ex)) {
          message = new ApplicationMessage("PushContent.msg.unableToConnect", null, ApplicationMessage.ERROR);
        } else {
          message = new ApplicationMessage("PushContent.msg.synchronizationError", null, ApplicationMessage.ERROR);
        }
        message.setResourceBundle(getResourceBundle());
        uiPopupContainer.deActivate();
        uiApp.addMessage(message);
      }
    }
  }

  /**
   * Check if the exception has a ConnectionException cause.
   *
   * @param ex the ex
   * @return true, if is connection exception
   */
  private static boolean isConnectionException(Throwable ex) {
    boolean connectionException = false;
    Throwable throwable = ex;
    do {
      if (throwable instanceof ConnectException) {
        connectionException = true;
      } else {
        throwable = throwable.getCause();
      }
    } while (!connectionException && throwable != null);

    return connectionException;
  }

  /**
   * The listener interface for receiving closeAction events.
   * The class that is interested in processing a closeAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addCloseActionListener</code> method. When
   * the closeAction event occurs, that object's appropriate
   * method is invoked.
   *
   */
  static public class CloseActionListener extends EventListener<PushContentPopupComponent> {
    
    /**
     * {@inheritDoc}
     */
    public void execute(Event<PushContentPopupComponent> event) throws Exception {
      PushContentPopupComponent pushContentPopupComponent = event.getSource();
      UIPopupContainer uiPopupContainer = (UIPopupContainer) pushContentPopupComponent.getAncestorOfType(UIPopupContainer.class);
      uiPopupContainer.deActivate();
      event.getRequestContext().addUIComponentToUpdateByAjax(uiPopupContainer);
    }
  }

  /**
   * {@inheritDoc}
   */
  public void activate() {}

  /**
   * {@inheritDoc}
   */
  public void deActivate() {}

  /**
   * {@inheritDoc}
   */
  public String[] getActions() {
    return new String[] { "Push", "Close", "Select" };
  }

  /**
   * Gets the selected nodes.
   *
   * @return the selected nodes
   */
  public List<NodeComparison> getSelectedNodes() {
    return selectedNodes;
  }

  /**
   * Gets the resource bundle.
   *
   * @return the resource bundle
   */
  public static ResourceBundle getResourceBundle() {
    return WebuiRequestContext.getCurrentInstance().getApplicationResourceBundle();
  }

  /**
   * Gets the target servers.
   *
   * @return the target servers
   */
  public List<TargetServer> getTargetServers() {
    return targetServers;
  }

  /**
   * Sets the synchronization service.
   *
   * @param synchronizationService the new synchronization service
   */
  public void setSynchronizationService(SynchronizationService synchronizationService) {
    synchronizationService_ = synchronizationService;
  }

  /**
   * Sets the current path.
   *
   * @param currentPath the new current path
   */
  public void setCurrentPath(String currentPath) {
    this.currentPath = currentPath;
  }

  /**
   * Gets the current path.
   *
   * @return the current path
   */
  public String getCurrentPath() {
    return this.currentPath;
  }

  /**
   * Gets the workspace.
   *
   * @return the workspace
   */
  public String getWorkspace() {
    return workspace;
  }

  /**
   * Sets the workspace.
   *
   * @param workspace the new workspace
   */
  public void setWorkspace(String workspace) {
    this.workspace = workspace;
  }

  /**
   * Gets the default selection.
   *
   * @return the default selection
   */
  public List<NodeComparison> getDefaultSelection() {
    return defaultSelection;
  }

  /**
   * Gets the select nodes component.
   *
   * @return the select nodes component
   */
  public SelectNodesComponent getSelectNodesComponent() {
    return selectNodesComponent;
  }
}