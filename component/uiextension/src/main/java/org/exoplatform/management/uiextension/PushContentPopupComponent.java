package org.exoplatform.management.uiextension;

import org.apache.commons.lang.StringEscapeUtils;
import org.exoplatform.commons.utils.LazyPageList;
import org.exoplatform.commons.utils.ListAccessImpl;
import org.exoplatform.management.service.api.Resource;
import org.exoplatform.management.service.api.StagingService;
import org.exoplatform.management.service.api.SynchronizationService;
import org.exoplatform.management.service.api.TargetServer;
import org.exoplatform.management.service.handler.ResourceHandlerLocator;
import org.exoplatform.management.service.handler.content.SiteContentsHandler;
import org.exoplatform.management.uiextension.coparaison.NodeComparaison;
import org.exoplatform.management.uiextension.coparaison.NodeComparaisonState;
import org.exoplatform.management.uiextension.coparaison.Utils;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.wcm.webui.core.UIPopupWindow;
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
import org.exoplatform.webui.form.UIFormStringInput;

import java.net.ConnectException;
import java.net.URLDecoder;
import java.util.*;

/**
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */

@ComponentConfigs(
  { @ComponentConfig(
    type = UIGrid.class,
    id = "selectedNodesGrid",
    template = "classpath:groovy/webui/component/explorer/popup/staging/UISelectedNodesGrid.gtmpl"), @ComponentConfig(
    lifecycle = UIFormLifecycle.class,
    template = "classpath:groovy/webui/component/explorer/popup/staging/PushContent.gtmpl",
    events =
      { @EventConfig(
        listeners = PushContentPopupComponent.CloseActionListener.class), @EventConfig(
        listeners = PushContentPopupComponent.PushActionListener.class), @EventConfig(
        listeners = PushContentPopupComponent.SelectActionListener.class), @EventConfig(
        listeners = PushContentPopupComponent.DeleteActionListener.class) }) })
public class PushContentPopupComponent extends UIForm implements UIPopupComponent {
  private static final Log LOG = ExoLogger.getLogger(PushContentPopupComponent.class.getName());

  protected static SiteContentsHandler CONTENTS_HANDLER = (SiteContentsHandler) ResourceHandlerLocator.getResourceHandler(StagingService.CONTENT_SITES_PATH);;

  private static final String TARGET_SERVER_NAME_FIELD_NAME = "targetServer";
  private static final String USERNAME_FIELD_NAME = "username";
  private static final String PWD_FIELD_NAME = "password";

  // private static final String PUBLISH_FIELD_NAME = "publishOnTarget";

  private static final String INFO_FIELD_NAME = "info";

  private List<NodeComparaison> defaultSelection = new ArrayList<NodeComparaison>();

  private SynchronizationService synchronizationService_;

  private List<TargetServer> targetServers;
  private String currentPath;
  private String workspace;
  private final ResourceBundle resourceBundle;

  List<NodeComparaison> selectedNodes = new ArrayList<NodeComparaison>();

  public static String[] SELECTED_NODES_BEAN_FIELD =
    { "title", "path" };

  public static String[] SELECTED_BEAN_ACTION =
    { "Delete" };

  private UIGrid selectedNodesGrid;

