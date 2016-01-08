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
  private String message = null;
  private String position = null;
  private boolean displayForAll = false;

  public StagingMessageREST(UserACL userACL) {
    this.userACL = userACL;
    readParametersFromProperties();
  }

  public void readParametersFromProperties() {
    message = System.getProperty("exo.staging.ui.message", "").trim();
    position = System.getProperty("exo.staging.ui.message.position", "bottom-left");
    String displayMessageForAll = System.getProperty("exo.staging.ui.displayMessageForAll", null);
    if (StringUtils.isNotEmpty(displayMessageForAll)) {
      displayForAll = displayMessageForAll.trim().equalsIgnoreCase("true");
    }
  }

  @GET
  @Path("get")
  @Produces("text/html")
  public String getMessageTodisplayInUI() {
    try {
      if (diplayMessageForAll() || Utils.isAdministratorUser() || userACL.isUserInGroup("/platform/web-contributors")) {
        String computedMessage = message;
        if (StringUtils.isNotEmpty(computedMessage)) {
          if (StringUtils.isNotEmpty(position)) {
            computedMessage += "@" + position;
          }
        }
        return computedMessage;
      }
    } catch (Exception e) {
      log.error(e);
    }
    return "";
  }

  private boolean diplayMessageForAll() {
    return displayForAll;
  }

  public void setDisplayForAll(boolean displayForAll) {
    this.displayForAll = displayForAll;
  }

  public boolean isDisplayForAll() {
    return displayForAll;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public String getMessage() {
    return message;
  }

  public void setPosition(String position) {
    this.position = position;
  }

  @Override
  public void start() {}

  @Override
  public void stop() {}
}