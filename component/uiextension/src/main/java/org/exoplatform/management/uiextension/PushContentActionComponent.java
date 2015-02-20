package org.exoplatform.management.uiextension;

import java.util.Arrays;
import java.util.List;

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

/**
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
@ComponentConfig(
  events = { @EventConfig(
    listeners = PushContentActionComponent.PushContentActionListener.class) })
public class PushContentActionComponent extends UIComponent {

  private static SynchronizationService SYNCHRONIZATION_SERVICE;
  private static boolean servicesStarted = false;

  private static final List<UIExtensionFilter> FILTERS = Arrays.asList(new UIExtensionFilter[] { new CanAddNodeFilter(), new IsNotLockedFilter(), new IsCheckedOutFilter(), new CanPushContentFilter(),
      new IsNotEditingDocumentFilter() });

  @UIExtensionFilters
  public List<UIExtensionFilter> getFilters() {
    return FILTERS;
  }

  public PushContentActionComponent() {
    if (!servicesStarted) {
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