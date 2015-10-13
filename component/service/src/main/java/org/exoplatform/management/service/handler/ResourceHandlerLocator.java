package org.exoplatform.management.service.handler;

import java.util.Arrays;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.management.service.api.ResourceHandler;
import org.exoplatform.management.service.api.StagingService;
import org.exoplatform.management.service.handler.answer.AnswerHandler;
import org.exoplatform.management.service.handler.common.ResourcesInFilterHandler;
import org.exoplatform.management.service.handler.common.SimpleHandler;
import org.exoplatform.management.service.handler.forum.ForumHandler;
import org.exoplatform.management.service.handler.mop.MOPSiteHandler;
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

    // Backup
    registry.register(new SimpleHandler(StagingService.BACKUP_PATH));

    // Social
    registry.register(new SimpleHandler(StagingService.SOCIAL_SPACE_PATH));

    // Wiki
    registry.register(new ResourcesInFilterHandler(StagingService.PORTAL_WIKIS_PATH));
    registry.register(new ResourcesInFilterHandler(StagingService.GROUP_WIKIS_PATH));
    registry.register(new ResourcesInFilterHandler(StagingService.USER_WIKIS_PATH));

    // Forum
    registry.register(new ForumHandler(StagingService.PUBLIC_FORUM_PATH));
    registry.register(new ForumHandler(StagingService.SPACE_FORUM_PATH));
    registry.register(new ForumHandler(StagingService.FORUM_SETTINGS));

    // Calendar
    registry.register(new ResourcesInFilterHandler(StagingService.GROUP_CALENDAR_PATH));
    registry.register(new ResourcesInFilterHandler(StagingService.SPACE_CALENDAR_PATH));
    registry.register(new ResourcesInFilterHandler(StagingService.PERSONAL_FORUM_PATH));

    // Answer
    registry.register(new AnswerHandler(StagingService.PUBLIC_ANSWER_PATH, false));
    registry.register(new AnswerHandler(StagingService.SPACE_ANSWER_PATH, false));
    registry.register(new AnswerHandler(StagingService.FAQ_TEMPLATE_PATH, true));

    // ECM Admin Handlers
    registry.register(new ResourcesInFilterHandler(StagingService.ECM_NODETYPE_PATH));
    registry.register(new ResourcesInFilterHandler(StagingService.ECM_ACTION_PATH));
    registry.register(new ResourcesInFilterHandler(StagingService.ECM_SCRIPT_PATH));
    registry.register(new ResourcesInFilterHandler(StagingService.ECM_DRIVE_PATH));
    registry.register(new ResourcesInFilterHandler(StagingService.ECM_QUERY_PATH));
    registry.register(new ResourcesInFilterHandler(StagingService.ECM_TEMPLATES_METADATA_PATH));
    registry.register(new ResourcesInFilterHandler(StagingService.ECM_TEMPLATES_DOCUMENT_TYPE_PATH));
    registry.register(new SimpleHandler(StagingService.ECM_TEMPLATES_APPLICATION_SEARCH_PATH));
    registry.register(new SimpleHandler(StagingService.ECM_TEMPLATES_APPLICATION_CLV_PATH));
    registry.register(new SimpleHandler(StagingService.ECM_TAXONOMY_PATH));
    registry.register(new ResourcesInFilterHandler(StagingService.ECM_VIEW_TEMPLATES_PATH));
    registry.register(new ResourcesInFilterHandler(StagingService.ECM_VIEW_CONFIGURATION_PATH));

    // Organization Handlers
    registry.register(new SimpleHandler(StagingService.USERS_PATH));
    registry.register(new SimpleHandler(StagingService.GROUPS_PATH));
    registry.register(new SimpleHandler(StagingService.ROLE_PATH));

    // Gadget Handler
    registry.register(new SimpleHandler(StagingService.GADGET_PATH));

    // Aplication Registry Handler
    registry.register(new SimpleHandler(StagingService.REGISTRY_PATH));

    // Sites JCR Content Handler
    registry.register(new SimpleHandler(StagingService.CONTENT_SITES_PATH));

    // MOP Handlers
    registry.register(new MOPSiteHandler(SiteType.PORTAL));
    registry.register(new MOPSiteHandler(SiteType.GROUP));
    registry.register(new MOPSiteHandler(SiteType.USER));
  }

  public static ResourceHandler getResourceHandler(String path) {
    return registry.get(path);
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
