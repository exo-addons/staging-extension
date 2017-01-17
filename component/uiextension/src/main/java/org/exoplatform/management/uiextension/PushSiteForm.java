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
 * The Class PushSiteForm.
 *
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */

@ComponentConfig(lifecycle = UIFormLifecycle.class, template = "classpath:groovy/webui/component/staging/site/PushSite.gtmpl", events = {
    @EventConfig(listeners = PushSiteForm.CloseActionListener.class), @EventConfig(listeners = PushSiteForm.PushActionListener.class) })
public class PushSiteForm extends UIForm {
  
  /** The Constant LOG. */
  private static final Log LOG = ExoLogger.getLogger(PushSiteForm.class.getName());

  /** The synchronization started. */
  protected boolean synchronizationStarted = false;
  
  /** The synchronization finished. */
  protected boolean synchronizationFinished = true;
  
  /** The synchronization error. */
  protected Throwable synchronizationError = null;

  /** The site handler. */
  protected static MOPSiteHandler SITE_HANDLER = (MOPSiteHandler) ResourceHandlerLocator.getResourceHandler(StagingService.SITES_PARENT_PATH);;

  /** The Constant POPUP_WINDOW. */
  public static final String POPUP_WINDOW = "PushSitePopupWindow";

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
   * Instantiates a new push site form.
   *
   * @throws Exception the exception
   */
  public PushSiteForm() throws Exception {
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
   * @param pushSiteForm the push site form
   * @param context the context
   */
  private static void closePopup(PushSiteForm pushSiteForm, WebuiRequestContext context) {
    UIPopupContainer popupContainer = pushSiteForm.getAncestorOfType(UIPopupContainer.class);
    if (popupContainer != null)
      popupContainer.removeChildById(POPUP_WINDOW);
    context.addUIComponentToUpdateByAjax(popupContainer);
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
  static public class CloseActionListener extends EventListener<PushSiteForm> {
    
    /**
     * {@inheritDoc}
     */
    public void execute(Event<PushSiteForm> event) throws Exception {
      closePopup(event.getSource(), event.getRequestContext());
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
  static public class PushActionListener extends EventListener<PushSiteForm> {
    
    /**
     * {@inheritDoc}
     */
    public void execute(Event<PushSiteForm> event) throws Exception {
      final PushSiteForm pushSiteForm = event.getSource();
      ResourceBundle resourceBundle = pushSiteForm.getResourceBundle();

      pushSiteForm.setMessage(null, "info");
      try {
        // get target server
        final TargetServer targetServer = getTargetServer(pushSiteForm);
        if (targetServer == null) {
          pushSiteForm.setMessage(resourceBundle.getString("PushSite.msg.targetServerMandatory"), "error");
          return;
        }

        pushSiteForm.setMessage(resourceBundle.getString("PushSite.msg.synchronizationInProgress"), "info");

        if (pushSiteForm.synchronizationFinished && !pushSiteForm.synchronizationStarted) {
          pushSiteForm.synchronizationStarted = true;
          pushSiteForm.synchronizationFinished = false;

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
                // Synchronize Site
                synchronizeSite(userNode, targetServer);
                LOG.info("Synchronization of site '" + userNode.getPageRef().getSite().getName() + "' is done.");
              } catch (Exception e) {
                pushSiteForm.synchronizationError = e;
              } finally {
                pushSiteForm.synchronizationFinished = true;
                RequestLifeCycle.end();
              }
            }
          });
          synchronizeThread.start();
        } else {
          if (pushSiteForm.synchronizationStarted) {
            if (pushSiteForm.synchronizationFinished) {
              if (pushSiteForm.synchronizationError == null) {
                pushSiteForm.synchronizationStarted = false;
                // Update UI
                Utils.createPopupMessage(pushSiteForm, "PushSite.msg.synchronizationDone", null, ApplicationMessage.INFO);
                closePopup(event.getSource(), event.getRequestContext());
              } else {
                Throwable tempException = pushSiteForm.synchronizationError;
                pushSiteForm.synchronizationError = null;
                pushSiteForm.synchronizationStarted = false;
                throw tempException;
              }
            }
          }
        }
      } catch (Throwable ex) {
        if (isConnectionException(ex)) {
          Utils.createPopupMessage(pushSiteForm, "PushSite.msg.unableToConnect", null, ApplicationMessage.ERROR);
        } else {
          Utils.createPopupMessage(pushSiteForm, "PushSite.msg.synchronizationError", null, ApplicationMessage.ERROR);
        }
        closePopup(event.getSource(), event.getRequestContext());
        LOG.error("Synchronization of site '" + Util.getUIPortal().getLabel() + "' failed:", ex);
      }
    }

    /**
     * Synchronize site.
     *
     * @param userNode the user node
     * @param targetServer the target server
     * @throws Exception the exception
     */
    private void synchronizeSite(UserNode userNode, TargetServer targetServer) throws Exception {
      String siteType = userNode.getPageRef().getSite().getType().getName();
      String siteName = userNode.getPageRef().getSite().getName();

      List<Resource> resources = new ArrayList<Resource>();
      Map<String, String> exportOptions = new HashMap<String, String>();
      Map<String, String> importOptions = new HashMap<String, String>();

      // Synchronize site
      resources.add(new Resource("/site/" + siteType + "sites/" + siteName, "Site", "Site"));
      SITE_HANDLER.synchronize(resources, exportOptions, importOptions, targetServer);
    }

    /**
     * Gets the target server.
     *
     * @param pushSiteForm the push site form
     * @return the target server
     */
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