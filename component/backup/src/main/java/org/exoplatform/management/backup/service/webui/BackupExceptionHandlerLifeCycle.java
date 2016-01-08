package org.exoplatform.management.backup.service.webui;

import org.exoplatform.management.backup.service.BackupInProgressException;
import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.web.application.Application;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.web.application.ApplicationRequestPhaseLifecycle;
import org.exoplatform.web.application.Phase;
import org.exoplatform.web.application.RequestFailure;

public class BackupExceptionHandlerLifeCycle implements ApplicationRequestPhaseLifecycle<PortalRequestContext> {

  @Override
  public void onInit(Application app) throws Exception {}

  @Override
  public void onStartRequest(Application app, PortalRequestContext context) throws Exception {}

  @Override
  public void onFailRequest(Application app, PortalRequestContext context, RequestFailure failureType) {
    try {
      onEndRequest(app, context);
    } catch (Exception e) {
      // Nothing wrong
    }
  }

  @Override
  public void onEndRequest(Application app, PortalRequestContext context) throws Exception {
    Boolean exceptionCaught = BackupInProgressException.untreatedException.get();
    if (exceptionCaught != null && exceptionCaught) {
      context.getUIApplication().getUIPopupMessages().clearMessages();
      ApplicationMessage message = new ApplicationMessage("Operation not allowed, backup is in progress", null, ApplicationMessage.ERROR);
      context.getUIApplication().addMessage(message);
      BackupInProgressException.untreatedException.set(false);
    }
  }

  @Override
  public void onDestroy(Application app) throws Exception {}

  @Override
  public void onStartRequestPhase(Application app, PortalRequestContext context, Phase phase) {}

  @Override
  public void onEndRequestPhase(Application app, PortalRequestContext context, Phase phase) {
    try {
      onEndRequest(app, context);
    } catch (Exception e) {
      // Nothing wrong
    }
  }

}
