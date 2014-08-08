package org.exoplatform.management.service.handler;

import java.util.Arrays;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.management.service.api.ResourceHandler;
import org.exoplatform.management.service.api.StagingService;
import org.exoplatform.management.service.handler.answer.AnswerHandler;
import org.exoplatform.management.service.handler.calendar.CalendarHandler;
import org.exoplatform.management.service.handler.content.SiteContentsHandler;
import org.exoplatform.management.service.handler.ecmadmin.ActionNodeTypeHandler;
import org.exoplatform.management.service.handler.ecmadmin.CLVTemplatesHandler;
import org.exoplatform.management.service.handler.ecmadmin.DrivesHandler;
import org.exoplatform.management.service.handler.ecmadmin.JCRQueryHandler;
import org.exoplatform.management.service.handler.ecmadmin.MetadataTemplatesHandler;
import org.exoplatform.management.service.handler.ecmadmin.NodeTypeHandler;
import org.exoplatform.management.service.handler.ecmadmin.NodeTypeTemplatesHandler;
import org.exoplatform.management.service.handler.ecmadmin.ScriptsHandler;
import org.exoplatform.management.service.handler.ecmadmin.SearchTemplatesHandler;
import org.exoplatform.management.service.handler.ecmadmin.SiteExplorerTemplatesHandler;
import org.exoplatform.management.service.handler.ecmadmin.SiteExplorerViewHandler;
import org.exoplatform.management.service.handler.ecmadmin.TaxonomyHandler;
import org.exoplatform.management.service.handler.forum.ForumHandler;
import org.exoplatform.management.service.handler.gadget.GadgetHandler;
import org.exoplatform.management.service.handler.mop.MOPSiteHandler;
import org.exoplatform.management.service.handler.organization.GroupsHandler;
import org.exoplatform.management.service.handler.organization.RolesHandler;
import org.exoplatform.management.service.handler.organization.UsersHandler;
import org.exoplatform.management.service.handler.registry.ApplicationRegistryHandler;
import org.exoplatform.management.service.handler.social.SocialHandler;
import org.exoplatform.management.service.handler.wiki.WikiHandler;
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

    // Social
    registry.register(new SocialHandler());

    // Wiki
    registry.register(new WikiHandler(StagingService.PORTAL_WIKIS_PATH));
    registry.register(new WikiHandler(StagingService.GROUP_WIKIS_PATH));
    registry.register(new WikiHandler(StagingService.USER_WIKIS_PATH));

    // Forum
    registry.register(new ForumHandler(StagingService.PUBLIC_FORUM_PATH));
    registry.register(new ForumHandler(StagingService.SPACE_FORUM_PATH));
    registry.register(new ForumHandler(StagingService.FORUM_SETTINGS));

    // Calendar
    registry.register(new CalendarHandler(StagingService.GROUP_CALENDAR_PATH));
    registry.register(new CalendarHandler(StagingService.SPACE_CALENDAR_PATH));
    registry.register(new CalendarHandler(StagingService.PERSONAL_FORUM_PATH));

    // Answer
    registry.register(new AnswerHandler(StagingService.PUBLIC_ANSWER_PATH, false));
    registry.register(new AnswerHandler(StagingService.SPACE_ANSWER_PATH, false));
    registry.register(new AnswerHandler(StagingService.FAQ_TEMPLATE_PATH, true));

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

  public static ResourceHandler findResourceByPath(String path) {
    String[] fileNameParts = path.split("/");
    for (int i = fileNameParts.length; i > 0; i--) {
      String tmpPath = StringUtils.join(Arrays.copyOfRange(fileNameParts, 0, i), "/");
      ResourceHandler handler = getResourceHandler(tmpPath);
      if (handler != null) {
        return handler;
      }
    }
    return null;
  }
}