  public PushContentPopupComponent() throws Exception {
    this.addChild(UIPopupContainer.class, null, "SelectNodesPopupContainer");

    addUIFormInput(new UIFormInputInfo(INFO_FIELD_NAME, INFO_FIELD_NAME, ""));

    addUIFormInput(new UIFormSelectBox(TARGET_SERVER_NAME_FIELD_NAME, TARGET_SERVER_NAME_FIELD_NAME, new ArrayList<SelectItemOption<String>>()));
    addUIFormInput(new UIFormStringInput(USERNAME_FIELD_NAME, USERNAME_FIELD_NAME, ""));
    UIFormStringInput pwdInput = new UIFormStringInput(PWD_FIELD_NAME, PWD_FIELD_NAME, "");
    pwdInput.setType(UIFormStringInput.PASSWORD_TYPE);
    addUIFormInput(pwdInput);

    // addUIFormInput(new UICheckBoxInput(PUBLISH_FIELD_NAME,
    // PUBLISH_FIELD_NAME, false));

    selectedNodesGrid = addChild(UIGrid.class, "selectedNodesGrid", "selectedNodesGrid");
    selectedNodesGrid.configure("path", SELECTED_NODES_BEAN_FIELD, null);

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
    nodeComparaison.setTitle("Current Content");
    nodeComparaison.setPath(getCurrentPath());
    nodeComparaison.setState(NodeComparaisonState.UNKNOWN);
    defaultSelection.add(nodeComparaison);

    nodeComparaison = new NodeComparaison();
    nodeComparaison.setTitle("Sub contents of current content");
    nodeComparaison.setPath(getCurrentPath() + "/*");
    nodeComparaison.setState(NodeComparaisonState.UNKNOWN);
    defaultSelection.add(nodeComparaison);

    selectedNodesGrid.getUIPageIterator().setPageList(new LazyPageList<NodeComparaison>(new ListAccessImpl<NodeComparaison>(NodeComparaison.class, defaultSelection), 5));
  }

  public void addSelection(NodeComparaison nodeComparaison) {
    selectedNodes.add(nodeComparaison);
    selectedNodesGrid.getUIPageIterator().setPageList(new LazyPageList<NodeComparaison>(new ListAccessImpl<NodeComparaison>(NodeComparaison.class, selectedNodes), 5));
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
      } else if (uiForm instanceof SelectNodesPopupComponent) {
        pushContentPopupComponent = ((SelectNodesPopupComponent) uiForm).getPushContentPopupComponent();
      }
      pushContentPopupComponent.getUIFormInputInfo(INFO_FIELD_NAME).setValue(null);

      UIPopupContainer popupContainer = pushContentPopupComponent.getChildById("SelectNodesPopupContainer");

      UIPopupWindow popupWindow = popupContainer.getChildById("SelectNodesPopupWindow");
      if (popupWindow != null) {
        popupContainer.removeChildById("SelectNodesPopupWindow");
      }
      UIPopupWindow selectNodesPopupWindow = popupContainer.addChild(UIPopupWindow.class, null, "SelectNodesPopupWindow");
      selectNodesPopupWindow.setParent(popupContainer);
      selectNodesPopupWindow.setShow(false);

      SelectNodesPopupComponent selectNodesPopupComponent = selectNodesPopupWindow.createUIComponent(SelectNodesPopupComponent.class, null, "SelectNodesPopupComponent");
      selectNodesPopupWindow.setUIComponent(selectNodesPopupComponent);
      selectNodesPopupComponent.setParent(selectNodesPopupWindow);
      selectNodesPopupComponent.setPushContentPopupComponent(pushContentPopupComponent);

