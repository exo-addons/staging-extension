package org.exoplatform.management.uiextension;

import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.component.explorer.UIWorkingArea;
import org.exoplatform.ecm.webui.component.explorer.control.UIActionBar;
import org.exoplatform.ecm.webui.component.explorer.control.listener.UIActionBarActionListener;
import org.exoplatform.management.service.api.ResourceHandler;
import org.exoplatform.management.service.api.StagingService;
import org.exoplatform.management.service.api.SynchronizationService;
import org.exoplatform.management.service.handler.ResourceHandlerLocator;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIPopupContainer;
import org.exoplatform.webui.event.Event;

/**
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
@ComponentConfig(
  events =
    { @EventConfig(
      listeners = PushContentActionComponent.PushContentActionListener.class) })
public class PushContentActionComponent extends UIComponent {

  private static ResourceHandler CONTENTS_HANDLER;
  private static SynchronizationService SYNCHRONIZATION_SERVICE;
  private static boolean servicesStarted = false;

  public PushContentActionComponent() {
    if (!servicesStarted) {
      CONTENTS_HANDLER = ResourceHandlerLocator.getResourceHandler(StagingService.CONTENT_SITES_PATH);
      SYNCHRONIZATION_SERVICE = getApplicationComponent(SynchronizationService.class);
      servicesStarted = true;
    }
  }

  public static class PushContentActionListener extends UIActionBarActionListener<PushContentActionComponent> {
    public void processEvent(Event<PushContentActionComponent> event) throws Exception {
      UIJCRExplorer uiExplorer = event.getSource().getAncestorOfType(UIJCRExplorer.class);
      UIPopupContainer uiPopupContainer = uiExplorer.getChild(UIPopupContainer.class);

      UIWorkingArea uiWorkingArea = uiExplorer.getChild(UIWorkingArea.class);
      UIActionBar uiActionBar = uiWorkingArea.getChild(UIActionBar.class);
      PushContentPopupComponent pushContentPopupContainer = uiActionBar.createUIComponent(PushContentPopupComponent.class, null, null);

      pushContentPopupContainer.setContentsHandler(CONTENTS_HANDLER);
      pushContentPopupContainer.setSynchronizationService(SYNCHRONIZATION_SERVICE);
      pushContentPopupContainer.setCurrentPath(uiExplorer.getCurrentPath());
      pushContentPopupContainer.init();

      uiPopupContainer.activate(pushContentPopupContainer, 700, 0);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiPopupContainer);
    }
  }

}