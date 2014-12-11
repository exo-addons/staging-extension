package org.exoplatform.management.uiextension;

import java.net.ConnectException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import org.exoplatform.management.service.api.Resource;
import org.exoplatform.management.service.api.StagingService;
import org.exoplatform.management.service.api.SynchronizationService;
import org.exoplatform.management.service.api.TargetServer;
import org.exoplatform.management.service.handler.ResourceHandlerLocator;
import org.exoplatform.management.service.handler.mop.MOPSiteHandler;
import org.exoplatform.portal.mop.user.UserNode;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.wcm.webui.Utils;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
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

@ComponentConfig(
  lifecycle = UIFormLifecycle.class,
  template = "classpath:groovy/webui/component/explorer/popup/staging/PushSite.gtmpl",
  events = { @EventConfig(
    listeners = PushSiteForm.CloseActionListener.class), @EventConfig(
    listeners = PushSiteForm.PushActionListener.class) })
public class PushSiteForm extends UIForm {
  private static final Log LOG = ExoLogger.getLogger(PushSiteForm.class.getName());

  protected static MOPSiteHandler SITE_HANDLER = (MOPSiteHandler) ResourceHandlerLocator.getResourceHandler(StagingService.SITES_PARENT_PATH);;

  public static final String POPUP_WINDOW = "PushSitePopupWindow";

  private static final String TARGET_SERVER_NAME_FIELD_NAME = "targetServer";

  private static final String INFO_FIELD_NAME = "info";

  private SynchronizationService synchronizationService;

  private List<TargetServer> targetServers;

  public PushSiteForm() throws Exception {
    addUIFormInput(new UIFormInputInfo(INFO_FIELD_NAME, INFO_FIELD_NAME, ""));
    addUIFormInput(new UIFormSelectBox(TARGET_SERVER_NAME_FIELD_NAME, TARGET_SERVER_NAME_FIELD_NAME, new ArrayList<SelectItemOption<String>>()));
  }

  public void init() throws Exception {
    List<SelectItemOption<String>> itemOptions = getChild(UIFormSelectBox.class).getOptions();
    itemOptions.clear();
    try {
      targetServers = synchronizationService.getSynchonizationServers();
    } catch (Exception e) {
      LOG.warn(e);
      targetServers = new ArrayList<TargetServer>();
    }
    for (TargetServer targetServer : targetServers) {
      SelectItemOption<String> selectItemOption = new SelectItemOption<String>(targetServer.getName(), targetServer.getId());
      itemOptions.add(selectItemOption);
    }
  }

  public String[] getActions() {
    return new String[] { "Push", "Close" };
  }

  public List<TargetServer> getTargetServers() {
    return targetServers;
  }

  public ResourceBundle getResourceBundle() {
    return WebuiRequestContext.getCurrentInstance().getApplicationResourceBundle();
  }

  public void setSynchronizationService(SynchronizationService synchronizationService) {
    this.synchronizationService = synchronizationService;
  }

  private static void closePopup(Event<PushSiteForm> event) {
    PushSiteForm pushSiteForm = event.getSource();
    UIPopupContainer popupContainer = pushSiteForm.getAncestorOfType(UIPopupContainer.class);
    if (popupContainer != null)
      popupContainer.removeChildById(POPUP_WINDOW);
    event.getRequestContext().addUIComponentToUpdateByAjax(popupContainer);
  }

  static public class CloseActionListener extends EventListener<PushSiteForm> {
    public void execute(Event<PushSiteForm> event) throws Exception {
      closePopup(event);
    }
  }

  static public class PushActionListener extends EventListener<PushSiteForm> {
    public void execute(Event<PushSiteForm> event) throws Exception {
      PushSiteForm pushSiteForm = event.getSource();
      ResourceBundle resourceBundle = pushSiteForm.getResourceBundle();

      pushSiteForm.getUIFormInputInfo(INFO_FIELD_NAME).setValue(null);
      try {
        // get target server
        TargetServer targetServer = getTargetServer(pushSiteForm);
        if (targetServer == null) {
          pushSiteForm.getUIFormInputInfo(INFO_FIELD_NAME).setValue(resourceBundle.getString("PushSite.msg.targetServerMandatory"));
          return;
        }

        // Synchronize Site
        synchronizeSite(targetServer);
        LOG.info("Synchronization of site '" + Util.getUIPortal().getLabel() + "' is done.");

        // Update UI
        Utils.createPopupMessage(pushSiteForm, "PushSite.msg.synchronizationDone", null, ApplicationMessage.INFO);
        closePopup(event);
      } catch (Exception ex) {
        if (isConnectionException(ex)) {
          pushSiteForm.getUIFormInputInfo(INFO_FIELD_NAME).setValue(resourceBundle.getString("PushSite.msg.unableToConnect"));
        } else {
          pushSiteForm.getUIFormInputInfo(INFO_FIELD_NAME).setValue(resourceBundle.getString("PushSite.msg.synchronizationError"));
        }
        LOG.error("Synchronization of site '" + Util.getUIPortal().getLabel() + "' failed:", ex);
      }
    }

    private void synchronizeSite(TargetServer targetServer) throws Exception {
      UserNode userNode = Util.getUIPortal().getSelectedUserNode();
      String siteType = userNode.getPageRef().getSite().getType().getName();
      String siteName = userNode.getPageRef().getSite().getName();

      List<Resource> resources = new ArrayList<Resource>();
      Map<String, String> exportOptions = new HashMap<String, String>();
      Map<String, String> importOptions = new HashMap<String, String>();

      // Synchronize site
      resources.add(new Resource("/site/" + siteType + "sites/" + siteName, "Site", "Site"));
      SITE_HANDLER.synchronize(resources, exportOptions, importOptions, targetServer);
    }

    private TargetServer getTargetServer(PushSiteForm pushSiteForm) {
      TargetServer targetServer = null;
      String targetServerId = pushSiteForm.getUIFormSelectBox(TARGET_SERVER_NAME_FIELD_NAME).getValue();
      Iterator<TargetServer> iterator = pushSiteForm.getTargetServers().iterator();
      while (iterator.hasNext() && targetServer == null) {
        TargetServer itTargetServer = iterator.next();
        if (itTargetServer.getId().equals(targetServerId)) {
          targetServer = itTargetServer;
        }
      }
      return targetServer;
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
  }
}