package org.exoplatform.management.uiextension;

import java.net.ConnectException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
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
import org.exoplatform.management.service.api.StagingService;
import org.exoplatform.management.service.api.SynchronizationService;
import org.exoplatform.management.service.api.TargetServer;
import org.exoplatform.management.service.handler.ResourceHandlerLocator;
import org.exoplatform.management.service.handler.content.SiteContentsHandler;
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

@ComponentConfigs({
    @ComponentConfig(type = UIGrid.class, id = "selectedNodesGrid", template = "classpath:groovy/webui/component/explorer/popup/staging/UISelectedNodesGrid.gtmpl"),
    @ComponentConfig(lifecycle = UIFormLifecycle.class, template = "classpath:groovy/webui/component/explorer/popup/staging/PushContent.gtmpl", events = {
        @EventConfig(listeners = PushContentPopupComponent.CloseActionListener.class), @EventConfig(listeners = PushContentPopupComponent.PushActionListener.class),
        @EventConfig(listeners = PushContentPopupComponent.SelectActionListener.class) }) })
public class PushContentPopupComponent extends UIForm implements UIPopupComponent {
  private static final Log LOG = ExoLogger.getLogger(PushContentPopupComponent.class.getName());

  public static final String INFO_FIELD_NAME = "info";
  private static final String CLEANUP_PUBLICATION = "exo.staging.explorer.content.noVersion";

  public static final String TARGET_SERVER_NAME_FIELD_NAME = "targetServer";

  protected static SiteContentsHandler CONTENTS_HANDLER = (SiteContentsHandler) ResourceHandlerLocator.getResourceHandler(StagingService.CONTENT_SITES_PATH);;

  private boolean synchronizationStarted = false;
  private boolean synchronizationFinished = true;
  private Throwable synchronizationError = null;

  private List<String> synchronizedContents = new ArrayList<String>();
  private List<String> notSynchronizedContents = new ArrayList<String>();

  // public static final String PUBLISH_FIELD_NAME = "publishOnTarget";

  private List<NodeComparison> defaultSelection = Collections.emptyList();
  private SynchronizationService synchronizationService_;

  String currentNodePath = "";
  int currentNodesCount = 0;

  String stateString;
  Calendar modifiedDateFilter = null;
  String filterString = null;
  boolean publishedContentOnly;

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
    itemOptions.add(new SelectItemOption<String>(""));
    getChild(UIFormSelectBox.class).setOnChange("Select");
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
    selectedNodes.clear();
    selectNodesComponent.getSelectedNodesGrid().getUIPageIterator().setPageList(new LazyPageList<NodeComparison>(new ListAccessImpl<NodeComparison>(NodeComparison.class, defaultSelection), 5));
    synchronizationStarted = false;
    synchronizationFinished = true;
    synchronizationError = null;
    synchronizedContents.clear();
    notSynchronizedContents.clear();
  }

  public void addSelection(NodeComparison nodeComparison) {
    selectedNodes.add(nodeComparison);
  }

  public boolean isSynchronizationStarted() {
    return synchronizationStarted;
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

        if (pushContentPopupComponent.getSelectedNodes().isEmpty()) {
          ApplicationMessage message = new ApplicationMessage("PushContent.msg.noContent", null, ApplicationMessage.ERROR);
          message.setResourceBundle(getResourceBundle());
          pushContentPopupComponent.getUIFormInputInfo(INFO_FIELD_NAME).setValue(message.getMessage());
          event.getRequestContext().addUIComponentToUpdateByAjax(uiPopupContainer);
          return;
        }

        ResourceBundle resourceBundle = PushContentPopupComponent.getResourceBundle();
        pushContentPopupComponent.getUIFormInputInfo(INFO_FIELD_NAME).setValue(resourceBundle.getString("PushNavigation.msg.synchronizationInProgress")
            + (pushContentPopupComponent.currentNodesCount == 0 ? "" : (": " + pushContentPopupComponent.currentNodePath + "  (" + pushContentPopupComponent.currentNodesCount + "/" + pushContentPopupComponent.getSelectedNodes().size() + ")")));
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
                // Multiple contents was selected,
                // synchronize one by one

                for (NodeComparison nodeComparison : selectedComparisons) {
                  try {
                    pushContentPopupComponent.currentNodePath = nodeComparison.getPath();
                    pushContentPopupComponent.currentNodesCount++;

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
                      exportOptions.put("filter/query", StringEscapeUtils.unescapeHtml(sqlQueryFilter));
                      exportOptions.put("filter/taxonomy", "false");
                      exportOptions.put("filter/no-history", "" + cleanupPublication);
                      exportOptions.put("filter/workspace", pushContentPopupComponent.getWorkspace());
                    }
                    CONTENTS_HANDLER.synchronize(resources, exportOptions, importOptions, selectedServer);
                    pushContentPopupComponent.synchronizedContents.add(nodeComparison.getPath());
                  } catch (Exception e) {
                    pushContentPopupComponent.synchronizationError = e;
                    pushContentPopupComponent.notSynchronizedContents.add(nodeComparison.getPath());
                  }
                }
                LOG.info("Synchronization of '" + pushContentPopupComponent.getCurrentPath() + "' done.");
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
              try {
                if (pushContentPopupComponent.synchronizationError == null) {
                  uiApp.addMessage(new ApplicationMessage("PushContent.msg.synchronizationDone", null, ApplicationMessage.INFO));
                  for (String contentPath : pushContentPopupComponent.synchronizedContents) {
                    uiApp.addMessage(new NoI18NApplicationMessage("OK: " + contentPath, null, ApplicationMessage.INFO));
                  }
                  pushContentPopupComponent.synchronizationStarted = false;
                  // Update UI
                  uiPopupContainer.deActivate();
                } else {
                  uiApp.addMessage(new ApplicationMessage("PushContent.msg.synchronizationError", null, ApplicationMessage.INFO));
                  for (String contentPath : pushContentPopupComponent.synchronizedContents) {
                    uiApp.addMessage(new NoI18NApplicationMessage(contentPath, null, ApplicationMessage.INFO));
                  }
                  for (String contentPath : pushContentPopupComponent.notSynchronizedContents) {
                    uiApp.addMessage(new NoI18NApplicationMessage(contentPath, null, ApplicationMessage.ERROR));
                  }
                  // Update UI
                  uiPopupContainer.deActivate();
                  pushContentPopupComponent.synchronizationStarted = false;
                  pushContentPopupComponent.synchronizationError = null;
                }
              } finally {
                pushContentPopupComponent.synchronizationStarted = false;
                pushContentPopupComponent.synchronizationFinished = true;
                pushContentPopupComponent.synchronizationError = null;
                pushContentPopupComponent.synchronizedContents.clear();
                pushContentPopupComponent.notSynchronizedContents.clear();
                pushContentPopupComponent.currentNodesCount = 0;
                pushContentPopupComponent.currentNodePath = "";
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

  public boolean isSynchronizationFinished() {
    return synchronizationFinished;
  }

  public void setSynchronizationFinished(boolean synchronizationFinished) {
    this.synchronizationFinished = synchronizationFinished;
  }

  public Throwable getSynchronizationError() {
    return synchronizationError;
  }

  public void setSynchronizationError(Throwable synchronizationError) {
    this.synchronizationError = synchronizationError;
  }

}