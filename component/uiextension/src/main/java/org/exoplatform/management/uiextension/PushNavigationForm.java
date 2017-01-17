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

import java.net.ConnectException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * The Class PushNavigationForm.
 *
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */

@ComponentConfig(lifecycle = UIFormLifecycle.class, template = "classpath:groovy/webui/component/staging/navigation/PushNavigation.gtmpl", events = {
    @EventConfig(listeners = PushNavigationForm.CloseActionListener.class), @EventConfig(listeners = PushNavigationForm.PushActionListener.class) })
public class PushNavigationForm extends UIForm {
  
  /** The Constant LOG. */
  private static final Log LOG = ExoLogger.getLogger(PushNavigationForm.class.getName());

  /** The site handler. */
  protected static MOPSiteHandler SITE_HANDLER = (MOPSiteHandler) ResourceHandlerLocator.getResourceHandler(StagingService.SITES_PARENT_PATH);;

  /** The synchronization started. */
  protected boolean synchronizationStarted = false;
  
  /** The synchronization finished. */
  protected boolean synchronizationFinished = true;
  
  /** The synchronization error. */
  protected Throwable synchronizationError = null;

  /** The Constant POPUP_WINDOW. */
  public static final String POPUP_WINDOW = "PushNavigationPopupWindow";

  /** The Constant TARGET_SERVER_NAME_FIELD_NAME. */
  private static final String TARGET_SERVER_NAME_FIELD_NAME = "targetServer";

  /** The Constant INFO_FIELD_NAME. */
  private static final String INFO_FIELD_NAME = "info";

  /** The Constant SITE_FIELD_NAME. */
  private static final String SITE_FIELD_NAME = "siteName";

  /** The synchronization service. */
  private SynchronizationService synchronizationService;

  /** The target servers. */
  private List<TargetServer> targetServers;

  /** The message type. */
  String messageType = "info";

  /**
   * Instantiates a new push navigation form.
   *
   * @throws Exception the exception
   */
  public PushNavigationForm() throws Exception {
    addUIFormInput(new UIFormInputInfo(INFO_FIELD_NAME, INFO_FIELD_NAME, ""));
    UIFormStringInput siteNameInput = new UIFormStringInput(SITE_FIELD_NAME, SITE_FIELD_NAME, "");
    siteNameInput.setDisabled(true);
    addUIFormInput(siteNameInput);
    addUIFormInput(new UIFormSelectBox(TARGET_SERVER_NAME_FIELD_NAME, TARGET_SERVER_NAME_FIELD_NAME, new ArrayList<SelectItemOption<String>>()));
  }

  /**
   * Inits the.
   *
   * @throws Exception the exception
   */
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
      setMessage(resolveLabel("PushNavigation.msg.groupSiteSynchronization"), "warning");
    }
  }

  /**
   * Resolve label.
   *
   * @param label the label
   * @return the string
   * @throws Exception the exception
   */
  public String resolveLabel(String label) throws Exception {
    WebuiRequestContext context = WebuiRequestContext.getCurrentInstance();
    ResourceBundle res = context.getApplicationResourceBundle();
    try {
      return res.getString(label);
    } catch (Exception e) {
      return label;
    }
  }

  /**
   * {@inheritDoc}
   */
  public String[] getActions() {
    return new String[] { "Push", "Close" };
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
   * Gets the target servers.
   *
   * @return the target servers
   */
  public List<TargetServer> getTargetServers() {
    return targetServers;
  }

  /**
   * Gets the resource bundle.
   *
   * @return the resource bundle
   */
  public ResourceBundle getResourceBundle() {
    return WebuiRequestContext.getCurrentInstance().getApplicationResourceBundle();
  }

  /**
   * Sets the synchronization service.
   *
   * @param synchronizationService the new synchronization service
   */
  public void setSynchronizationService(SynchronizationService synchronizationService) {
    this.synchronizationService = synchronizationService;
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
   * Close popup.
   *
   * @param event the event
   */
  private static void closePopup(Event<PushNavigationForm> event) {
    PushNavigationForm pushNavigationForm = event.getSource();
    UIPopupContainer popupContainer = pushNavigationForm.getAncestorOfType(UIPopupContainer.class);
    if (popupContainer != null)
      popupContainer.removeChildById(POPUP_WINDOW);
    event.getRequestContext().addUIComponentToUpdateByAjax(popupContainer);
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
  static public class CloseActionListener extends EventListener<PushNavigationForm> {
    
    /**
     * {@inheritDoc}
     */
    public void execute(Event<PushNavigationForm> event) throws Exception {
      closePopup(event);
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
  static public class PushActionListener extends EventListener<PushNavigationForm> {
    
    /**
     * {@inheritDoc}
     */
    public void execute(Event<PushNavigationForm> event) throws Exception {
      final PushNavigationForm pushNavigationForm = event.getSource();
      ResourceBundle resourceBundle = pushNavigationForm.getResourceBundle();

      pushNavigationForm.setMessage(null, "info");
      try {
        // get target server
        final TargetServer targetServer = getTargetServer(pushNavigationForm);
        if (targetServer == null) {
          pushNavigationForm.setMessage(resourceBundle.getString("PushNavigation.msg.targetServerMandatory"), "error");
          return;
        }
        pushNavigationForm.setMessage(resourceBundle.getString("PushNavigation.msg.synchronizationInProgress"), "info");

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

    /**
     * Synchronize navigation.
     *
     * @param userNode the user node
     * @param targetServer the target server
     * @throws Exception the exception
     */
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

    /**
     * Gets the target server.
     *
     * @param pushNavigationForm the push navigation form
     * @return the target server
     */
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
  }
}