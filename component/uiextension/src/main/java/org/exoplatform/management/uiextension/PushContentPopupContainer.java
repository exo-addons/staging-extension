package org.exoplatform.management.uiextension;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.management.service.api.Resource;
import org.exoplatform.management.service.api.SynchronizationService;
import org.exoplatform.management.service.api.TargetServer;
import org.exoplatform.management.service.handler.content.SiteContentsHandler;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIPopupComponent;
import org.exoplatform.webui.core.model.SelectItemOption;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormSelectBox;
import org.exoplatform.webui.form.UIFormStringInput;
import org.exoplatform.webui.form.validator.MandatoryValidator;

/**
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
@ComponentConfig(
  template = "classpath:groovy/webui/component/explorer/popup/staging/PushContent.gtmpl",
  events =
    { @EventConfig(
      listeners = PushContentPopupContainer.CloseActionListener.class), @EventConfig(
      listeners = PushContentPopupContainer.PushActionListener.class) })
public class PushContentPopupContainer extends UIForm implements UIPopupComponent {
  private static final Log LOG = ExoLogger.getLogger(PushContentPopupContainer.class.getName());

  private static final String TARGET_SERVER_NAME_FIELD_NAME = "targetServer";
  private static final String USERNAME_FIELD_NAME = "username";
  private static final String PWD_FIELD_NAME = "password";

  private SiteContentsHandler contentsHandler_;
  private SynchronizationService synchronizationService_;

  private List<TargetServer> targetServers;
  private String currentPath;

  public PushContentPopupContainer() throws Exception {
  }

  public void init() throws Exception {
    List<SelectItemOption<String>> itemOptions = new ArrayList<SelectItemOption<String>>();
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
    addUIFormInput(new UIFormSelectBox(TARGET_SERVER_NAME_FIELD_NAME, TARGET_SERVER_NAME_FIELD_NAME, itemOptions).addValidator(MandatoryValidator.class));
    addUIFormInput(new UIFormStringInput(USERNAME_FIELD_NAME, USERNAME_FIELD_NAME, ""));
    UIFormStringInput pwdInput = new UIFormStringInput(PWD_FIELD_NAME, PWD_FIELD_NAME, "");
    pwdInput.setType(UIFormStringInput.PASSWORD_TYPE);
    addUIFormInput(pwdInput);
  }

  static public class PushActionListener extends EventListener<PushContentPopupContainer> {
    public void execute(Event<PushContentPopupContainer> event) throws Exception {
      PushContentPopupContainer pushContentPopupContainer = event.getSource();
      UIJCRExplorer uiExplorer = pushContentPopupContainer.getAncestorOfType(UIJCRExplorer.class);
      UIApplication uiApp = pushContentPopupContainer.getAncestorOfType(UIApplication.class);
      try {
        List<Resource> resources = new ArrayList<Resource>();
        resources.add(new Resource(pushContentPopupContainer.getContentsHandler().getPath() + "/shared", "shared", "shared"));

        Map<String, String> exportOptions = new HashMap<String, String>();
        String sqlQueryFilter = "query:select * from nt:base where jcr:path like '" + pushContentPopupContainer.getCurrentPath() + "'";
        exportOptions.put("filter/query", sqlQueryFilter);
        exportOptions.put("filter/taxonomy:false", "false");

        Map<String, String> importOptions = new HashMap<String, String>();
        importOptions.put("filter/cleanPublication", "true");

        TargetServer targetServer = null;
        String targetServerId = pushContentPopupContainer.getUIFormSelectBox(TARGET_SERVER_NAME_FIELD_NAME).getValue();
        Iterator<TargetServer> iterator = pushContentPopupContainer.getTargetServers().iterator();

        while (iterator.hasNext() && targetServer == null) {
          TargetServer itTargetServer = iterator.next();
          if (itTargetServer.getId().equals(targetServerId)) {
            targetServer = itTargetServer;
          }
        }
        if (targetServer == null) {
          uiApp.addMessage(new ApplicationMessage("PushContent.msg.targetServerMandatory", null, ApplicationMessage.ERROR));
          return;
        }

        String password = pushContentPopupContainer.getUIStringInput(PWD_FIELD_NAME).getValue();
        String username = pushContentPopupContainer.getUIStringInput(USERNAME_FIELD_NAME).getValue();
        targetServer.setUsername(username);
        targetServer.setPassword(password);

        pushContentPopupContainer.getContentsHandler().synchronize(resources, exportOptions, importOptions, targetServer);
      } catch (Exception ex) {
        LOG.error("Synchronization of '" + pushContentPopupContainer.getCurrentPath() + "' error:", ex);
        uiApp.addMessage(new ApplicationMessage("PushContent.msg.synchronizationError", null, ApplicationMessage.ERROR));
        return;
      }
      uiExplorer.cancelAction();
      LOG.info("Synchronization of '" + pushContentPopupContainer.getCurrentPath() + "' done.");
    }
  }

  static public class CloseActionListener extends EventListener<PushContentPopupContainer> {
    public void execute(Event<PushContentPopupContainer> event) throws Exception {
      UIJCRExplorer uiExplorer = event.getSource().getAncestorOfType(UIJCRExplorer.class);
      uiExplorer.cancelAction();
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

  public void setContentsHandler(SiteContentsHandler contentsHandler) {
    contentsHandler_ = contentsHandler;
  }

  public SiteContentsHandler getContentsHandler() {
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