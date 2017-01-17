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
 * The Class PushPageActionComponent.
 *
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
@ComponentConfig(template = "classpath:groovy/webui/component/staging/page/PushPageActionComponent.gtmpl", events = { @EventConfig(listeners = PushPageActionComponent.PushPageActionListener.class) })
public class PushPageActionComponent extends UIContainer {

  /** The Constant PERMISSIONS_VARIABLE. */
  private static final String PERMISSIONS_VARIABLE = "exo.staging.page.button.permissions";

  /** The synchronization service. */
  private static SynchronizationService SYNCHRONIZATION_SERVICE;
  
  /** The services started. */
  private static boolean servicesStarted = false;

  /**
   * Instantiates a new push page action component.
   */
  public PushPageActionComponent() {
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
      PushPageActionComponent pushPageActionComponent = uicomponent.getChild(PushPageActionComponent.class);
      if (pushPageActionComponent == null) {
        pushPageActionComponent = uicomponent.addChild(PushPageActionComponent.class, null, null);
      }
      pushPageActionComponent.setRendered(true);
      uicomponent.renderChild(PushPageActionComponent.class);
      pushPageActionComponent.setRendered(false);
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
   * The listener interface for receiving pushPageAction events.
   * The class that is interested in processing a pushPageAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addPushPageActionListener</code> method. When
   * the pushPageAction event occurs, that object's appropriate
   * method is invoked.
   *
   */
  public static class PushPageActionListener extends EventListener<PushPageActionComponent> {
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void execute(Event<PushPageActionComponent> event) throws Exception {
      PushPageActionComponent actionComponent = event.getSource();
      PushPageForm pushPageForm = actionComponent.createUIComponent(PushPageForm.class, null, null);

      pushPageForm.setSynchronizationService(SYNCHRONIZATION_SERVICE);
      pushPageForm.init();

      org.exoplatform.wcm.webui.Utils.createPopupWindow(actionComponent, pushPageForm, PushPageForm.POPUP_WINDOW, true, 640);
    }
  }

}