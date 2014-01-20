package org.exoplatform.management.service.api;

/**
 * Target server of a synchronization
 *
 * @author Thomas Delhoménie
 */
public class TargetServer {
  private String id;
  private String name;
  private String host;
  private String port;
  private String username;
  private String password;
  private boolean ssl;

  public TargetServer() {
  }

  public TargetServer(String id, String name, String host, String port, String username, String password, boolean ssl) {
    this.id = id;
    this.name = name;
    this.host = host;
    this.port = port;
    this.username = username;
    this.password = password;
    this.ssl = ssl;
  }

  public TargetServer(String name, String host, String port, String username, String password, boolean ssl) {
    this(null, name, host, port, username, password, ssl);
  }

  public TargetServer(String host, String port, String username, String password, boolean ssl) {
    this(null, host, port, username, password, ssl);
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getHost() {
    return host;
  }

  public void setHost(String host) {
    this.host = host;
  }

  public String getPort() {
    return port;
  }

  public void setPort(String port) {
    this.port = port;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public boolean isSsl() {
    return ssl;
  }

  public void setSsl(boolean ssl) {
    this.ssl = ssl;
  }
}
