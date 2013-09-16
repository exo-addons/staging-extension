package org.exoplatform.management.service.handler;

import org.exoplatform.management.service.api.ResourceHandler;
import org.exoplatform.management.service.handler.content.SiteContentsHandler;
import org.exoplatform.management.service.handler.ecmadmin.*;
import org.exoplatform.management.service.handler.gadget.GadgetHandler;
import org.exoplatform.management.service.handler.mop.MOPSiteHandler;
import org.exoplatform.management.service.handler.organization.GroupsHandler;
import org.exoplatform.management.service.handler.organization.RolesHandler;
import org.exoplatform.management.service.handler.organization.UsersHandler;
import org.exoplatform.management.service.handler.registry.ApplicationRegistryHandler;
import org.exoplatform.portal.mop.SiteType;

/**
 * Service Locator for resources handlers, based on the resource category's path
 *
 * @author Thomas Delhoménie
 */
public class ResourceHandlerLocator {
  private static ResourceHandlerRegistry registry;

  static {
    registry = new ResourceHandlerRegistry();

    // Organization Handlers
    registry.register(new UsersHandler());
    registry.register(new GroupsHandler());
    registry.register(new RolesHandler());

    // Gadget Handler
    registry.register(new GadgetHandler());

    // ECM Admin Handlers
    registry.register(new NodeTypeHandler());
    registry.register(new ActionNodeTypeHandler());
    registry.register(new ScriptsHandler());
    registry.register(new DrivesHandler());
    registry.register(new JCRQueryHandler());
    registry.register(new MetadataTemplatesHandler());
    registry.register(new NodeTypeTemplatesHandler());
    registry.register(new SearchTemplatesHandler());
    registry.register(new CLVTemplatesHandler());
    registry.register(new TaxonomyHandler());
    registry.register(new SiteExplorerTemplatesHandler());
    registry.register(new SiteExplorerViewHandler());

    // Aplication Registry Handler
    registry.register(new ApplicationRegistryHandler());

    // Sites JCR Content Handler
    registry.register(new SiteContentsHandler());

    // MOP Handlers
    registry.register(new MOPSiteHandler(SiteType.PORTAL));
    registry.register(new MOPSiteHandler(SiteType.GROUP));
    registry.register(new MOPSiteHandler(SiteType.USER));

  }

  public static ResourceHandler getResourceHandler(String name) {
    return registry.get(name);
  }
}