      UIPopupContainer uiPopupContainer = (UIPopupContainer) pushContentPopupComponent.getAncestorOfType(UIPopupContainer.class);
      try {
        // get username
        String username = pushContentPopupComponent.getUIStringInput(USERNAME_FIELD_NAME).getValue();
        if (username == null || username.isEmpty()) {
          ApplicationMessage message = new ApplicationMessage("PushContent.msg.userNameMandatory", null, ApplicationMessage.ERROR);
          message.setResourceBundle(pushContentPopupComponent.getResourceBundle());
          pushContentPopupComponent.getUIFormInputInfo(INFO_FIELD_NAME).setValue(message.getMessage());
          event.getRequestContext().addUIComponentToUpdateByAjax(uiPopupContainer);
          return;
        }

        // get password
        String password = pushContentPopupComponent.getUIStringInput(PWD_FIELD_NAME).getValue();
        if (password == null || password.isEmpty()) {
          ApplicationMessage message = new ApplicationMessage("PushContent.msg.passwordMandatory", null, ApplicationMessage.ERROR);
          message.setResourceBundle(pushContentPopupComponent.getResourceBundle());
          pushContentPopupComponent.getUIFormInputInfo(INFO_FIELD_NAME).setValue(message.getMessage());
          event.getRequestContext().addUIComponentToUpdateByAjax(uiPopupContainer);
          return;
        }

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
        targetServer.setUsername(username);
        targetServer.setPassword(password);

        List<NodeComparaison> comparaisons = Utils.compareLocalNodesWithTargetServer(pushContentPopupComponent.getWorkspace(), pushContentPopupComponent.getCurrentPath(), targetServer);
        selectNodesPopupComponent.setComparaisons(comparaisons);
        selectNodesPopupWindow.setShow(true);
        selectNodesPopupWindow.setWindowSize(1024, 600);
        selectNodesPopupWindow.setRendered(true);
        event.getRequestContext().addUIComponentToUpdateByAjax(selectNodesPopupWindow.getParent());
        event.getRequestContext().addUIComponentToUpdateByAjax(uiPopupContainer);
      } catch (Exception ex) {
        ApplicationMessage message;
        if(isConnectionException(ex)) {
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

  static public class DeleteActionListener extends EventListener<UIForm> {
    public void execute(Event<UIForm> event) throws Exception {
      UIForm uiForm = event.getSource();
      PushContentPopupComponent pushContentPopupComponent = null;
      if (uiForm instanceof PushContentPopupComponent) {
        pushContentPopupComponent = (PushContentPopupComponent) uiForm;
      } else if (uiForm instanceof SelectNodesPopupComponent) {
        pushContentPopupComponent = ((SelectNodesPopupComponent) uiForm).getPushContentPopupComponent();
      }
      String path = event.getRequestContext().getRequestParameter(OBJECTID);

      UIPopupContainer popupContainer = pushContentPopupComponent.getChildById("SelectNodesPopupContainer");
      SelectNodesPopupComponent selectNodesPopupComponent = null;
      try {
        UIPopupWindow popupWindow = popupContainer.getChild(UIPopupWindow.class);
        selectNodesPopupComponent = (SelectNodesPopupComponent) popupWindow.getUIComponent();
      } catch (Exception e) {
        // Nothing to do, the component not found
      }
      try {
        Iterator<NodeComparaison> comparaisons = pushContentPopupComponent.getSelectedNodes().iterator();
        boolean removed = false;
        while (!removed && comparaisons.hasNext()) {
          NodeComparaison comparaison = comparaisons.next();
          if (path.equals(comparaison.getPath())) {
            comparaisons.remove();
            removed = true;
          }
        }
        if (removed) {
          if (pushContentPopupComponent.getSelectedNodes().isEmpty()) {
            pushContentPopupComponent.getSelectedNodesGrid().getUIPageIterator()
                .setPageList(new LazyPageList<NodeComparaison>(new ListAccessImpl<NodeComparaison>(NodeComparaison.class, pushContentPopupComponent.getDefaultSelection()), 5));
          } else {
            pushContentPopupComponent.getSelectedNodesGrid().getUIPageIterator()
                .setPageList(new LazyPageList<NodeComparaison>(new ListAccessImpl<NodeComparaison>(NodeComparaison.class, pushContentPopupComponent.getSelectedNodes()), 5));
          }
        }
        if (selectNodesPopupComponent != null) {
          selectNodesPopupComponent.computeComparaisons();

          event.getRequestContext().addUIComponentToUpdateByAjax(selectNodesPopupComponent.getNodesGrid());
          event.getRequestContext().addUIComponentToUpdateByAjax(selectNodesPopupComponent.getSelectedNodesGrid());
        }
        event.getRequestContext().addUIComponentToUpdateByAjax(pushContentPopupComponent.getSelectedNodesGrid());
      } catch (Exception ex) {
        ApplicationMessage message = new ApplicationMessage("PushContent.msg.synchronizationError", null, ApplicationMessage.ERROR);
        message.setResourceBundle(pushContentPopupComponent.getResourceBundle());
        pushContentPopupComponent.getUIFormInputInfo(INFO_FIELD_NAME).setValue(message.getMessage());
        LOG.error("Error while deleting '" + path + "' from selected contents:", ex);
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
        // get username
        String username = pushContentPopupComponent.getUIStringInput(USERNAME_FIELD_NAME).getValue();
        if (username == null || username.isEmpty()) {
          ApplicationMessage message = new ApplicationMessage("PushContent.msg.userNameMandatory", null, ApplicationMessage.ERROR);
          message.setResourceBundle(pushContentPopupComponent.getResourceBundle());
          pushContentPopupComponent.getUIFormInputInfo(INFO_FIELD_NAME).setValue(message.getMessage());
          event.getRequestContext().addUIComponentToUpdateByAjax(uiPopupContainer);
          return;
        }

        // get password
        String password = pushContentPopupComponent.getUIStringInput(PWD_FIELD_NAME).getValue();
        if (password == null || password.isEmpty()) {
          ApplicationMessage message = new ApplicationMessage("PushContent.msg.passwordMandatory", null, ApplicationMessage.ERROR);
          message.setResourceBundle(pushContentPopupComponent.getResourceBundle());
          pushContentPopupComponent.getUIFormInputInfo(INFO_FIELD_NAME).setValue(message.getMessage());
          event.getRequestContext().addUIComponentToUpdateByAjax(uiPopupContainer);
          return;
        }

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
        targetServer.setUsername(username);
        targetServer.setPassword(password);

        @SuppressWarnings(
          { "unchecked", "deprecation" })
        List<NodeComparaison> selectedComparaisons = (List<NodeComparaison>) pushContentPopupComponent.getSelectedNodesGrid().getUIPageIterator().getPageList().getAll();
        // If default selection
        if (pushContentPopupComponent.getDefaultSelection().equals(selectedComparaisons)) {
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
          // Different contents was selected

          for (NodeComparaison nodeComparaison : selectedComparaisons) {
            List<Resource> resources = new ArrayList<Resource>();
            resources.add(new Resource(StagingService.CONTENT_SITES_PATH + "/shared", "shared", "shared"));

            Map<String, String> exportOptions = new HashMap<String, String>();
            String sqlQueryFilter = "query:select * from nt:base where jcr:path like '" + nodeComparaison.getPath() + "'";
            exportOptions.put("filter/query", StringEscapeUtils.unescapeHtml(URLDecoder.decode(sqlQueryFilter, "UTF-8")));
            exportOptions.put("filter/taxonomy", "false");
            exportOptions.put("filter/no-history", "" + cleanupPublication);
            exportOptions.put("filter/workspace", pushContentPopupComponent.getWorkspace());

            Map<String, String> importOptions = new HashMap<String, String>();

            // importOptions.put("filter/cleanPublication", "" +
            // cleanupPublication);

            CONTENTS_HANDLER.synchronize(resources, exportOptions, importOptions, targetServer);
          }

          uiPopupContainer.deActivate();
          uiApp.addMessage(new ApplicationMessage("PushContent.msg.synchronizationDone", null, ApplicationMessage.INFO));
        }
        LOG.info("Synchronization of '" + pushContentPopupComponent.getCurrentPath() + "' done.");
      } catch (Exception ex) {
        ApplicationMessage message;
        if(isConnectionException(ex)) {
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
   * @param ex
   * @return
   */
  private static boolean isConnectionException(Exception ex) {
    boolean connectionException = false;
    Throwable throwable = ex;
    do {
      if(throwable instanceof ConnectException) {
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

  public void activate() {
  }

  public void deActivate() {
  }

  public String[] getActions() {
    return new String[]
      { "Push", "Close" };
  }

  public List<NodeComparaison> getSelectedNodes() {
    return selectedNodes;
  }

  public UIGrid getSelectedNodesGrid() {
    return selectedNodesGrid;
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

}