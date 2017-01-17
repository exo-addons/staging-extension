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

import java.net.ConnectException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * The Class PushPageForm.
 *
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */

@ComponentConfig(lifecycle = UIFormLifecycle.class, template = "classpath:groovy/webui/component/staging/page/PushPage.gtmpl", events = {
    @EventConfig(listeners = PushPageForm.CloseActionListener.class), @EventConfig(listeners = PushPageForm.PushActionListener.class) })
public class PushPageForm extends UIForm {
  
  /** The Constant LOG. */
  private static final Log LOG = ExoLogger.getLogger(PushPageForm.class.getName());

  /** The synchronization started. */
  protected boolean synchronizationStarted = false;
  
  /** The synchronization finished. */
  protected boolean synchronizationFinished = true;
  
  /** The synchronization error. */
  protected Throwable synchronizationError = null;

  /** The site handler. */
  protected static MOPSiteHandler SITE_HANDLER = (MOPSiteHandler) ResourceHandlerLocator.getResourceHandler(StagingService.SITES_PARENT_PATH);;

  /** The Constant POPUP_WINDOW. */
  public static final String POPUP_WINDOW = "PushPagePopupWindow";

  /** The Constant TARGET_SERVER_NAME_FIELD_NAME. */
  private static final String TARGET_SERVER_NAME_FIELD_NAME = "targetServer";

  /** The Constant INFO_FIELD_NAME. */
  private static final String INFO_FIELD_NAME = "info";

  /** The synchronization service. */
  private SynchronizationService synchronizationService;

  /** The target servers. */
  private List<TargetServer> targetServers;

  /** The message type. */
  String messageType = "info";

  /**
   * Instantiates a new push page form.
   *
   * @throws Exception the exception
   */
  public PushPageForm() throws Exception {
    addUIFormInput(new UIFormInputInfo(INFO_FIELD_NAME, INFO_FIELD_NAME, ""));
    addUIFormInput(new UIFormSelectBox(TARGET_SERVER_NAME_FIELD_NAME, TARGET_SERVER_NAME_FIELD_NAME, new ArrayList<SelectItemOption<String>>()));
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

  /**
   * {@inheritDoc}
   */
  public String[] getActions() {
    return new String[] { "Push", "Close" };
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
   * Close popup.
   *
   * @param event the event
   */
  private static void closePopup(Event<PushPageForm> event) {
    PushPageForm pushPageForm = event.getSource();
    UIPopupContainer popupContainer = pushPageForm.getAncestorOfType(UIPopupContainer.class);
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
   */
  static public class CloseActionListener extends EventListener<PushPageForm> {
    
    /**
     * {@inheritDoc}
     */
    public void execute(Event<PushPageForm> event) throws Exception {
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
   */
  static public class PushActionListener extends EventListener<PushPageForm> {
    
    /**
     * {@inheritDoc}
     */
    public void execute(Event<PushPageForm> event) throws Exception {
      final PushPageForm pushPageForm = event.getSource();
      ResourceBundle resourceBundle = pushPageForm.getResourceBundle();

      pushPageForm.setMessage(null, "info");
      try {
        // get target server
        final TargetServer targetServer = getTargetServer(pushPageForm);
        if (targetServer == null) {
          pushPageForm.setMessage(resourceBundle.getString("PushPage.msg.targetServerMandatory"), "error");
          return;
        }
        pushPageForm.setMessage(resourceBundle.getString("PushPage.msg.synchronizationInProgress"), "info");

        if (pushPageForm.synchronizationFinished && !pushPageForm.synchronizationStarted) {
          pushPageForm.synchronizationStarted = true;
          pushPageForm.synchronizationFinished = false;

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
                // Synchronize Page
                synchronizePage(userNode, targetServer);
                LOG.info("Synchronization of page '" + userNode.getLabel() + "' is done.");

              } catch (Exception e) {
                pushPageForm.synchronizationError = e;
              } finally {
                pushPageForm.synchronizationFinished = true;
                RequestLifeCycle.end();
              }
            }
          });
          synchronizeThread.start();
        } else {
          if (pushPageForm.synchronizationStarted) {
            if (pushPageForm.synchronizationFinished) {
              if (pushPageForm.synchronizationError == null) {
                pushPageForm.synchronizationStarted = false;
                // Update UI
                Utils.createPopupMessage(pushPageForm, "PushNavigation.msg.synchronizationDone", null, ApplicationMessage.INFO);
                closePopup(event);
              } else {
                Throwable tempException = pushPageForm.synchronizationError;
                pushPageForm.synchronizationStarted = false;
                pushPageForm.synchronizationError = null;
                throw tempException;
              }
            }
          }
        }
      } catch (Throwable ex) {
        if (isConnectionException(ex)) {
          Utils.createPopupMessage(pushPageForm, "PushPage.msg.unableToConnect", null, ApplicationMessage.ERROR);
        } else {
          Utils.createPopupMessage(pushPageForm, "PushPage.msg.synchronizationError", null, ApplicationMessage.ERROR);
        }
        closePopup(event);
        LOG.error("Synchronization of page '" + Util.getUIPortal().getSelectedUserNode().getResolvedLabel() + "' failed:", ex);
      }
    }

    /**
     * Synchronize page.
     *
     * @param userNode the user node
     * @param targetServer the target server
     * @throws Exception the exception
     */
    private void synchronizePage(UserNode userNode, TargetServer targetServer) throws Exception {
      String navuri = userNode.getURI();
      String pageName = userNode.getPageRef().getName();
      String siteType = userNode.getPageRef().getSite().getType().getName();
      String siteName = userNode.getPageRef().getSite().getName();

      List<Resource> resources = new ArrayList<Resource>();
      Map<String, String> exportOptions = new HashMap<String, String>();
      Map<String, String> importOptions = new HashMap<String, String>();

      // Synchronize page
      resources.add(new Resource("/site/" + siteType + "sites/" + siteName + "/pages/" + pageName, "Page", "Page"));
      SITE_HANDLER.synchronize(resources, exportOptions, importOptions, targetServer);

      // Synchronize navigation
      resources.clear();
      resources.add(new Resource("/site/" + siteType + "sites/" + siteName + "/navigation/" + navuri, "Navigation", "Navigation"));
      SITE_HANDLER.synchronize(resources, exportOptions, importOptions, targetServer);
    }

    /**
     * Gets the target server.
     *
     * @param pushPageForm the push page form
     * @return the target server
     */
    private TargetServer getTargetServer(PushPageForm pushPageForm) {
      TargetServer targetServer = null;
      String targetServerId = pushPageForm.getUIFormSelectBox(TARGET_SERVER_NAME_FIELD_NAME).getValue();
      Iterator<TargetServer> iterator = pushPageForm.getTargetServers().iterator();
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