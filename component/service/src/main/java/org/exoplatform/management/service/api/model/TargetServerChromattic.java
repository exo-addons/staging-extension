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
package org.exoplatform.management.service.api.model;

import org.chromattic.api.annotations.Id;
import org.chromattic.api.annotations.Name;
import org.chromattic.api.annotations.PrimaryType;
import org.chromattic.api.annotations.Property;

/**
 * Chromattic mapping of a synchronization target server.
 *
 * @author Thomas Delhoménie
 */
@PrimaryType(name = "staging:server")
public abstract class TargetServerChromattic {

  /**
   * Instantiates a new target server chromattic.
   */
  public TargetServerChromattic() {}

  /**
   * Gets the id.
   *
   * @return the id
   */
  @Id
  public abstract String getId();

  /**
   * Sets the id.
   *
   * @param id the new id
   */
  public abstract void setId(String id);

  /**
   * Gets the name.
   *
   * @return the name
   */
  @Name
  public abstract String getName();

  /**
   * Sets the name.
   *
   * @param name the new name
   */
  public abstract void setName(String name);

  /**
   * Gets the host.
   *
   * @return the host
   */
  @Property(name = "staging:host")
  public abstract String getHost();

  /**
   * Sets the host.
   *
   * @param host the new host
   */
  public abstract void setHost(String host);

  /**
   * Gets the port.
   *
   * @return the port
   */
  @Property(name = "staging:port")
  public abstract String getPort();

  /**
   * Sets the port.
   *
   * @param port the new port
   */
  public abstract void setPort(String port);

  /**
   * Gets the username.
   *
   * @return the username
   */
  @Property(name = "staging:username")
  public abstract String getUsername();

  /**
   * Sets the username.
   *
   * @param username the new username
   */
  public abstract void setUsername(String username);

  /**
   * Gets the password.
   *
   * @return the password
   */
  @Property(name = "staging:password")
  public abstract String getPassword();

  /**
   * Sets the password.
   *
   * @param password the new password
   */
  public abstract void setPassword(String password);

  /**
   * Checks if is ssl.
   *
   * @return true, if is ssl
   */
  @Property(name = "staging:ssl")
  public abstract boolean isSsl();

  /**
   * Sets the ssl.
   *
   * @param ssl the new ssl
   */
  public abstract void setSsl(boolean ssl);
}
