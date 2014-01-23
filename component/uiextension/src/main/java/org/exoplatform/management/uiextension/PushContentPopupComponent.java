package org.exoplatform.management.uiextension;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import org.exoplatform.management.service.api.Resource;
import org.exoplatform.management.service.api.ResourceHandler;
import org.exoplatform.management.service.api.SynchronizationService;
import org.exoplatform.management.service.api.TargetServer;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
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
import org.exoplatform.webui.form.input.UICheckBoxInput;

/**
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
@ComponentConfig(
  lifecycle = UIFormLifecycle.class,
  template = "classpath:groovy/webui/component/explorer/popup/staging/PushContent.gtmpl",
  events =
    { @EventConfig(
      listeners = PushContentPopupComponent.CloseActionListener.class), @EventConfig(
      listeners = PushContentPopupComponent.PushActionListener.class) })
public class PushContentPopupComponent extends UIForm implements UIPopupComponent {
  private static final Log LOG = ExoLogger.getLogger(PushContentPopupComponent.class.getName());

  private static final String TARGET_SERVER_NAME_FIELD_NAME = "targetServer";
  private static final String USERNAME_FIELD_NAME = "username";
  private static final String PWD_FIELD_NAME = "password";
  private static final String PUBLISH_FIELD_NAME = "publishOnTarget";
  private static final String INFO_FIELD_NAME = "info";

  private ResourceHandler contentsHandler_;
  private SynchronizationService synchronizationService_;

  private List<TargetServer> targetServers;
  private String currentPath;
  private final ResourceBundle resourceBundle;

  public PushContentPopupComponent() throws Exception {
    addUIFormInput(new UIFormSelectBox(TARGET_SERVER_NAME_FIELD_NAME, TARGET_SERVER_NAME_FIELD_NAME, new ArrayList<SelectItemOption<String>>()));
    addUIFormInput(new UIFormStringInput(USERNAME_FIELD_NAME, USERNAME_FIELD_NAME, ""));
    UIFormStringInput pwdInput = new UIFormStringInput(PWD_FIELD_NAME, PWD_FIELD_NAME, "");
    pwdInput.setType(UIFormStringInput.PASSWORD_TYPE);
    addUIFormInput(pwdInput);

    addUIFormInput(new UIFormInputInfo(INFO_FIELD_NAME, INFO_FIELD_NAME, ""));
    addUIFormInput(new UICheckBoxInput(PUBLISH_FIELD_NAME, PUBLISH_FIELD_NAME, false));
    resourceBundle = WebuiRequestContext.getCurrentInstance().getApplicationResourceBundle();
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
  }

  public ResourceBundle getResourceBundle() {
    return resourceBundle;
  }

  static public class PushActionListener extends EventListener<PushContentPopupComponent> {
    public void execute(Event<PushContentPopupComponent> event) throws Exception {
      PushContentPopupComponent pushContentPopupComponent = event.getSource();
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
        boolean cleanupPublication = pushContentPopupComponent.getUICheckBoxInput(PUBLISH_FIELD_NAME).getValue();

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

        List<Resource> resources = new ArrayList<Resource>();
        resources.add(new Resource(pushContentPopupComponent.getContentsHandler().getPath() + "/shared", "shared", "shared"));

        Map<String, String> exportOptions = new HashMap<String, String>();
        String sqlQueryFilter = "query:select * from nt:base where jcr:path like '" + pushContentPopupComponent.getCurrentPath() + "'";
        exportOptions.put("filter/query", sqlQueryFilter);
        exportOptions.put("filter/taxonomy", "false");
        exportOptions.put("filter/no-history", "" + cleanupPublication);

        Map<String, String> importOptions = new HashMap<String, String>();

        importOptions.put("filter/cleanPublication", "" + cleanupPublication);

        pushContentPopupComponent.getContentsHandler().synchronize(resources, exportOptions, importOptions, targetServer);
        uiPopupContainer.deActivate();
        uiApp.addMessage(new ApplicationMessage("PushContent.msg.synchronizationError", null, ApplicationMessage.INFO));
      } catch (Exception ex) {
        if (ex != null && ex.getMessage() != null && ex.getMessage().contains("java.net.ConnectException")) {
          ApplicationMessage message = new ApplicationMessage("PushContent.msg.unableToConnect", null, ApplicationMessage.ERROR);
          message.setResourceBundle(pushContentPopupComponent.getResourceBundle());
          pushContentPopupComponent.getUIFormInputInfo(INFO_FIELD_NAME).setValue(message.getMessage());
        } else {
          ApplicationMessage message = new ApplicationMessage("PushContent.msg.synchronizationError", null, ApplicationMessage.ERROR);
          message.setResourceBundle(pushContentPopupComponent.getResourceBundle());
          pushContentPopupComponent.getUIFormInputInfo(INFO_FIELD_NAME).setValue(message.getMessage());
        }
        LOG.error("Synchronization of '" + pushContentPopupComponent.getCurrentPath() + "' error:", ex);
      }
      event.getRequestContext().addUIComponentToUpdateByAjax(uiPopupContainer);
      LOG.info("Synchronization of '" + pushContentPopupComponent.getCurrentPath() + "' done.");
    }
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

  public List<TargetServer> getTargetServers() {
    return targetServers;
  }

  public void setContentsHandler(ResourceHandler contentsHandler) {
    contentsHandler_ = contentsHandler;
  }

  public ResourceHandler getContentsHandler() {
    return this.contentsHandler_;
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
}