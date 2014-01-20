package org.exoplatform.management.service.api;

import org.chromattic.api.Chromattic;

import java.util.List;

/**
 * @author Thomas Delhoménie
 */
public interface ChromatticService {

  public Chromattic init();

  public List<TargetServer> getSynchonizationServers();

  public void addSynchonizationServer(TargetServer targetServer);

  public void removeSynchonizationServer(TargetServer targetServer);

}
