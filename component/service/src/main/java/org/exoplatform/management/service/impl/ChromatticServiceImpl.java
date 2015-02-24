package org.exoplatform.management.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.chromattic.api.Chromattic;
import org.chromattic.api.ChromatticBuilder;
import org.chromattic.api.ChromatticSession;
import org.chromattic.api.query.QueryResult;
import org.exoplatform.commons.chromattic.ChromatticManager;
import org.exoplatform.management.service.api.ChromatticService;
import org.exoplatform.management.service.api.TargetServer;
import org.exoplatform.management.service.api.model.TargetServerChromattic;
import org.exoplatform.management.service.integration.CurrentRepositoryLifeCycle;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.picocontainer.Startable;

/**
 * @author Thomas Delhoménie
 */
public class ChromatticServiceImpl implements ChromatticService, Startable {

  private static final String EXO_PRIVILEGEABLE_MIXIN = "exo:privilegeable";

  private static final Log LOG = ExoLogger.getLogger(ChromatticServiceImpl.class);

  private static Map<String, String[]> DEFAULT_PERMISSIONS = new HashMap<String, String[]>();
  static {
    try {
      DEFAULT_PERMISSIONS.put("*:/platform/administrators", new String[] { "read", "set_property", "add_node", "remove" });
      DEFAULT_PERMISSIONS.put("*:/platform/web-contributors", new String[] { "read", "set_property", "add_node", "remove" });
    } catch (Exception e) {
      LOG.error(e);
      DEFAULT_PERMISSIONS = null;
    }
  }

  public final static String STAGING_SERVERS_ROOT_PATH = "/exo:applications/staging/servers";

  private String workspaceName = null;
  private RepositoryService repositoryService;

  Chromattic chromattic;

  public ChromatticServiceImpl(ChromatticManager chromatticManager, RepositoryService repositoryService) {
    // Nothing to do with chromatticManager, it's only to ensure that
    // ChromatticManager is started before this service
    this.repositoryService = repositoryService;
  }

  @Override
  public void start() {
    try {
      workspaceName = repositoryService.getDefaultRepository().getConfiguration().getDefaultWorkspaceName();
    } catch (Exception e) {
      workspaceName = "collaboration";
    }

    setPermissions(STAGING_SERVERS_ROOT_PATH, DEFAULT_PERMISSIONS);

    // Init Chromattic
    ChromatticBuilder builder = ChromatticBuilder.create();
    builder.add(TargetServerChromattic.class);
    builder.setOptionValue(ChromatticBuilder.SESSION_LIFECYCLE_CLASSNAME, CurrentRepositoryLifeCycle.class.getName());
    builder.setOptionValue(ChromatticBuilder.CREATE_ROOT_NODE, true);
    builder.setOptionValue(ChromatticBuilder.ROOT_NODE_PATH, STAGING_SERVERS_ROOT_PATH);

    chromattic = builder.build();
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
  public TargetServer getServerByName(String name) {
    TargetServer targetServer = null;
    ChromatticSession session = null;
    try {
      session = openSession();
      TargetServerChromattic server = null;
      QueryResult<TargetServerChromattic> servers = session.createQueryBuilder(TargetServerChromattic.class).where("jcr:path like '" + STAGING_SERVERS_ROOT_PATH + "/" + name + "'").get().objects();
      if (servers.size() == 0) {
        return null;
      } else if (servers.size() > 1) {
        throw new IllegalStateException("found more than one server with name: " + name);
      }
      server = servers.next();
      if (server != null) {
        targetServer = new TargetServer(server.getId(), server.getName(), server.getHost(), server.getPort(), server.getUsername(), server.getPassword(), server.isSsl());
      }
    } finally {
      if (session != null) {
        session.close();
      }
    }
    return targetServer;
  }

  @Override
  public void addSynchonizationServer(TargetServer targetServer) {
    ChromatticSession session = null;

    try {
      session = openSession();

      TargetServerChromattic chromatticObject = null;
      try {
        chromatticObject = session.findByPath(TargetServerChromattic.class, STAGING_SERVERS_ROOT_PATH + "/" + targetServer.getName());
        if (chromatticObject != null) {
          throw new IllegalStateException("Attempt to add server with same name");
        }
      } catch (Exception e) {
        // Nothing to do
      }

      TargetServerChromattic server = session.insert(TargetServerChromattic.class, targetServer.getName());
      server.setHost(targetServer.getHost());
      server.setPort(targetServer.getPort());
      server.setUsername(targetServer.getUsername());
      server.setPassword(targetServer.getPassword());
      server.setSsl(targetServer.isSsl());

      session.save();

      String jcrPath = session.getPath(server);
      setPermissions(jcrPath, DEFAULT_PERMISSIONS);
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
    return chromattic.openSession(workspaceName);
  }

  public void setPermissions(String jcrPath, Map<String, String[]> permissions) {
    Session session = null;
    try {
      session = getSession(repositoryService, workspaceName);
      ExtendedNode extendedNode = (ExtendedNode) session.getItem(jcrPath);
      if (extendedNode.canAddMixin(EXO_PRIVILEGEABLE_MIXIN)) {
        extendedNode.addMixin(EXO_PRIVILEGEABLE_MIXIN);
        extendedNode.setPermissions(permissions);
        session.save();
      }
    } catch (Exception e) {
      LOG.error(e);
    } finally {
      if (session != null) {
        session.logout();
      }
    }
  }

  public static final Session getSession(RepositoryService repositoryService, String workspace) throws RepositoryException, LoginException, NoSuchWorkspaceException {
    SessionProvider provider = SessionProvider.createSystemProvider();
    ManageableRepository repository = repositoryService.getCurrentRepository();
    return provider.getSession(workspace, repository);
  }

  @Override
  public void stop() {}
}
