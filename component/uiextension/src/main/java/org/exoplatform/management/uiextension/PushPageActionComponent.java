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
@ComponentConfig(template = "classpath:groovy/webui/component/staging/page/PushPageActionComponent.gtmpl", events = { @EventConfig(listeners = PushPageActionComponent.PushPageActionListener.class) })
public class PushPageActionComponent extends UIContainer {

  private static final String PERMISSIONS_VARIABLE = "exo.staging.page.button.permissions";

  private static SynchronizationService SYNCHRONIZATION_SERVICE;
  private static boolean servicesStarted = false;

  public PushPageActionComponent() {
    if (!servicesStarted) {
      SYNCHRONIZATION_SERVICE = getApplicationComponent(SynchronizationService.class);
      servicesStarted = true;
    }
  }

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

  public static boolean isShowButton() {
    return Utils.hasPushButtonPermission(PERMISSIONS_VARIABLE);
  }

  public static class PushPageActionListener extends EventListener<PushPageActionComponent> {
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