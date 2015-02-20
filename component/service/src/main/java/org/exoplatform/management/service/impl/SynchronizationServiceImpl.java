package org.exoplatform.management.service.impl;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.management.service.api.ChromatticService;
import org.exoplatform.management.service.api.ResourceCategory;
import org.exoplatform.management.service.api.ResourceHandler;
import org.exoplatform.management.service.api.SynchronizationService;
import org.exoplatform.management.service.api.TargetServer;
import org.exoplatform.management.service.handler.ResourceHandlerLocator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.picocontainer.Startable;

public class SynchronizationServiceImpl implements SynchronizationService, Startable {

  private static final Pattern VARIABLE_PATTERN = Pattern.compile("^\\$\\{(.*)\\}$");

  private static final Log LOG = ExoLogger.getLogger(SynchronizationServiceImpl.class);

  private ChromatticService chromatticService;

  public SynchronizationServiceImpl(ChromatticService chromatticService) {
    this.chromatticService = chromatticService;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<TargetServer> getSynchonizationServers() {
    return chromatticService.getSynchonizationServers();
  }

  @Override
  public void addSynchonizationServer(TargetServer targetServer) {
    targetServer.setName(getVariableFromPattern(targetServer.getName()));
    targetServer.setHost(getVariableFromPattern(targetServer.getHost()));
    targetServer.setPort(getVariableFromPattern(targetServer.getPort()));
    targetServer.setUsername(getVariableFromPattern(targetServer.getUsername()));
    targetServer.setPassword(getVariableFromPattern(targetServer.getPassword()));

    chromatticService.addSynchonizationServer(targetServer);
  }

  private static String getVariableFromPattern(String variable) {
    if (StringUtils.isEmpty(variable)) {
      return null;
    }
    Matcher matcher = VARIABLE_PATTERN.matcher(variable.trim());
    if (matcher.matches()) {
      return matcher.group(1);
    }
    return variable;
  }

  @Override
  public void removeSynchonizationServer(TargetServer targetServer) {
    chromatticService.removeSynchonizationServer(targetServer);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void synchronize(List<ResourceCategory> selectedResourcesCategories, TargetServer targetServer) throws Exception {
    for (ResourceCategory selectedResourceCategory : selectedResourcesCategories) {
      // Gets the right resource handler thanks to the Service Locator
      ResourceHandler resourceHandler = ResourceHandlerLocator.getResourceHandler(selectedResourceCategory.getPath());

      if (resourceHandler != null) {
        resourceHandler.synchronize(selectedResourceCategory.getResources(), selectedResourceCategory.getExportOptions(), selectedResourceCategory.getImportOptions(), targetServer);
      } else {
        LOG.error("No handler for " + selectedResourceCategory.getPath());
        throw new Exception("No handler for " + selectedResourceCategory.getPath());
      }
    }
  }

  @Override
  public void start() {
    String name = System.getProperty("exo.staging.server.default.name");
    String host = System.getProperty("exo.staging.server.default.host");
    String port = System.getProperty("exo.staging.server.default.port");
    String username = System.getProperty("exo.staging.server.default.username");
    String password = System.getProperty("exo.staging.server.default.password");
    String isSSLString = System.getProperty("exo.staging.server.default.isSSL");
    if (!StringUtils.isEmpty(name) && !StringUtils.isEmpty(host) && !StringUtils.isEmpty(port) && !StringUtils.isEmpty(username) && !StringUtils.isEmpty(password)) {
      TargetServer server = chromatticService.getServerByName(name);
      if (server == null) {
        boolean isSSL = StringUtils.isEmpty(isSSLString) ? false : Boolean.parseBoolean(isSSLString);
        server = new TargetServer(name, host, port, username, password, isSSL);
        addSynchonizationServer(server);
      }
    }
  }

  @Override
  public void stop() {}
}