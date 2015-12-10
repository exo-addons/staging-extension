package org.exoplatform.management.uiextension;

import org.exoplatform.management.service.api.SynchronizationService;
import org.exoplatform.portal.webui.container.UIContainer;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

/**
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
@ComponentConfig(template = "classpath:groovy/webui/component/staging/navigation/PushNavigationActionComponent.gtmpl", events = { @EventConfig(
  listeners = PushNavigationActionComponent.PushNavigationActionListener.class) })
public class PushNavigationActionComponent extends UIContainer {

  private static final String PERMISSIONS_VARIABLE = "exo.staging.navigation.button.permissions";

  private static SynchronizationService SYNCHRONIZATION_SERVICE;
  private static boolean servicesStarted = false;

  public PushNavigationActionComponent() {
    if (!servicesStarted) {
      SYNCHRONIZATION_SERVICE = getApplicationComponent(SynchronizationService.class);
      servicesStarted = true;
    }
  }

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

  public static boolean isShowButton() {
    return Utils.hasPushButtonPermission(PERMISSIONS_VARIABLE);
  }

  public static class PushNavigationActionListener extends EventListener<PushNavigationActionComponent> {
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