package org.exoplatform.management.service.api;

import java.util.List;

/**
 * @author Thomas Delhoménie
 */
public interface ChromatticService {

  public List<TargetServer> getSynchonizationServers();

  public void addSynchonizationServer(TargetServer targetServer);

  public void removeSynchonizationServer(TargetServer targetServer);

}
