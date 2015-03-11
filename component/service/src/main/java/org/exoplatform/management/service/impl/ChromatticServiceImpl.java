package org.exoplatform.management.service.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.chromattic.api.Chromattic;
import org.chromattic.api.ChromatticBuilder;
import org.chromattic.api.ChromatticSession;
import org.chromattic.api.query.QueryResult;
import org.exoplatform.commons.chromattic.ChromatticManager;
import org.exoplatform.commons.utils.PropertyManager;
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
import org.exoplatform.web.security.codec.AbstractCodec;
import org.exoplatform.web.security.codec.AbstractCodecBuilder;
import org.exoplatform.web.security.security.TokenServiceInitializationException;
import org.gatein.common.io.IOTools;
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

  private AbstractCodec codec;

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

    try {
      initCodec();
    } catch (Exception e) {
      this.codec = null;
    }
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
        String password = decodePassword(server.getPassword());
        targetServers.add(new TargetServer(server.getId(), server.getName(), server.getHost(), server.getPort(), server.getUsername(), password, server.isSsl()));
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
        String password = decodePassword(server.getPassword());
        targetServer = new TargetServer(server.getId(), server.getName(), server.getHost(), server.getPort(), server.getUsername(), password, server.isSsl());
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
      String password = encodePassword(targetServer.getPassword());
      server.setPassword(password);
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

  private void initCodec() throws Exception {
    String builderType = PropertyManager.getProperty("gatein.codec.builderclass");
    Map<String, String> config = new HashMap<String, String>();

    if (builderType != null) {
      // If there is config for codec in configuration.properties, we read the
      // config parameters from config file
      // referenced in configuration.properties
      String configFile = PropertyManager.getProperty("gatein.codec.config");
      InputStream in = null;
      try {
        File f = new File(configFile);
        in = new FileInputStream(f);
        Properties properties = new Properties();
        properties.load(in);
        for (Map.Entry<?, ?> entry : properties.entrySet()) {
          config.put((String) entry.getKey(), (String) entry.getValue());
        }
        config.put("gatein.codec.config.basedir", f.getParentFile().getAbsolutePath());
      } catch (IOException e) {
        throw new TokenServiceInitializationException("Failed to read the config parameters from file '" + configFile + "'.", e);
      } finally {
        IOTools.safeClose(in);
      }
    } else {
      // If there is no config for codec in configuration.properties, we
      // generate key if it does not exist and setup the
      // default config
      builderType = "org.exoplatform.web.security.codec.JCASymmetricCodecBuilder";
      String gtnConfDir = PropertyManager.getProperty("gatein.conf.dir");
      if (gtnConfDir == null || gtnConfDir.length() == 0) {
        throw new TokenServiceInitializationException("'gatein.conf.dir' property must be set.");
      }
      File f = new File(gtnConfDir + "/codec/codeckey.txt");
      if (!f.exists()) {
        File codecDir = f.getParentFile();
        if (!codecDir.exists()) {
          codecDir.mkdir();
        }
        OutputStream out = null;
        try {
          KeyGenerator keyGen = KeyGenerator.getInstance("AES");
          keyGen.init(128);
          SecretKey key = keyGen.generateKey();
          KeyStore store = KeyStore.getInstance("JCEKS");
          store.load(null, "gtnStorePass".toCharArray());
          store.setEntry("gtnKey", new KeyStore.SecretKeyEntry(key), new KeyStore.PasswordProtection("gtnKeyPass".toCharArray()));
          out = new FileOutputStream(f);
          store.store(out, "gtnStorePass".toCharArray());
        } catch (Exception e) {
          throw new TokenServiceInitializationException(e);
        } finally {
          IOTools.safeClose(out);
        }
      }
      config.put("gatein.codec.jca.symmetric.keyalg", "AES");
      config.put("gatein.codec.jca.symmetric.keystore", "codeckey.txt");
      config.put("gatein.codec.jca.symmetric.storetype", "JCEKS");
      config.put("gatein.codec.jca.symmetric.alias", "gtnKey");
      config.put("gatein.codec.jca.symmetric.keypass", "gtnKeyPass");
      config.put("gatein.codec.jca.symmetric.storepass", "gtnStorePass");
      config.put("gatein.codec.config.basedir", f.getParentFile().getAbsolutePath());
    }

    try {
      this.codec = Class.forName(builderType).asSubclass(AbstractCodecBuilder.class).newInstance().build(config);
      LOG.info("Initialized CookieTokenService.codec using builder " + builderType);
    } catch (Exception e) {
      throw new TokenServiceInitializationException("Could not initialize CookieTokenService.codec.", e);
    }
  }

  private String decodePassword(String password) {
    if (codec != null) {
      try {
        password = codec.decode(password);
      } catch (Exception e) {
        LOG.warn("Error while decoding password, it will be used in plain text", e);
      }
    }
    return password;
  }

  private String encodePassword(String password) {
    if (codec != null) {
      try {
        password = codec.encode(password);
      } catch (Exception e) {
        LOG.warn("Error while encoding password, it will be used in plain text", e);
      }
    }
    return password;
  }

}
