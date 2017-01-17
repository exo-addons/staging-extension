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

import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.component.explorer.UIWorkingArea;
import org.exoplatform.ecm.webui.component.explorer.control.UIActionBar;
import org.exoplatform.ecm.webui.component.explorer.control.filter.CanAddNodeFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsCheckedOutFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsNotEditingDocumentFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsNotLockedFilter;
import org.exoplatform.ecm.webui.component.explorer.control.listener.UIActionBarActionListener;
import org.exoplatform.management.service.api.SynchronizationService;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIPopupContainer;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.ext.filter.UIExtensionFilter;
import org.exoplatform.webui.ext.filter.UIExtensionFilters;

import java.util.Arrays;
import java.util.List;

/**
 * The Class PushContentActionComponent.
 *
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
@ComponentConfig(events = { @EventConfig(listeners = PushContentActionComponent.PushContentActionListener.class) })
public class PushContentActionComponent extends UIComponent {

  /** The synchronization service. */
  private static SynchronizationService SYNCHRONIZATION_SERVICE;
  
  /** The services started. */
  private static boolean servicesStarted = false;

  /** The Constant FILTERS. */
  private static final List<UIExtensionFilter> FILTERS = Arrays.asList(new UIExtensionFilter[] { new CanAddNodeFilter(), new IsNotLockedFilter(), new IsCheckedOutFilter(), new CanPushContentFilter(),
      new IsNotEditingDocumentFilter() });

  /**
   * Gets the filters.
   *
   * @return the filters
   */
  @UIExtensionFilters
  public List<UIExtensionFilter> getFilters() {
    return FILTERS;
  }

  /**
   * Instantiates a new push content action component.
   */
  public PushContentActionComponent() {
    if (!servicesStarted) {
      SYNCHRONIZATION_SERVICE = getApplicationComponent(SynchronizationService.class);
      servicesStarted = true;
    }
  }

  /**
   * The listener interface for receiving pushContentAction events.
   * The class that is interested in processing a pushContentAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addPushContentActionListener</code> method. When
   * the pushContentAction event occurs, that object's appropriate
   * method is invoked.
   *
   */
  public static class PushContentActionListener extends UIActionBarActionListener<PushContentActionComponent> {
    
    /**
     * {@inheritDoc}
     */
    public void processEvent(Event<PushContentActionComponent> event) throws Exception {
      UIJCRExplorer uiExplorer = event.getSource().getAncestorOfType(UIJCRExplorer.class);
      UIPopupContainer uiPopupContainer = uiExplorer.getChild(UIPopupContainer.class);

      UIWorkingArea uiWorkingArea = uiExplorer.getChild(UIWorkingArea.class);
      UIActionBar uiActionBar = uiWorkingArea.getChild(UIActionBar.class);
      PushContentPopupComponent pushContentPopupContainer = uiActionBar.getChild(PushContentPopupComponent.class);
      if (pushContentPopupContainer != null) {
        pushContentPopupContainer = uiActionBar.removeChild(PushContentPopupComponent.class);
      }
      pushContentPopupContainer = uiActionBar.createUIComponent(PushContentPopupComponent.class, null, "PushContentPopupComponent");

      pushContentPopupContainer.setSynchronizationService(SYNCHRONIZATION_SERVICE);
      pushContentPopupContainer.setCurrentPath(uiExplorer.getCurrentPath());
      pushContentPopupContainer.setWorkspace(uiExplorer.getWorkspaceName());
      pushContentPopupContainer.init();

      uiPopupContainer.activate(pushContentPopupContainer, 1024, 0);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiPopupContainer);
    }
  }

}