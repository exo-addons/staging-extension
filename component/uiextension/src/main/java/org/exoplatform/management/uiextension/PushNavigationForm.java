package org.exoplatform.management.uiextension;

import java.net.ConnectException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.management.service.api.Resource;
import org.exoplatform.management.service.api.StagingService;
import org.exoplatform.management.service.api.SynchronizationService;
import org.exoplatform.management.service.api.TargetServer;
import org.exoplatform.management.service.handler.ResourceHandlerLocator;
import org.exoplatform.management.service.handler.mop.MOPSiteHandler;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.mop.importer.ImportMode;
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
import org.exoplatform.webui.form.UIFormStringInput;

/**
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */

@ComponentConfig(lifecycle = UIFormLifecycle.class, template = "classpath:groovy/webui/component/staging/navigation/PushNavigation.gtmpl", events = {
    @EventConfig(listeners = PushNavigationForm.CloseActionListener.class), @EventConfig(listeners = PushNavigationForm.PushActionListener.class) })
public class PushNavigationForm extends UIForm {
  private static final Log LOG = ExoLogger.getLogger(PushNavigationForm.class.getName());

  protected static MOPSiteHandler SITE_HANDLER = (MOPSiteHandler) ResourceHandlerLocator.getResourceHandler(StagingService.SITES_PARENT_PATH);;

  protected boolean synchronizationStarted = false;
  protected boolean synchronizationFinished = true;
  protected Throwable synchronizationError = null;

  public static final String POPUP_WINDOW = "PushNavigationPopupWindow";

  private static final String TARGET_SERVER_NAME_FIELD_NAME = "targetServer";

  private static final String INFO_FIELD_NAME = "info";

  private static final String SITE_FIELD_NAME = "siteName";

  private SynchronizationService synchronizationService;

  private List<TargetServer> targetServers;

  public PushNavigationForm() throws Exception {
    addUIFormInput(new UIFormInputInfo(INFO_FIELD_NAME, INFO_FIELD_NAME, ""));
    UIFormStringInput siteNameInput = new UIFormStringInput(SITE_FIELD_NAME, SITE_FIELD_NAME, "");
    siteNameInput.setDisabled(true);
    addUIFormInput(siteNameInput);
    addUIFormInput(new UIFormSelectBox(TARGET_SERVER_NAME_FIELD_NAME, TARGET_SERVER_NAME_FIELD_NAME, new ArrayList<SelectItemOption<String>>()));
  }

