/*
 * Copyright (C) 2003-2017 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.exoplatform.management.service.impl;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.wcm.webui.Utils;
import org.picocontainer.Startable;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

/**
 * The Class StagingMessageREST.
 */
@Path("/staging/message")
public class StagingMessageREST implements ResourceContainer, Startable {

  /** The Constant log. */
  private static final Log log = ExoLogger.getLogger(StagingServiceImpl.class);

  /** The user ACL. */
  private UserACL userACL;
  
  /** The message. */
  private String message = null;
  
  /** The position. */
  private String position = null;
  
  /** The display for all. */
  private boolean displayForAll = false;

  /**
   * Instantiates a new staging message REST.
   *
   * @param userACL the user ACL
   */
  public StagingMessageREST(UserACL userACL) {
    this.userACL = userACL;
    readParametersFromProperties();
  }

  /**
   * Read parameters from properties.
   */
  public void readParametersFromProperties() {
    message = System.getProperty("exo.staging.ui.message", "").trim();
    position = System.getProperty("exo.staging.ui.message.position", "bottom-left");
    String displayMessageForAll = System.getProperty("exo.staging.ui.displayMessageForAll", null);
    if (StringUtils.isNotEmpty(displayMessageForAll)) {
      displayForAll = displayMessageForAll.trim().equalsIgnoreCase("true");
    }
  }

  /**
   * Gets the message todisplay in UI.
   *
   * @return the message todisplay in UI
   */
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

  /**
   * Diplay message for all.
   *
   * @return true, if successful
   */
  private boolean diplayMessageForAll() {
    return displayForAll;
  }

  /**
   * Sets the display for all.
   *
   * @param displayForAll the new display for all
   */
  public void setDisplayForAll(boolean displayForAll) {
    this.displayForAll = displayForAll;
  }

  /**
   * Checks if is display for all.
   *
   * @return true, if is display for all
   */
  public boolean isDisplayForAll() {
    return displayForAll;
  }

  /**
   * Sets the message.
   *
   * @param message the new message
   */
  public void setMessage(String message) {
    this.message = message;
  }

  /**
   * Gets the message.
   *
   * @return the message
   */
  public String getMessage() {
    return message;
  }

  /**
   * Sets the position.
   *
   * @param position the new position
   */
  public void setPosition(String position) {
    this.position = position;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void start() {}

  /**
   * {@inheritDoc}
   */
  @Override
  public void stop() {}
}