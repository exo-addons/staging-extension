package org.exoplatform.management.service.api.model;

import org.chromattic.api.annotations.Id;
import org.chromattic.api.annotations.Name;
import org.chromattic.api.annotations.PrimaryType;
import org.chromattic.api.annotations.Property;

/**
 * Chromattic mapping of a synchronization target server
 *
 * @author Thomas Delhoménie
 */
@PrimaryType(name = "staging:server")
public abstract class TargetServerChromattic {

  public TargetServerChromattic() {
  }

  @Id
  public abstract String getId();

  public abstract void setId(String id);

  @Name
  public abstract String getName();

  public abstract void setName(String name);

  @Property(name = "staging:host")
  public abstract String getHost();

  public abstract void setHost(String host);

  @Property(name = "staging:port")
  public abstract String getPort();

  public abstract void setPort(String port);

  @Property(name = "staging:username")
  public abstract String getUsername();

  public abstract void setUsername(String username);

  @Property(name = "staging:password")
  public abstract String getPassword();

  public abstract void setPassword(String password);

  @Property(name = "staging:ssl")
  public abstract boolean isSsl();

  public abstract void setSsl(boolean ssl);
}
