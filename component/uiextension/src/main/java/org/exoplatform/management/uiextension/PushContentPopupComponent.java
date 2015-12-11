package org.exoplatform.management.uiextension;

import java.net.ConnectException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

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

/**
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */

@ComponentConfigs({ @ComponentConfig(
  type = UIGrid.class,
  id = "selectedNodesGrid",
  template = "classpath:groovy/webui/component/explorer/popup/staging/UISelectedNodesGrid.gtmpl"), @ComponentConfig(
  lifecycle = UIFormLifecycle.class,
  template = "classpath:groovy/webui/component/explorer/popup/staging/PushContent.gtmpl",
  events = { @EventConfig(
    listeners = PushContentPopupComponent.CloseActionListener.class), @EventConfig(
    listeners = PushContentPopupComponent.PushActionListener.class), @EventConfig(
    listeners = PushContentPopupComponent.SelectActionListener.class) }) })
public class PushContentPopupComponent extends UIForm implements UIPopupComponent {
  private static final Log LOG = ExoLogger.getLogger(PushContentPopupComponent.class.getName());

  protected static ResourceHandler CONTENTS_HANDLER = ResourceHandlerLocator.getResourceHandler(StagingService.CONTENT_SITES_PATH);;

  protected boolean synchronizationStarted = false;
  protected boolean synchronizationFinished = true;
  protected Throwable synchronizationError = null;

  public static final String TARGET_SERVER_NAME_FIELD_NAME = "targetServer";

  // public static final String PUBLISH_FIELD_NAME = "publishOnTarget";

  public static final String INFO_FIELD_NAME = "info";
  private static final String CLEANUP_PUBLICATION = "exo.staging.explorer.content.noVersion";

  private List<NodeComparison> defaultSelection = new ArrayList<NodeComparison>();
  private SynchronizationService synchronizationService_;

  String stateString = NodeComparisonState.MODIFIED_ON_SOURCE.getKey();
  Calendar modifiedDateFilter = null;
  String filterString = null;
  boolean publishedContentOnly = true;

  private final UIFormSelectBox targetServerInput;
  private final UIFormInputInfo infoField;
  private final SelectNodesComponent selectNodesComponent;

  private List<TargetServer> targetServers;
  private String currentPath;
  private String workspace;

  List<NodeComparison> selectedNodes = new ArrayList<NodeComparison>();

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

  public void addSelection(NodeComparison nodeComparison) {
    selectedNodes.add(nodeComparison);
  }

  public boolean isSynchronizationStarted() {
    return synchronizationStarted;
  }

  public boolean isDefaultEntry(String path) {
    for (NodeComparison comparison : defaultSelection) {
      if (path.equals(comparison.getPath())) {
        return true;
      }
    }
    return false;
  }

  static public class SelectActionListener extends EventListener<UIForm> {
    public void execute(Event<UIForm> event) throws Exception {
      UIForm uiForm = event.getSource();
      PushContentPopupComponent pushContentPopupComponent = null;
      if (uiForm instanceof PushContentPopupComponent) {
        pushContentPopupComponent = (PushContentPopupComponent) uiForm;
      } else if (uiForm instanceof SelectNodesComponent) {
        pushContentPopupComponent = ((SelectNodesComponent) uiForm).getPushContentPopupComponent();
      }
      pushContentPopupComponent.getUIFormInputInfo(INFO_FIELD_NAME).setValue(null);
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
          pushContentPopupComponent.getUIFormInputInfo(INFO_FIELD_NAME).setValue(message.getMessage());
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
        pushContentPopupComponent.getUIFormInputInfo(INFO_FIELD_NAME).setValue(message.getMessage());
        LOG.error("Comparison of '" + pushContentPopupComponent.getCurrentPath() + "' failed:", ex);
      }
    }
  }

  static public class PushActionListener extends EventListener<PushContentPopupComponent> {
    public void execute(Event<PushContentPopupComponent> event) throws Exception {
      final PushContentPopupComponent pushContentPopupComponent = event.getSource();
      pushContentPopupComponent.getUIFormInputInfo(INFO_FIELD_NAME).setValue(null);

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
          pushContentPopupComponent.getUIFormInputInfo(INFO_FIELD_NAME).setValue(message.getMessage());
          event.getRequestContext().addUIComponentToUpdateByAjax(uiPopupContainer);
          return;
        }

        ResourceBundle resourceBundle = PushContentPopupComponent.getResourceBundle();
        pushContentPopupComponent.getUIFormInputInfo(INFO_FIELD_NAME).setValue(resourceBundle.getString("PushNavigation.msg.synchronizationInProgress"));
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
        ApplicationMessage message;
        if (isConnectionException(ex)) {
          message = new ApplicationMessage("PushContent.msg.unableToConnect", null, ApplicationMessage.ERROR);
        } else {
          message = new ApplicationMessage("PushContent.msg.synchronizationError", null, ApplicationMessage.ERROR);
        }
        message.setResourceBundle(getResourceBundle());
        pushContentPopupComponent.getUIFormInputInfo(INFO_FIELD_NAME).setValue(message.getMessage());
        LOG.error("Synchronization of '" + pushContentPopupComponent.getCurrentPath() + "' failed:", ex);
        event.getRequestContext().addUIComponentToUpdateByAjax(uiPopupContainer);
      }
    }
  }

  /**
   * Check if the exception has a ConnectionException cause
   * 
   * @param ex
   * @return
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

  static public class CloseActionListener extends EventListener<PushContentPopupComponent> {
    public void execute(Event<PushContentPopupComponent> event) throws Exception {
      PushContentPopupComponent pushContentPopupComponent = event.getSource();
      UIPopupContainer uiPopupContainer = (UIPopupContainer) pushContentPopupComponent.getAncestorOfType(UIPopupContainer.class);
      uiPopupContainer.deActivate();
      event.getRequestContext().addUIComponentToUpdateByAjax(uiPopupContainer);
    }
  }

  public void activate() {}

  public void deActivate() {}

  public String[] getActions() {
    return new String[] { "Push", "Close", "Select" };
  }

  public List<NodeComparison> getSelectedNodes() {
    return selectedNodes;
  }

  public static ResourceBundle getResourceBundle() {
    return WebuiRequestContext.getCurrentInstance().getApplicationResourceBundle();
  }

  public List<TargetServer> getTargetServers() {
    return targetServers;
  }

  public void setSynchronizationService(SynchronizationService synchronizationService) {
    synchronizationService_ = synchronizationService;
  }

  public void setCurrentPath(String currentPath) {
    this.currentPath = currentPath;
  }

  public String getCurrentPath() {
    return this.currentPath;
  }

  public String getWorkspace() {
    return workspace;
  }

  public void setWorkspace(String workspace) {
    this.workspace = workspace;
  }

  public List<NodeComparison> getDefaultSelection() {
    return defaultSelection;
  }

  public SelectNodesComponent getSelectNodesComponent() {
    return selectNodesComponent;
  }
}