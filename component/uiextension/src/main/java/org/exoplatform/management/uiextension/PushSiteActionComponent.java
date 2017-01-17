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

import org.exoplatform.management.service.api.SynchronizationService;
import org.exoplatform.portal.webui.container.UIContainer;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

/**
 * The Class PushSiteActionComponent.
 *
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
@ComponentConfig(template = "classpath:groovy/webui/component/staging/site/PushSiteActionComponent.gtmpl", events = { @EventConfig(listeners = PushSiteActionComponent.PushSiteActionListener.class) })
public class PushSiteActionComponent extends UIContainer {

  /** The Constant PERMISSIONS_VARIABLE. */
  private static final String PERMISSIONS_VARIABLE = "exo.staging.site.button.permissions";

  /** The synchronization service. */
  private static SynchronizationService SYNCHRONIZATION_SERVICE;
  
  /** The services started. */
  private static boolean servicesStarted = false;

  /**
   * Instantiates a new push site action component.
   */
  public PushSiteActionComponent() {
    if (!servicesStarted) {
      SYNCHRONIZATION_SERVICE = getApplicationComponent(SynchronizationService.class);
      servicesStarted = true;
    }
  }

  /**
   * Adds the button to component.
   *
   * @param uicomponent the uicomponent
   * @throws Exception the exception
   */
  public static void addButtonToComponent(org.exoplatform.webui.core.UIContainer uicomponent) throws Exception {
    if (isShowButton()) {
      PushSiteActionComponent pushSiteActionComponent = uicomponent.getChild(PushSiteActionComponent.class);
      if (pushSiteActionComponent == null) {
        pushSiteActionComponent = uicomponent.addChild(PushSiteActionComponent.class, null, null);
      }
      pushSiteActionComponent.setRendered(true);
      uicomponent.renderChild(PushSiteActionComponent.class);
      pushSiteActionComponent.setRendered(false);
    }
  }

  /**
   * Checks if is show button.
   *
   * @return true, if is show button
   */
  public static boolean isShowButton() {
    return org.exoplatform.wcm.webui.Utils.isAdministratorUser() && Utils.hasPushButtonPermission(PERMISSIONS_VARIABLE);
  }

  /**
   * The listener interface for receiving pushSiteAction events.
   * The class that is interested in processing a pushSiteAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addPushSiteActionListener</code> method. When
   * the pushSiteAction event occurs, that object's appropriate
   * method is invoked.
   */
  public static class PushSiteActionListener extends EventListener<PushSiteActionComponent> {
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void execute(Event<PushSiteActionComponent> event) throws Exception {
      PushSiteActionComponent actionComponent = event.getSource();
      PushSiteForm pushSiteForm = actionComponent.createUIComponent(PushSiteForm.class, null, null);

      pushSiteForm.setSynchronizationService(SYNCHRONIZATION_SERVICE);
      pushSiteForm.init();

      org.exoplatform.wcm.webui.Utils.createPopupWindow(actionComponent, pushSiteForm, PushSiteForm.POPUP_WINDOW, true, 640);
    }
  }

}