package org.exoplatform.management.uiextension;

import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.component.explorer.UIWorkingArea;
import org.exoplatform.ecm.webui.component.explorer.control.UIActionBar;
import org.exoplatform.ecm.webui.component.explorer.control.listener.UIActionBarActionListener;
import org.exoplatform.management.service.api.SynchronizationService;
import org.exoplatform.management.service.handler.content.SiteContentsHandler;
import org.exoplatform.management.service.impl.ChromatticServiceImpl;
import org.exoplatform.management.service.impl.SynchronizationServiceImpl;
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

  private static final SiteContentsHandler CONTENTS_HANDLER = new SiteContentsHandler();
  private static final ChromatticServiceImpl CHROMATTIC_SERVICE = new ChromatticServiceImpl();
  private static final SynchronizationService SYNCHRONIZATION_SERVICE = new SynchronizationServiceImpl();
  private static boolean servicesStarted = false;

  public PushContentActionComponent() {
    if (!servicesStarted) {
      CHROMATTIC_SERVICE.init();
      SYNCHRONIZATION_SERVICE.init(CHROMATTIC_SERVICE);
    }
  }

  public static class PushContentActionListener extends UIActionBarActionListener<PushContentActionComponent> {
    public void processEvent(Event<PushContentActionComponent> event) throws Exception {
      UIJCRExplorer uiExplorer = event.getSource().getAncestorOfType(UIJCRExplorer.class);
      UIPopupContainer uiPopupContainer = uiExplorer.getChild(UIPopupContainer.class);

      UIWorkingArea uiWorkingArea = uiExplorer.getChild(UIWorkingArea.class);
      UIActionBar uiActionBar = uiWorkingArea.getChild(UIActionBar.class);
      PushContentPopupContainer pushContentPopupContainer = uiActionBar.createUIComponent(PushContentPopupContainer.class, null, null);

      pushContentPopupContainer.setContentsHandler(CONTENTS_HANDLER);
      pushContentPopupContainer.setSynchronizationService(SYNCHRONIZATION_SERVICE);
      pushContentPopupContainer.setCurrentPath(uiExplorer.getCurrentPath());
      pushContentPopupContainer.init();

      uiPopupContainer.activate(pushContentPopupContainer, 700, 0);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiPopupContainer);
    }
  }

}