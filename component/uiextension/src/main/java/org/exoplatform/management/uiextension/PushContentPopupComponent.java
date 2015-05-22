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
import org.exoplatform.management.service.api.Resource;
import org.exoplatform.management.service.api.StagingService;
import org.exoplatform.management.service.api.SynchronizationService;
import org.exoplatform.management.service.api.TargetServer;
import org.exoplatform.management.service.handler.ResourceHandlerLocator;
import org.exoplatform.management.service.handler.content.SiteContentsHandler;
import org.exoplatform.management.uiextension.comparaison.NodeComparaison;
import org.exoplatform.management.uiextension.comparaison.NodeComparaisonState;
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

  protected static SiteContentsHandler CONTENTS_HANDLER = (SiteContentsHandler) ResourceHandlerLocator.getResourceHandler(StagingService.CONTENT_SITES_PATH);;

  public static final String TARGET_SERVER_NAME_FIELD_NAME = "targetServer";

  // public static final String PUBLISH_FIELD_NAME = "publishOnTarget";

  public static final String INFO_FIELD_NAME = "info";
  private static final String CLEANUP_PUBLICATION = "exo.staging.explorer.content.noVersion";

  private List<NodeComparaison> defaultSelection = new ArrayList<NodeComparaison>();
  private SynchronizationService synchronizationService_;

  String stateString = NodeComparaisonState.MODIFIED_ON_SOURCE.getKey();
  Calendar modifiedDateFilter = null;
  String filterString = null;
  boolean publishedContentOnly = true;

  private final UIFormSelectBox targetServerInput;
  private final UIFormInputInfo infoField;
  private final SelectNodesComponent selectNodesComponent;
  private final ResourceBundle resourceBundle;

  private List<TargetServer> targetServers;
  private String currentPath;
  private String workspace;

  List<NodeComparaison> selectedNodes = new ArrayList<NodeComparaison>();

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

    resourceBundle = WebuiRequestContext.getCurrentInstance().getApplicationResourceBundle();
    NodeComparaison.resourceBundle = resourceBundle;
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
    NodeComparaison nodeComparaison = new NodeComparaison();
    nodeComparaison.setTitle(resourceBundle.getString("PushContentPopupComponent.label.currentContent"));
    nodeComparaison.setPath(getCurrentPath());
    nodeComparaison.setState(NodeComparaisonState.UNKNOWN);
    defaultSelection.add(nodeComparaison);

    selectNodesComponent.getSelectedNodesGrid().getUIPageIterator().setPageList(new LazyPageList<NodeComparaison>(new ListAccessImpl<NodeComparaison>(NodeComparaison.class, defaultSelection), 5));
  }

  public void addSelection(NodeComparaison nodeComparaison) {
    selectedNodes.add(nodeComparaison);
  }

  public boolean isDefaultEntry(String path) {
    for (NodeComparaison comparaison : defaultSelection) {
      if (path.equals(comparaison.getPath())) {
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
          message.setResourceBundle(pushContentPopupComponent.getResourceBundle());
          pushContentPopupComponent.getUIFormInputInfo(INFO_FIELD_NAME).setValue(message.getMessage());
          event.getRequestContext().addUIComponentToUpdateByAjax(pushContentPopupComponent.getParent());
          return;
        }

        List<NodeComparaison> comparaisons = Utils.compareLocalNodesWithTargetServer(pushContentPopupComponent.getWorkspace(), pushContentPopupComponent.getCurrentPath(), targetServer);
        pushContentPopupComponent.getSelectNodesComponent().init();
        pushContentPopupComponent.getSelectNodesComponent().setComparaisons(comparaisons);
        pushContentPopupComponent.getSelectNodesComponent().setRendered(true);
        event.getRequestContext().addUIComponentToUpdateByAjax(pushContentPopupComponent.getParent());
      } catch (Exception ex) {
        ApplicationMessage message;
        if (isConnectionException(ex)) {
          message = new ApplicationMessage("PushContent.msg.unableToConnect", null, ApplicationMessage.ERROR);
        } else {
          message = new ApplicationMessage("PushContent.msg.synchronizationError", null, ApplicationMessage.ERROR);
        }
        message.setResourceBundle(pushContentPopupComponent.getResourceBundle());
        pushContentPopupComponent.getUIFormInputInfo(INFO_FIELD_NAME).setValue(message.getMessage());
        LOG.error("Comparaison of '" + pushContentPopupComponent.getCurrentPath() + "' failed:", ex);
      }
    }
  }

  static public class PushActionListener extends EventListener<PushContentPopupComponent> {
    public void execute(Event<PushContentPopupComponent> event) throws Exception {
      PushContentPopupComponent pushContentPopupComponent = event.getSource();
      pushContentPopupComponent.getUIFormInputInfo(INFO_FIELD_NAME).setValue(null);

      UIPopupContainer uiPopupContainer = (UIPopupContainer) pushContentPopupComponent.getAncestorOfType(UIPopupContainer.class);
      UIApplication uiApp = pushContentPopupComponent.getAncestorOfType(UIApplication.class);
      try {
        // get cleanupPublication checkbox value
        // boolean cleanupPublication =
        // pushContentPopupComponent.getUICheckBoxInput(PUBLISH_FIELD_NAME).getValue();
        boolean cleanupPublication = false;

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
          message.setResourceBundle(pushContentPopupComponent.getResourceBundle());
          pushContentPopupComponent.getUIFormInputInfo(INFO_FIELD_NAME).setValue(message.getMessage());
          event.getRequestContext().addUIComponentToUpdateByAjax(uiPopupContainer);
          return;
        }

        List<NodeComparaison> selectedComparaisons = (List<NodeComparaison>) pushContentPopupComponent.getSelectedNodes();
        // If default selection
        if (selectedComparaisons.isEmpty()) {
          List<Resource> resources = new ArrayList<Resource>();
          resources.add(new Resource(StagingService.CONTENT_SITES_PATH + "/shared", "shared", "shared"));

          Map<String, String> exportOptions = new HashMap<String, String>();
          String sqlQueryFilter = "query:select * from nt:base where jcr:path like '" + pushContentPopupComponent.getCurrentPath() + "'";
          exportOptions.put("filter/query", StringEscapeUtils.unescapeHtml(URLDecoder.decode(sqlQueryFilter, "UTF-8")));
          exportOptions.put("filter/taxonomy", "false");
          exportOptions.put("filter/no-history", "" + cleanupPublication);
          exportOptions.put("filter/workspace", pushContentPopupComponent.getWorkspace());

          Map<String, String> importOptions = new HashMap<String, String>();

          // importOptions.put("filter/cleanPublication", "" +
          // cleanupPublication);

          CONTENTS_HANDLER.synchronize(resources, exportOptions, importOptions, targetServer);
          uiPopupContainer.deActivate();
          uiApp.addMessage(new ApplicationMessage("PushContent.msg.synchronizationDone", null, ApplicationMessage.INFO));
        } else {
          // Multiple contents was selected, synchronize one by one

          for (NodeComparaison nodeComparaison : selectedComparaisons) {
            List<Resource> resources = new ArrayList<Resource>();
            resources.add(new Resource(StagingService.CONTENT_SITES_PATH + "/shared/contents", "shared", "shared"));

            Map<String, String> exportOptions = new HashMap<String, String>();
            Map<String, String> importOptions = new HashMap<String, String>();

            if (nodeComparaison.getState().equals(NodeComparaisonState.NOT_FOUND_ON_SOURCE)) {
              exportOptions.put("filter/removeNodes", nodeComparaison.getPath());
            } else {

              boolean noVersion = false;
              String noVersionString = System.getProperty(CLEANUP_PUBLICATION, null);
              if (!StringUtils.isEmpty(noVersionString)) {
                noVersion = noVersionString.trim().equals("true");
              }

              if (noVersion) {
                exportOptions.put("filter/no-history", "true");
                importOptions.put("filter/cleanPublication", "true");
              }

              String sqlQueryFilter = "query:select * from nt:base where jcr:path like '" + nodeComparaison.getPath() + "'";
              exportOptions.put("filter/query", StringEscapeUtils.unescapeHtml(URLDecoder.decode(sqlQueryFilter, "UTF-8")));
              exportOptions.put("filter/taxonomy", "false");
              exportOptions.put("filter/no-history", "" + cleanupPublication);
              exportOptions.put("filter/workspace", pushContentPopupComponent.getWorkspace());
            }
            CONTENTS_HANDLER.synchronize(resources, exportOptions, importOptions, targetServer);
          }

          uiPopupContainer.deActivate();
          uiApp.addMessage(new ApplicationMessage("PushContent.msg.synchronizationDone", null, ApplicationMessage.INFO));
        }
        LOG.info("Synchronization of '" + pushContentPopupComponent.getCurrentPath() + "' done.");
      } catch (Exception ex) {
        ApplicationMessage message;
        if (isConnectionException(ex)) {
          message = new ApplicationMessage("PushContent.msg.unableToConnect", null, ApplicationMessage.ERROR);
        } else {
          message = new ApplicationMessage("PushContent.msg.synchronizationError", null, ApplicationMessage.ERROR);
        }
        message.setResourceBundle(pushContentPopupComponent.getResourceBundle());
        pushContentPopupComponent.getUIFormInputInfo(INFO_FIELD_NAME).setValue(message.getMessage());
        LOG.error("Synchronization of '" + pushContentPopupComponent.getCurrentPath() + "' failed:", ex);
      }
      event.getRequestContext().addUIComponentToUpdateByAjax(uiPopupContainer);
    }
  }

  /**
   * Check if the exception has a ConnectionException cause
   * 
   * @param ex
   * @return
   */
  private static boolean isConnectionException(Exception ex) {
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

  public List<NodeComparaison> getSelectedNodes() {
    return selectedNodes;
  }

  public ResourceBundle getResourceBundle() {
    return resourceBundle;
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

  public List<NodeComparaison> getDefaultSelection() {
    return defaultSelection;
  }

  public SelectNodesComponent getSelectNodesComponent() {
    return selectNodesComponent;
  }
}