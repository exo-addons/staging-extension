package org.exoplatform.management.service.api;

/**
 * Target server of a synchronization
 *
 * @author Thomas Delhoménie
 */
public class TargetServer {
  private String host;
  private String port;
  private String username;
  private String password;
  private boolean ssl;

  public TargetServer(String host, String port, String username, String password, boolean ssl) {
    this.host = host;
    this.port = port;
    this.username = username;
    this.password = password;
    this.ssl = ssl;
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
