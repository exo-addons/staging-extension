package org.exoplatform.management.service.impl;

import org.exoplatform.management.service.api.ResourceHandler;
import org.exoplatform.management.service.api.SynchronizationService;
import org.exoplatform.management.service.handler.content.SiteContentsHandler;
import org.exoplatform.management.service.handler.ecmadmin.*;
import org.exoplatform.management.service.handler.gadget.GadgetHandler;
import org.exoplatform.management.service.handler.mop.MOPSiteHandler;
import org.exoplatform.management.service.handler.organization.GroupsHandler;
import org.exoplatform.management.service.handler.organization.RolesHandler;
import org.exoplatform.management.service.handler.organization.UsersHandler;
import org.exoplatform.management.service.handler.registry.ApplicationRegistryHandler;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import javax.inject.Singleton;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Singleton
public class SynchronizationServiceImpl implements SynchronizationService {

  private Log log = ExoLogger.getLogger(SynchronizationServiceImpl.class);

  private List<ResourceHandler> handlers = new ArrayList<ResourceHandler>();

  public SynchronizationServiceImpl() {
    // Organization Handlers
    handlers.add(new UsersHandler());
    handlers.add(new GroupsHandler());
    handlers.add(new RolesHandler());

    // Gadget Handler
    handlers.add(new GadgetHandler());
    
    // ECM Admin Handlers
    handlers.add(new NodeTypeHandler());
    handlers.add(new ActionNodeTypeHandler());
    handlers.add(new ScriptsHandler());
    handlers.add(new DrivesHandler());
    handlers.add(new JCRQueryHandler());
    handlers.add(new MetadataTemplatesHandler());
    handlers.add(new NodeTypeTemplatesHandler());
    handlers.add(new SearchTemplatesHandler());
    handlers.add(new CLVTemplatesHandler());
    handlers.add(new TaxonomyHandler());
    handlers.add(new SiteExplorerTemplatesHandler());
    handlers.add(new SiteExplorerViewHandler());

    // Aplication Registry Handler
    handlers.add(new ApplicationRegistryHandler());
    
    // Sites JCR Content Handler
    handlers.add(new SiteContentsHandler());
    
    // MOP Handlers
    handlers.add(new MOPSiteHandler(SiteType.PORTAL));
    handlers.add(new MOPSiteHandler(SiteType.GROUP));
    handlers.add(new MOPSiteHandler(SiteType.USER));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void synchronize(Set<String> selectedResources, Map<String, String> options, String isSSLString, String host, String port, String username, String password) throws IOException {
    boolean isSSL = false;
    if (isSSLString != null && isSSLString.equals("true")) {
      isSSL = true;
    }
    for (ResourceHandler handler : handlers) {
      handler.synchronizeData(selectedResources, isSSL, host, port, username, password, options);
    }
  }

}