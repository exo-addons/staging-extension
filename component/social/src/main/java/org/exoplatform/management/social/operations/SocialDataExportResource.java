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

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
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
import javax.jcr.ValueFormatException;

import org.exoplatform.management.common.AbstractJCROperationHandler;
import org.exoplatform.management.common.AbstractOperationHandler;
import org.exoplatform.management.common.ActivitiesExportTask;
import org.exoplatform.management.common.ExportTaskWrapper;
import org.exoplatform.management.common.SpaceMetadataExportTask;
import org.exoplatform.management.social.SocialExtension;
import org.exoplatform.services.jcr.RepositoryService;
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
import org.exoplatform.social.core.model.AvatarAttachment;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.social.core.storage.api.ActivityStorage;
import org.gatein.common.logging.Logger;
import org.gatein.common.logging.LoggerFactory;
import org.gatein.management.api.ContentType;
import org.gatein.management.api.PathAddress;
import org.gatein.management.api.controller.ManagedRequest;
import org.gatein.management.api.controller.ManagedResponse;
import org.gatein.management.api.controller.ManagementController;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.exceptions.ResourceNotFoundException;
import org.gatein.management.api.operation.OperationAttributes;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.ExportResourceModel;
import org.gatein.management.api.operation.model.ExportTask;

/**
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class SocialDataExportResource extends AbstractOperationHandler {

  final private static Logger log = LoggerFactory.getLogger(SocialDataExportResource.class);

  private static final String GROUPS_PATH = "groupsPath";

  private IdentityManager identityManager;
  private ManagementController managementController;
  private NodeHierarchyCreator nodeHierarchyCreator;
  private DataDistributionManager dataDistributionManager;
  private RepositoryService repositoryService;

  // TODO For Space Dashboard export/import
  // private DataStorage dataStorage;

  @Override
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws ResourceNotFoundException, OperationException {
    spaceService = operationContext.getRuntimeContext().getRuntimeComponent(SpaceService.class);
    managementController = operationContext.getRuntimeContext().getRuntimeComponent(ManagementController.class);
    nodeHierarchyCreator = operationContext.getRuntimeContext().getRuntimeComponent(NodeHierarchyCreator.class);
    dataDistributionManager = operationContext.getRuntimeContext().getRuntimeComponent(DataDistributionManager.class);
    repositoryService = operationContext.getRuntimeContext().getRuntimeComponent(RepositoryService.class);
    activityManager = operationContext.getRuntimeContext().getRuntimeComponent(ActivityManager.class);
    activityStorage = operationContext.getRuntimeContext().getRuntimeComponent(ActivityStorage.class);
    identityManager = operationContext.getRuntimeContext().getRuntimeComponent(IdentityManager.class);

    // Increase current transaction timeout
    increaseCurrentTransactionTimeOut(operationContext);

    // TODO For Space Dashboard export/import
    // dataStorage =
    // operationContext.getRuntimeContext().getRuntimeComponent(DataStorage.class);

    OperationAttributes attributes = operationContext.getAttributes();
    List<String> operationFilters = attributes.getValues("filter");

    // "replace-existing" attribute. Defaults to false.
    boolean exportWiki = false, exportAnswer = false, exportCalendar = false, exportForum = false;
    if (operationFilters != null) {
      exportWiki = operationFilters.contains("export-wiki:true");
      exportAnswer = operationFilters.contains("export-answer:true");
      exportForum = operationFilters.contains("export-forum:true");
      exportCalendar = operationFilters.contains("export-calendar:true");
    }

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

      Set<String> alreadyExportedPaths = new HashSet<String>();

      for (String application : appsSet) {
        String path = getEntryResourcePath(application);
        if (path == null || (path.equals(SocialExtension.FORUM_RESOURCE_PATH) && !exportForum) || (path.equals(SocialExtension.WIKI_RESOURCE_PATH) && !exportWiki)
            || (path.equals(SocialExtension.ANSWER_RESOURCE_PATH) && !exportAnswer) || (path.equals(SocialExtension.FAQ_RESOURCE_PATH) && !exportAnswer)
            || (path.equals(SocialExtension.CALENDAR_RESOURCE_PATH) && !exportCalendar)) {
          continue;
        }

        if (!alreadyExportedPaths.contains(path)) {
          addResourceExportTasks(exportTasks, attributesMap, path, space.getPrettyName());
          alreadyExportedPaths.add(path);
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

      log.info("export space JCR private data");
      computeContentFilters(space, attributesMap);
      addResourceExportTasks(exportTasks, attributesMap, SocialExtension.CONTENT_RESOURCE_PATH, space.getPrettyName());

      log.info("export space MOP data (layout & Pages & navigation)");
      attributesMap.clear();
      addResourceExportTasks(exportTasks, attributesMap, SocialExtension.SITES_RESOURCE_PATH + space.getGroupId(), space.getPrettyName());

      log.info("export space metadata");
      String prefix = "social/space/" + space.getPrettyName() + "/";
      exportTasks.add(new SpaceMetadataExportTask(space, prefix));

      Identity spaceIdentity = identityManager.getOrCreateIdentity(SpaceIdentityProvider.NAME, space.getPrettyName(), false);

      log.info("export space avatar");
      exportSpaceAvatar(exportTasks, space, spaceIdentity);

      log.info("export space activities");
      exportSpaceActivities(exportTasks, space, spaceIdentity, exportWiki);
    } catch (Exception e) {
      throw new OperationException(OperationNames.EXPORT_RESOURCE, "Can't export Space", e);
    }
    resultHandler.completed(new ExportResourceModel(exportTasks));
  }

  private void exportSpaceActivities(List<ExportTask> exportTasks, Space space, Identity spaceIdentity, boolean exportWiki) {
    RealtimeListAccess<ExoSocialActivity> spaceActivitiesList = activityManager.getActivitiesOfSpaceWithListAccess(spaceIdentity);
    ExoSocialActivity[] activities = null;

    int size = spaceActivitiesList.getSize(), i = 0;
    List<ExoSocialActivity> activitiesList = new ArrayList<ExoSocialActivity>();
    while (i < size) {
      int length = i + 10 < size ? 10 : size - i;
      activities = spaceActivitiesList.load(i, length);
      for (ExoSocialActivity exoSocialActivity : activities) {
        // Don't export application activities
        if (exoSocialActivity.getType().equals(SocialExtension.SITES_CONTENT_SPACES) || exoSocialActivity.getType().equals(SocialExtension.SITES_FILE_SPACES)
            || exoSocialActivity.getType().equals(SocialExtension.FORUM_ACTIVITY_TYPE) || exoSocialActivity.getType().equals(SocialExtension.POLL_ACTIVITY_TYPE)
            || exoSocialActivity.getType().equals(SocialExtension.WIKI_ACTIVITY_TYPE) || exoSocialActivity.getType().equals(SocialExtension.ANSWER_ACTIVITY_TYPE)
            || exoSocialActivity.getType().equals(SocialExtension.CALENDAR_ACTIVITY_TYPE)) {
          continue;
        }
        activityManager.getParentActivity(exoSocialActivity);
        addActivityWithComments(activitiesList, exoSocialActivity);
      }
      i += length;
    }
    String prefix = "social/space/" + space.getPrettyName() + "/";
    exportTasks.add(new ActivitiesExportTask(identityManager, activitiesList, prefix));
  }

  private void exportSpaceAvatar(List<ExportTask> exportTasks, Space space, Identity spaceIdentity) throws UnsupportedEncodingException, Exception, PathNotFoundException, RepositoryException,
      ValueFormatException {
    // No method to get avatar using Social API, so we have to use JCR
    String avatarURL = spaceIdentity.getProfile().getAvatarUrl();
    avatarURL = avatarURL == null ? null : URLDecoder.decode(avatarURL, "UTF-8");
    if (avatarURL != null && avatarURL.contains(space.getPrettyName())) {
      int beginIndexAvatarPath = avatarURL.indexOf("repository/social") + ("repository/social").length();
      int endIndexAvatarPath = avatarURL.indexOf("?");
      String avatarNodePath = endIndexAvatarPath >= 0 ? avatarURL.substring(beginIndexAvatarPath, endIndexAvatarPath) : avatarURL.substring(beginIndexAvatarPath);
      Session session = AbstractJCROperationHandler.getSession(repositoryService, "social");
      Node avatarNode = (Node) session.getItem(avatarNodePath);
      Node avatarJCRContentNode = avatarNode.getNode("jcr:content");
      String fileName = "avatar";
      String mimeType = avatarJCRContentNode.hasProperty("jcr:data") ? avatarJCRContentNode.getProperty("jcr:mimeType").getString() : null;
      InputStream inputStream = avatarJCRContentNode.hasProperty("jcr:data") ? avatarJCRContentNode.getProperty("jcr:data").getStream() : null;
      Calendar lastModified = avatarJCRContentNode.hasProperty("jcr:data") ? avatarJCRContentNode.getProperty("jcr:lastModified").getDate() : null;

      AvatarAttachment avatar = new AvatarAttachment(null, fileName, mimeType, inputStream, null, lastModified.getTimeInMillis());
      exportTasks.add(new SpaceAvatarExportTask(space.getPrettyName(), avatar));
    }
  }

  private String getEntryResourcePath(String application) {
    String path = null;
    if (application.contains(SocialExtension.FORUM_PORTLET)) {
      path = SocialExtension.FORUM_RESOURCE_PATH;
      log.info("export space forum data");
    } else if (application.contains(SocialExtension.ANSWERS_PORTLET)) {
      path = SocialExtension.ANSWER_RESOURCE_PATH;
      log.info("export space answer data");
    } else if (application.contains(SocialExtension.CALENDAR_PORTLET)) {
      path = SocialExtension.CALENDAR_RESOURCE_PATH;
      log.info("export space calendar data");
    } else if (application.contains(SocialExtension.FAQ_PORTLET)) {
      path = SocialExtension.FAQ_RESOURCE_PATH;
      log.info("export FAQ template");
    } else if (application.contains(SocialExtension.WIKI_PORTLET)) {
      path = SocialExtension.WIKI_RESOURCE_PATH;
      log.info("export space wiki data");
    }
    return path;
  }

  private void computeContentFilters(Space space, Map<String, List<String>> attributesMap) throws RepositoryException, LoginException, NoSuchWorkspaceException, PathNotFoundException {
    List<String> filters = attributesMap.get("filter");
    DataDistributionType dataDistributionType = dataDistributionManager.getDataDistributionType(DataDistributionMode.NONE);
    String contentWorkspace = repositoryService.getCurrentRepository().getConfiguration().getDefaultWorkspaceName();
    String groupsPath = nodeHierarchyCreator.getJcrPath(GROUPS_PATH);

    Session session = AbstractJCROperationHandler.getSession(repositoryService, contentWorkspace);
    Node groupRootNode = (Node) session.getItem(groupsPath);
    Node spaceNode = dataDistributionType.getDataNode(groupRootNode, space.getGroupId());

    filters.clear();
    filters.add("query:select * from nt:base where jcr:path = '" + spaceNode.getPath() + "'");
    filters.add("workspace:" + contentWorkspace);
    filters.add("taxonomy:false");
    filters.add("validate-structure:false");

    attributesMap.put("excludePaths", Collections.singletonList(spaceNode.getPath() + "/ApplicationData/eXoWiki"));
  }

  private void addResourceExportTasks(List<ExportTask> exportTasks, Map<String, List<String>> attributesMap, String path, String spaceId) {
    ManagedRequest request = ManagedRequest.Factory.create(OperationNames.EXPORT_RESOURCE, PathAddress.pathAddress(path), attributesMap, ContentType.ZIP);
    ManagedResponse response = managementController.execute(request);
    ExportResourceModel model = (ExportResourceModel) response.getResult();
    List<ExportTask> entryExportTasks = model.getTasks();
    String basePath = SocialExtension.SPACE_RESOURCE_PARENT_PATH + "/" + spaceId + "/";
    for (ExportTask exportTask : entryExportTasks) {
      exportTasks.add(new ExportTaskWrapper(exportTask, basePath));
    }
  }

}
