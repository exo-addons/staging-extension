package org.exoplatform.management.service.impl;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.apache.commons.lang.StringUtils;
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
      if (diplayMessageForAll() || Utils.isAdministratorUser() || userACL.isUserInGroup("/platform/web-contributors")) {
         String message = System.getProperty("exo.staging.ui.message", "").trim();
		 if(StringUtils.isNotEmpty(message)) {
			String position = System.getProperty("exo.staging.ui.message.position", "bottom-left");
			if(StringUtils.isNotEmpty(position)) {
				message += "@" + position;
			}
		 }
		 return message;
      }
    } catch (Exception e) {
      log.error(e);
    }
    return "";
  }

  private boolean diplayMessageForAll() {
    String displayMessageForAll = System.getProperty("exo.staging.ui.displayMessageForAll", null);
    if (StringUtils.isNotEmpty(displayMessageForAll)) {
      return displayMessageForAll.trim().equalsIgnoreCase("true");
    }
    return false;
  }

  @Override
  public void start() {}

  @Override
  public void stop() {}
}