package org.exoplatform.management.uiextension;

import java.util.ArrayList;
import java.util.List;

import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.management.service.api.SynchronizationService;
import org.exoplatform.management.service.api.TargetServer;
import org.exoplatform.management.service.handler.content.SiteContentsHandler;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIPopupComponent;
import org.exoplatform.webui.core.model.SelectItemOption;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormSelectBox;
import org.exoplatform.webui.form.validator.MandatoryValidator;

/**
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
@ComponentConfig(
  template = "classpath:/groovy/webui/component/explorer/popup/staging/PushContent.gtmpl",
  events =
    { @EventConfig(
      listeners = PushContentPopupContainer.CloseActionListener.class), @EventConfig(
      listeners = PushContentPopupContainer.PushActionListener.class) })
public class PushContentPopupContainer extends UIForm implements UIPopupComponent {
  private static final Log LOG = ExoLogger.getLogger(PushContentPopupContainer.class.getName());

  private static final String TARGET_SERVER_NAME = "targetServer";

  private SiteContentsHandler contentsHandler_;
  private SynchronizationService synchronizationService_;

  private List<TargetServer> targetServers;
  private String currentPath;

  public PushContentPopupContainer() throws Exception {
    List<SelectItemOption<String>> itemOptions = new ArrayList<SelectItemOption<String>>();
    targetServers = synchronizationService_.getSynchonizationServers();
    for (TargetServer targetServer : targetServers) {
      SelectItemOption<String> selectItemOption = new SelectItemOption<String>(targetServer.getName(), "" + targetServer.hashCode());
      itemOptions.add(selectItemOption);
    }

    addUIFormInput(new UIFormSelectBox(TARGET_SERVER_NAME, TARGET_SERVER_NAME, itemOptions).addValidator(MandatoryValidator.class));
  }

  static public class PushActionListener extends EventListener<PushContentPopupContainer> {
    public void execute(Event<PushContentPopupContainer> event) throws Exception {
      PushContentPopupContainer pushContentPopupContainer = event.getSource();
      // String sqlQueryFilter =
      // "query:select * from nt:base where jcr:path like '" +
      // pushContentPopupContainer.getCurrentPath() + "'";

      // TODO make synchronization
      // pushContentPopupContainer.getContentsHandler().synchronize(resources,
      // exportOptions, importOptions, targetServer);

      LOG.info("Synchronization of '" + pushContentPopupContainer.getCurrentPath() + "' done.");
    }
  }

  static public class CloseActionListener extends EventListener<PushContentPopupContainer> {
    public void execute(Event<PushContentPopupContainer> event) throws Exception {
      UIJCRExplorer uiExplorer = event.getSource().getAncestorOfType(UIJCRExplorer.class);
      uiExplorer.cancelAction();
    }
  }

  public void activate() {
  }

  public void deActivate() {
  }

  public String[] getActions() {
    return new String[]
      { "Push", "Close" };
  }

  public List<TargetServer> getTargetServers() {
    return targetServers;
  }

  public void setContentsHandler(SiteContentsHandler contentsHandler) {
    contentsHandler_ = contentsHandler;
  }

  public SiteContentsHandler getContentsHandler() {
    return this.contentsHandler_;
  }

  public void setSynchronizationService(SynchronizationService synchronizationService) {
    synchronizationService_ = synchronizationService;
  }

  public void setCurrentPath(String currentPath) {
    this.currentPath = currentPath;
  }

  public String getCurrentPath() {
    return this.currentPath;
  }
}
