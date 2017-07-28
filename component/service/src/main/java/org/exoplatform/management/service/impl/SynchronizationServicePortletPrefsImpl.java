package org.exoplatform.management.service.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import javax.portlet.PortletPreferences;

import juzu.impl.bridge.spi.portlet.PortletRequestBridge;
import juzu.impl.request.Request;

import org.exoplatform.management.service.api.ResourceCategory;
import org.exoplatform.management.service.api.ResourceHandler;
import org.exoplatform.management.service.api.SynchronizationService;
import org.exoplatform.management.service.api.TargetServer;
import org.exoplatform.management.service.handler.ResourceHandlerLocator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

public class SynchronizationServicePortletPrefsImpl implements SynchronizationService {

  private Log log = ExoLogger.getLogger(SynchronizationServicePortletPrefsImpl.class);

  /**
   * {@inheritDoc}
   */
  @Override
  public List<TargetServer> getSynchonizationServers() {
    PortletPreferences preferences = getPortletPreferences();

    List<TargetServer> synchronizationServers = new ArrayList<TargetServer>();

    String[] prefSynchronizationServers = preferences.getValues("synchronizationServers", new String[] {});
    for (String prefSynchronizationServer : prefSynchronizationServers) {
      String[] prefSynchronizationServerParts = prefSynchronizationServer.split("\\|");
      if (prefSynchronizationServerParts.length != 7) {
        log.warn("Synchronization server " + prefSynchronizationServer + " is not valid (should be id|name|host|port|username|password|ssl)");
        continue;
      }
      TargetServer targetServer = new TargetServer(prefSynchronizationServerParts[0], prefSynchronizationServerParts[1], prefSynchronizationServerParts[2], prefSynchronizationServerParts[3], prefSynchronizationServerParts[4], prefSynchronizationServerParts[5], "true".equals(prefSynchronizationServerParts[6]));

      synchronizationServers.add(targetServer);
    }

    return synchronizationServers;
  }

  @Override
  public void addSynchonizationServer(TargetServer targetServer) {
    PortletPreferences preferences = getPortletPreferences();

    String[] currentServers = preferences.getValues("synchronizationServers", new String[] {});

    String newServerAsString = new StringBuilder(40).append(UUID.randomUUID()).append("|").append(targetServer.getName()).append("|").append(targetServer.getHost()).append("|").append(targetServer.getPort()).append("|").append(targetServer.getUsername()).append("|").append(targetServer.getPassword()).append("|").append(String.valueOf(targetServer.isSsl())).toString();
    List<String> newServers = new ArrayList<String>(Arrays.asList(currentServers));
    newServers.add(newServerAsString);

    try {
      preferences.setValues("synchronizationServers", newServers.toArray(new String[newServers.size()]));
      preferences.store();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Override
  public void removeSynchonizationServer(TargetServer targetServer) {
    PortletPreferences preferences = getPortletPreferences();

    String[] currentServers = preferences.getValues("synchronizationServers", new String[] {});

    List<String> newServers = new ArrayList<String>(currentServers.length);
    for (String server : currentServers) {
      String id = server.substring(0, server.indexOf("|"));
      if (!targetServer.getId().equals(id)) {
        newServers.add(server);
      }
    }

    try {
      if (newServers.isEmpty()) {
        preferences.reset("synchronizationServers");
      } else {
        preferences.setValues("synchronizationServers", newServers.toArray(new String[newServers.size()]));
      }
      preferences.store();
    } catch (Exception e) {
      e.printStackTrace();
    }
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
        log.error("No handler for " + selectedResourceCategory.getPath());
      }
    }
  }

  /**
   * Workaround to get up to date preferences. Can't use @Inject
   * PortletPreferences probably due to an issue in Juzu.
   *
   */
  protected PortletPreferences getPortletPreferences() {
    Request request = Request.getCurrent();
    PortletRequestBridge<?, ?> bridge = (PortletRequestBridge<?, ?>) request.getBridge();
    return bridge.getPortletRequest().getPreferences();
  }
}