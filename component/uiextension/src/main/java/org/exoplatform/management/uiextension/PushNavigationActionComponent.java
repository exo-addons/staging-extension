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
 * The Class PushNavigationActionComponent.
 *
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
@ComponentConfig(template = "classpath:groovy/webui/component/staging/navigation/PushNavigationActionComponent.gtmpl", events = { @EventConfig(
  listeners = PushNavigationActionComponent.PushNavigationActionListener.class) })
public class PushNavigationActionComponent extends UIContainer {

  /** The Constant PERMISSIONS_VARIABLE. */
  private static final String PERMISSIONS_VARIABLE = "exo.staging.navigation.button.permissions";

  /** The synchronization service. */
  private static SynchronizationService SYNCHRONIZATION_SERVICE;
  
  /** The services started. */
  private static boolean servicesStarted = false;

  /**
   * Instantiates a new push navigation action component.
   */
  public PushNavigationActionComponent() {
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
      PushNavigationActionComponent pushNavigationActionComponent = uicomponent.getChild(PushNavigationActionComponent.class);
      if (pushNavigationActionComponent == null) {
        pushNavigationActionComponent = uicomponent.addChild(PushNavigationActionComponent.class, null, null);
      }
      pushNavigationActionComponent.setRendered(true);
      uicomponent.renderChild(PushNavigationActionComponent.class);
      pushNavigationActionComponent.setRendered(false);
    }
  }

  /**
   * Checks if is show button.
   *
   * @return true, if is show button
   */
  public static boolean isShowButton() {
    return Utils.hasPushButtonPermission(PERMISSIONS_VARIABLE);
  }

  /**
   * The listener interface for receiving pushNavigationAction events.
   * The class that is interested in processing a pushNavigationAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addPushNavigationActionListener</code> method. When
   * the pushNavigationAction event occurs, that object's appropriate
   * method is invoked.
   *
   */
  public static class PushNavigationActionListener extends EventListener<PushNavigationActionComponent> {
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void execute(Event<PushNavigationActionComponent> event) throws Exception {

      PushNavigationActionComponent actionComponent = event.getSource();
      PushNavigationForm pushNavigationForm = actionComponent.createUIComponent(PushNavigationForm.class, null, null);

      pushNavigationForm.setSynchronizationService(SYNCHRONIZATION_SERVICE);
      pushNavigationForm.init();

      org.exoplatform.wcm.webui.Utils.createPopupWindow(actionComponent, pushNavigationForm, PushNavigationForm.POPUP_WINDOW, true, 640);
    }
  }

}