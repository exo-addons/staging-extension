package org.exoplatform.management.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.chromattic.api.Chromattic;
import org.chromattic.api.ChromatticBuilder;
import org.chromattic.api.ChromatticSession;
import org.chromattic.api.query.QueryResult;
import org.exoplatform.management.service.api.ChromatticService;
import org.exoplatform.management.service.api.TargetServer;
import org.exoplatform.management.service.api.model.TargetServerChromattic;
import org.exoplatform.management.service.integration.CurrentRepositoryLifeCycle;

/**
 * @author Thomas Delhoménie
 */
public class ChromatticServiceImpl implements ChromatticService {

  public final static String STAGING_SERVERS_ROOT_PATH = "/exo:applications/staging/servers";

  Chromattic chromattic;

  public ChromatticServiceImpl() {
  }

  public Chromattic init() {

    // Init Chromattic
    ChromatticBuilder builder = ChromatticBuilder.create();
    builder.add(TargetServerChromattic.class);

    builder.setOptionValue(ChromatticBuilder.SESSION_LIFECYCLE_CLASSNAME, CurrentRepositoryLifeCycle.class.getName());
    builder.setOptionValue(ChromatticBuilder.CREATE_ROOT_NODE, true);
    builder.setOptionValue(ChromatticBuilder.ROOT_NODE_PATH, STAGING_SERVERS_ROOT_PATH);

    chromattic = builder.build();

    return chromattic;
  }

  @Override
  public List<TargetServer> getSynchonizationServers() {
    List<TargetServer> targetServers = new ArrayList<TargetServer>();

    ChromatticSession session = null;

    try {
      session = openSession();

      QueryResult<TargetServerChromattic> servers = session.createQueryBuilder(TargetServerChromattic.class).where("jcr:path like '" + STAGING_SERVERS_ROOT_PATH + "/%'").get().objects();
      while (servers.hasNext()) {
        TargetServerChromattic server = servers.next();
        targetServers.add(new TargetServer(server.getId(), server.getName(), server.getHost(), server.getPort(), server.getUsername(), server.getPassword(), server.isSsl()));
      }

      session.save();
    } finally {
      if (session != null) {
        session.close();
      }
    }

    return targetServers;
  }

  @Override
  public void addSynchonizationServer(TargetServer targetServer) {
    ChromatticSession session = null;

    try {
      session = openSession();

      TargetServerChromattic server = session.insert(TargetServerChromattic.class, targetServer.getName());
      server.setHost(targetServer.getHost());
      server.setPort(targetServer.getPort());
      server.setUsername(targetServer.getUsername());
      server.setPassword(targetServer.getPassword());
      server.setSsl(targetServer.isSsl());

      session.save();
    } finally {
      if (session != null) {
        session.close();
      }
    }

  }

  @Override
  public void removeSynchonizationServer(TargetServer targetServer) {
    ChromatticSession session = null;

    try {
      session = openSession();

      TargetServerChromattic server = session.findById(TargetServerChromattic.class, targetServer.getId());
      if (server != null) {
        session.remove(server);
        session.save();
      }
    } finally {
      if (session != null) {
        session.close();
      }
    }
  }

  private ChromatticSession openSession() {
    return chromattic.openSession("collaboration");
  }
}
