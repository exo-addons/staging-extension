/*
 * Copyright (C) 2003-2014 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.management.social.operations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.exoplatform.management.social.SocialExtension;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.distribution.DataDistributionManager;
import org.exoplatform.services.jcr.ext.distribution.DataDistributionMode;
import org.exoplatform.services.jcr.ext.distribution.DataDistributionType;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.social.common.RealtimeListAccess;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.SpaceIdentityProvider;
import org.exoplatform.social.core.manager.ActivityManager;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.gatein.common.logging.Logger;
import org.gatein.common.logging.LoggerFactory;
import org.gatein.management.api.ContentType;
import org.gatein.management.api.PathAddress;
import org.gatein.management.api.controller.ManagedRequest;
import org.gatein.management.api.controller.ManagedResponse;
import org.gatein.management.api.controller.ManagementController;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.exceptions.ResourceNotFoundException;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.OperationHandler;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.ExportResourceModel;
import org.gatein.management.api.operation.model.ExportTask;

/**
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class SocialDataExportResource implements OperationHandler {

  final private static Logger log = LoggerFactory.getLogger(SocialDataExportResource.class);

  private static final String GROUPS_PATH = "groupsPath";

  private SpaceService spaceService;
  private ActivityManager activityManager;
  private IdentityManager identityManager;
  private ManagementController managementController;
  private NodeHierarchyCreator nodeHierarchyCreator;
  private DataDistributionManager dataDistributionManager;
  private RepositoryService repositoryService;

  // private DataStorage dataStorage;

  @Override
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws ResourceNotFoundException, OperationException {
    spaceService = operationContext.getRuntimeContext().getRuntimeComponent(SpaceService.class);
    managementController = operationContext.getRuntimeContext().getRuntimeComponent(ManagementController.class);
    nodeHierarchyCreator = operationContext.getRuntimeContext().getRuntimeComponent(NodeHierarchyCreator.class);
    dataDistributionManager = operationContext.getRuntimeContext().getRuntimeComponent(DataDistributionManager.class);
    repositoryService = operationContext.getRuntimeContext().getRuntimeComponent(RepositoryService.class);
    activityManager = operationContext.getRuntimeContext().getRuntimeComponent(ActivityManager.class);
    identityManager = operationContext.getRuntimeContext().getRuntimeComponent(IdentityManager.class);
    // dataStorage =
    // operationContext.getRuntimeContext().getRuntimeComponent(DataStorage.class);

    String spaceDisplayName = operationContext.getAddress().resolvePathTemplate("space-name");

    List<ExportTask> exportTasks = new ArrayList<ExportTask>();

    try {
      Space space = spaceService.getSpaceByDisplayName(spaceDisplayName);
      String[] apps = space.getApp().split(",");
      Set<String> appsSet = new HashSet<String>(Arrays.asList(apps));
      Map<String, List<String>> attributesMap = new HashMap<String, List<String>>();
      attributesMap.put("exclude-space-metadata", Collections.singletonList("true"));
      List<String> filters = new ArrayList<String>();
      filters.add(spaceDisplayName);
      attributesMap.put("filter", filters);

      for (String application : appsSet) {
        String path = getEntryResourcePath(application);
        if (path != null) {
          addResourceExportTasks(exportTasks, attributesMap, path, space.getPrettyName());
        }
        // TODO Export/import Dashboard gadgets
        // else if (application.contains(SocialExtension.DASHBOARD_PORTLET)) {
        // Dashboard dashboard =
        // SocialDashboardExportTask.getDashboard(dataStorage,
        // space.getGroupId());
        // if (dashboard == null) {
        // continue;
        // }
        // UIContainer uiContainer = new UIContainer();
        // PortalDataMapper.toUIContainer(uiContainer, dashboard);
        // cleanupUIComponentFields(uiContainer);
        // exportTasks.add(new SocialDashboardExportTask(uiContainer,
        // space.getPrettyName()));
        // }
      }

      log.info("export space JCR data");
      computeContentFilters(space, filters);
      addResourceExportTasks(exportTasks, attributesMap, SocialExtension.CONTENT_RESOURCE_PATH, space.getPrettyName());

      log.info("export space MOP data (layout & Pages & navigation)");
      attributesMap.clear();
      addResourceExportTasks(exportTasks, attributesMap, SocialExtension.SITES_RESOURCE_PATH + space.getGroupId(), space.getPrettyName());

      exportTasks.add(new SpaceMetadataExportTask(space));

      Identity spaceIdentity = identityManager.getOrCreateIdentity(SpaceIdentityProvider.NAME, space.getPrettyName(), false);
      RealtimeListAccess<ExoSocialActivity> spaceList = activityManager.getActivitiesOfSpaceWithListAccess(spaceIdentity);

      ExoSocialActivity[] activities = null;
      int size = spaceList.getSize(), i = 0;
      while (i < size) {
        int length = i + 10 < size ? 10 : size - i;
        activities = spaceList.load(0, length);
        exportTasks.add(new SpaceActivitiesExportTask(activities, space.getPrettyName(), i));
        i += length;
      }
    } catch (Exception e) {
      throw new OperationException(OperationNames.EXPORT_RESOURCE, "Can't export Space", e);
    }
    resultHandler.completed(new ExportResourceModel(exportTasks));
  }

  private String getEntryResourcePath(String application) {
    String path = null;
    if (application.startsWith(SocialExtension.FORUM_PORTLET)) {
      path = SocialExtension.FORUM_RESOURCE_PATH;
      log.info("export space forum data");
    } else if (application.startsWith(SocialExtension.ANSWERS_PORTLET)) {
      path = SocialExtension.ANSWER_RESOURCE_PATH;
      log.info("export space answer data");
    } else if (application.startsWith(SocialExtension.CALENDAR_PORTLET)) {
      path = SocialExtension.CALENDAR_RESOURCE_PATH;
      log.info("export space calendar data");
    } else if (application.startsWith(SocialExtension.FAQ_PORTLET)) {
      path = SocialExtension.FAQ_RESOURCE_PATH;
      log.info("export FAQ template");
    } else if (application.startsWith(SocialExtension.WIKI_PORTLET)) {
      path = SocialExtension.WIKI_RESOURCE_PATH;
      log.info("export space wiki data");
    }
    return path;
  }

  private void computeContentFilters(Space space, List<String> filters) throws RepositoryException, LoginException, NoSuchWorkspaceException, PathNotFoundException {
    DataDistributionType dataDistributionType = dataDistributionManager.getDataDistributionType(DataDistributionMode.NONE);
    String contentWorkspace = repositoryService.getCurrentRepository().getConfiguration().getDefaultWorkspaceName();
    String groupsPath = nodeHierarchyCreator.getJcrPath(GROUPS_PATH);

    Session session = SessionProvider.createSystemProvider().getSession(contentWorkspace, repositoryService.getCurrentRepository());
    Node groupRootNode = (Node) session.getItem(groupsPath);
    Node spaceNode = dataDistributionType.getDataNode(groupRootNode, space.getGroupId());

    filters.clear();
    filters.add("query:select * from nt:base where jcr:path = '" + spaceNode.getPath() + "'");
    filters.add("workspace:" + contentWorkspace);
    filters.add("taxonomy:false");
    filters.add("validate-structure:false");
  }

  private void addResourceExportTasks(List<ExportTask> exportTasks, Map<String, List<String>> attributesMap, String path, String spaceId) {
    ManagedRequest request = ManagedRequest.Factory.create(OperationNames.EXPORT_RESOURCE, PathAddress.pathAddress(path), attributesMap, ContentType.ZIP);
    ManagedResponse response = managementController.execute(request);
    ExportResourceModel model = (ExportResourceModel) response.getResult();
    List<ExportTask> entryExportTasks = model.getTasks();
    String basePath = SocialExtension.SPACE_RESOURCE_PATH + "/" + spaceId + "/";
    for (ExportTask exportTask : entryExportTasks) {
      exportTasks.add(new SocialExportTaskWrapper(exportTask, basePath));
    }
  }
}
