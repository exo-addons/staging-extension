package org.exoplatform.management.service.impl;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.exoplatform.portal.config.UserACL;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.wcm.webui.Utils;
import org.picocontainer.Startable;

@Path("/staging/message")
public class StagingMessageREST implements ResourceContainer, Startable {

  private static final Log log = ExoLogger.getLogger(StagingServiceImpl.class);
  private UserACL userACL;

  public StagingMessageREST(UserACL userACL) {
    this.userACL = userACL;
  }

  @GET
  @Path("get")
  @Produces("text/html")
  public String getMessageTodisplayInUI() {
    try {
      if (Utils.isAdministratorUser() || userACL.isUserInGroup("/platform/web-contributors")) {
        return System.getProperty("exo.staging.ui.message", "").trim();
      }
    } catch (Exception e) {
      log.error(e);
    }
    return "";
  }

  @Override
  public void start() {

  }

  @Override
  public void stop() {}
}