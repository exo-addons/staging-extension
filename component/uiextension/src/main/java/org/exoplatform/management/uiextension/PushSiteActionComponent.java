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
@ComponentConfig(template = "classpath:groovy/webui/component/staging/site/PushSiteActionComponent.gtmpl", events = { @EventConfig(listeners = PushSiteActionComponent.PushSiteActionListener.class) })
public class PushSiteActionComponent extends UIContainer {

  private static final String PERMISSIONS_VARIABLE = "exo.staging.site.button.permissions";

  private static SynchronizationService SYNCHRONIZATION_SERVICE;
  private static boolean servicesStarted = false;

  public PushSiteActionComponent() {
    if (!servicesStarted) {
      SYNCHRONIZATION_SERVICE = getApplicationComponent(SynchronizationService.class);
      servicesStarted = true;
    }
  }

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

  public static boolean isShowButton() {
    return org.exoplatform.wcm.webui.Utils.isAdministratorUser() && Utils.hasPushButtonPermission(PERMISSIONS_VARIABLE);
  }

  public static class PushSiteActionListener extends EventListener<PushSiteActionComponent> {
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