  @SuppressWarnings("unchecked")
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
    String siteName = Util.getUIPortal().getSiteKey().getName();
    getUIInput(SITE_FIELD_NAME).setValue(siteName);
    if (Util.getUIPortal().getSiteKey().getType().equals(SiteType.GROUP)) {
      getUIFormInputInfo(INFO_FIELD_NAME).setValue(resolveLabel("PushNavigation.msg.groupSiteSynchronization"));
    }
  }

  public String resolveLabel(String label) throws Exception {
    WebuiRequestContext context = WebuiRequestContext.getCurrentInstance();
    ResourceBundle res = context.getApplicationResourceBundle();
    try {
      return res.getString(label);
    } catch (Exception e) {
      return label;
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

  public boolean isSynchronizationStarted() {
    return synchronizationStarted;
  }

  private static void closePopup(Event<PushNavigationForm> event) {
    PushNavigationForm pushNavigationForm = event.getSource();
    UIPopupContainer popupContainer = pushNavigationForm.getAncestorOfType(UIPopupContainer.class);
    if (popupContainer != null)
      popupContainer.removeChildById(POPUP_WINDOW);
    event.getRequestContext().addUIComponentToUpdateByAjax(popupContainer);
  }

  static public class CloseActionListener extends EventListener<PushNavigationForm> {
    public void execute(Event<PushNavigationForm> event) throws Exception {
      closePopup(event);
    }
  }

  static public class PushActionListener extends EventListener<PushNavigationForm> {
    public void execute(Event<PushNavigationForm> event) throws Exception {
      final PushNavigationForm pushNavigationForm = event.getSource();
      ResourceBundle resourceBundle = pushNavigationForm.getResourceBundle();

      pushNavigationForm.getUIFormInputInfo(INFO_FIELD_NAME).setValue(null);
      try {
        // get target server
        final TargetServer targetServer = getTargetServer(pushNavigationForm);
        if (targetServer == null) {
          pushNavigationForm.getUIFormInputInfo(INFO_FIELD_NAME).setValue(resourceBundle.getString("PushNavigation.msg.targetServerMandatory"));
          return;
        }
        pushNavigationForm.getUIFormInputInfo(INFO_FIELD_NAME).setValue(resourceBundle.getString("PushNavigation.msg.synchronizationInProgress"));

        if (pushNavigationForm.synchronizationFinished && !pushNavigationForm.synchronizationStarted) {
          pushNavigationForm.synchronizationStarted = true;
          pushNavigationForm.synchronizationFinished = false;

          final UserNode userNode = Util.getUIPortal().getSelectedUserNode();
          Thread synchronizeThread = new Thread(new Runnable() {
            @Override
            public void run() {
              // Make sure that current container is of type
              // "PortalContainer"
              ExoContainerContext.setCurrentContainer(PortalContainer.getInstance());

              // Use "PortalContainer" in current transaction
              RequestLifeCycle.begin(ExoContainerContext.getCurrentContainer());
              try {
                // Synchronize Navigation
                synchronizeNavigation(userNode, targetServer);
                LOG.info("Synchronization of Navigation done.");
              } catch (Exception e) {
                pushNavigationForm.synchronizationError = e;
              } finally {
                pushNavigationForm.synchronizationFinished = true;
                RequestLifeCycle.end();
              }
            }
          });
          synchronizeThread.start();
        } else {
          if (pushNavigationForm.synchronizationStarted) {
            if (pushNavigationForm.synchronizationFinished) {
              if (pushNavigationForm.synchronizationError == null) {
                pushNavigationForm.synchronizationStarted = false;
                // Update UI
                Utils.createPopupMessage(pushNavigationForm, "PushNavigation.msg.synchronizationDone", null, ApplicationMessage.INFO);
                closePopup(event);
              } else {
                Throwable tempException = pushNavigationForm.synchronizationError;
                pushNavigationForm.synchronizationStarted = false;
                pushNavigationForm.synchronizationError = null;
                throw tempException;
              }
            }
          }
        }
      } catch (Throwable ex) {
        if (isConnectionException(ex)) {
          Utils.createPopupMessage(pushNavigationForm, "PushNavigation.msg.unableToConnect", null, ApplicationMessage.ERROR);
        } else {
          Utils.createPopupMessage(pushNavigationForm, "PushNavigation.msg.synchronizationError", null, ApplicationMessage.ERROR);
        }
        closePopup(event);
        LOG.error("Synchronization of Navigation '" + Util.getUIPortal().getSelectedUserNode().getResolvedLabel() + "' failed:", ex);
      }
    }

    private void synchronizeNavigation(UserNode userNode, TargetServer targetServer) throws Exception {
      String siteType = userNode.getPageRef().getSite().getType().getName();
      String siteName = userNode.getPageRef().getSite().getName();

      List<Resource> resources = new ArrayList<Resource>();
      Map<String, String> exportOptions = new HashMap<String, String>();
      Map<String, String> importOptions = new HashMap<String, String>();
      importOptions.put("importMode", ImportMode.OVERWRITE.name());

      // Synchronize navigation
      resources.clear();
      resources.add(new Resource("/site/" + siteType + "sites/" + siteName + "/navigation", "Navigation", "Navigation"));
      SITE_HANDLER.synchronize(resources, exportOptions, importOptions, targetServer);
    }

    private TargetServer getTargetServer(PushNavigationForm pushNavigationForm) {
      TargetServer targetServer = null;
      String targetServerId = pushNavigationForm.getUIFormSelectBox(TARGET_SERVER_NAME_FIELD_NAME).getValue();
      Iterator<TargetServer> iterator = pushNavigationForm.getTargetServers().iterator();
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
  }
}