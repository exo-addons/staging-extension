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
package org.exoplatform.management.service.api;

/**
 * Target server of a synchronization.
 *
 * @author Thomas Delhoménie
 */
public class TargetServer {
  
  /** The id. */
  private String id;
  
  /** The name. */
  private String name;
  
  /** The host. */
  private String host;
  
  /** The port. */
  private String port;
  
  /** The username. */
  private String username;
  
  /** The password. */
  private String password;
  
  /** The ssl. */
  private boolean ssl;

  /**
   * Instantiates a new target server.
   */
  public TargetServer() {}

  /**
   * Instantiates a new target server.
   *
   * @param id the id
   * @param name the name
   * @param host the host
   * @param port the port
   * @param username the username
   * @param password the password
   * @param ssl the ssl
   */
  public TargetServer(String id, String name, String host, String port, String username, String password, boolean ssl) {
    this.id = id;
    this.name = name;
    this.host = host;
    this.port = port;
    this.username = username;
    this.password = password;
    this.ssl = ssl;
  }

  /**
   * Instantiates a new target server.
   *
   * @param name the name
   * @param host the host
   * @param port the port
   * @param username the username
   * @param password the password
   * @param ssl the ssl
   */
  public TargetServer(String name, String host, String port, String username, String password, boolean ssl) {
    this(null, name, host, port, username, password, ssl);
  }

  /**
   * Instantiates a new target server.
   *
   * @param host the host
   * @param port the port
   * @param username the username
   * @param password the password
   * @param ssl the ssl
   */
  public TargetServer(String host, String port, String username, String password, boolean ssl) {
    this(null, host, port, username, password, ssl);
  }

  /**
   * Gets the id.
   *
   * @return the id
   */
  public String getId() {
    return id;
  }

  /**
   * Sets the id.
   *
   * @param id the new id
   */
  public void setId(String id) {
    this.id = id;
  }

  /**
   * Gets the name.
   *
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * Sets the name.
   *
   * @param name the new name
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Gets the host.
   *
   * @return the host
   */
  public String getHost() {
    return host;
  }

  /**
   * Sets the host.
   *
   * @param host the new host
   */
  public void setHost(String host) {
    this.host = host;
  }

  /**
   * Gets the port.
   *
   * @return the port
   */
  public String getPort() {
    return port;
  }

  /**
   * Sets the port.
   *
   * @param port the new port
   */
  public void setPort(String port) {
    this.port = port;
  }

  /**
   * Gets the username.
   *
   * @return the username
   */
  public String getUsername() {
    return username;
  }

  /**
   * Sets the username.
   *
   * @param username the new username
   */
  public void setUsername(String username) {
    this.username = username;
  }

  /**
   * Gets the password.
   *
   * @return the password
   */
  public String getPassword() {
    return password;
  }

  /**
   * Sets the password.
   *
   * @param password the new password
   */
  public void setPassword(String password) {
    this.password = password;
  }

  /**
   * Checks if is ssl.
   *
   * @return true, if is ssl
   */
  public boolean isSsl() {
    return ssl;
  }

  /**
   * Sets the ssl.
   *
   * @param ssl the new ssl
   */
  public void setSsl(boolean ssl) {
    this.ssl = ssl;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj instanceof TargetServer) {
      TargetServer server = (TargetServer) obj;
      return host.equals(server.getHost()) && port.equals(server.getPort()) && username.equals(server.getUsername());
    }
    return false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return "[name=" + name + ",host=" + host + ",port" + port + ",username" + username + ",isSSL" + ssl + "]";
  }
}
