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
package org.exoplatform.management.wiki.operations;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;

import org.exoplatform.commons.utils.ActivityTypeUtils;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.social.common.RealtimeListAccess;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.manager.ActivityManager;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.space.SpaceUtils;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.wiki.mow.api.Page;
import org.exoplatform.wiki.mow.api.Wiki;
import org.exoplatform.wiki.mow.api.WikiType;
import org.exoplatform.wiki.mow.core.api.MOWService;
import org.exoplatform.wiki.mow.core.api.wiki.PageImpl;
import org.exoplatform.wiki.service.WikiService;
import org.gatein.common.logging.Logger;
import org.gatein.common.logging.LoggerFactory;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.exceptions.ResourceNotFoundException;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.OperationHandler;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.ExportResourceModel;
import org.gatein.management.api.operation.model.ExportTask;

/**
 * Created by The eXo Platform SAS Author : eXoPlatform exo@exoplatform.com Mar
 * 5, 2014
 */
public class WikiDataExportResource implements OperationHandler {

  final private static Logger log = LoggerFactory.getLogger(WikiDataExportResource.class);

  private MOWService mowService;

  private WikiType wikiType;

  private Map<String, Boolean> isNTRecursiveMap = new HashMap<String, Boolean>();

  private RepositoryService repositoryService;
  private SpaceService spaceService;
  private WikiService wikiService;
  private ActivityManager activityManager;
  private IdentityManager identityManager;

  public WikiDataExportResource(WikiType wikiType) {
    this.wikiType = wikiType;
  }

  @Override
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws ResourceNotFoundException, OperationException {
    spaceService = operationContext.getRuntimeContext().getRuntimeComponent(SpaceService.class);
    mowService = operationContext.getRuntimeContext().getRuntimeComponent(MOWService.class);
    repositoryService = operationContext.getRuntimeContext().getRuntimeComponent(RepositoryService.class);
    wikiService = operationContext.getRuntimeContext().getRuntimeComponent(WikiService.class);
    activityManager = operationContext.getRuntimeContext().getRuntimeComponent(ActivityManager.class);
    identityManager = operationContext.getRuntimeContext().getRuntimeComponent(IdentityManager.class);

    String wikiOwner = operationContext.getAttributes().getValue("filter");

    String excludeSpaceMetadataString = operationContext.getAttributes().getValue("exclude-space-metadata");
    boolean exportSpaceMetadata = excludeSpaceMetadataString == null || excludeSpaceMetadataString.trim().equalsIgnoreCase("false");

    List<ExportTask> exportTasks = new ArrayList<ExportTask>();
    if (wikiOwner == null || wikiOwner.isEmpty()) {
      log.info("Exporting all WIKI of type: " + wikiType);
      for (Wiki wiki : mowService.getModel().getWikiStore().getWikiContainer(wikiType).getAllWikis()) {
        exportWiki(exportTasks, wiki, exportSpaceMetadata);
      }
    } else {
      if (wikiType.equals(WikiType.GROUP)) {
        Space space = spaceService.getSpaceByDisplayName(wikiOwner);
        if (space != null) {
          wikiOwner = space.getGroupId();
        }
      }
      Wiki wiki = mowService.getModel().getWikiStore().getWikiContainer(wikiType).contains(wikiOwner);
      exportWiki(exportTasks, wiki, exportSpaceMetadata);
    }
    resultHandler.completed(new ExportResourceModel(exportTasks));
  }

  private void exportWiki(List<ExportTask> exportTasks, Wiki wiki, boolean exportSpaceMetadata) {
    if (wiki == null) {
      log.warn("Cannot export Resource: Wiki wasn't found.");
      return;
    }
    try {
      Node wikiNode = wiki.getWikiHome().getJCRPageNode().getParent();
      String workspace = wikiNode.getSession().getWorkspace().getName();
      exportNode(wikiNode, wiki.getOwner(), workspace, exportTasks);

      if (exportSpaceMetadata && WikiType.GROUP.name().equalsIgnoreCase(wiki.getType()) && wiki.getOwner().startsWith(SpaceUtils.SPACE_GROUP + "/")) {
        Space space = spaceService.getSpaceByGroupId(wiki.getOwner());
        exportTasks.add(new SpaceMetadataExportTask(space, wiki.getOwner()));
      }

      // export Activities
      exportActivities(exportTasks, wiki);
    } catch (Exception exception) {
      throw new OperationException(OperationNames.EXPORT_RESOURCE, "Error while exporting wiki", exception);
    }
  }

  private void exportActivities(List<ExportTask> exportTasks, Wiki wiki) throws Exception {
    log.info("export Wiki activities");
    // Refresh Wiki
    wiki = wikiService.getWiki(wiki.getType(), wiki.getOwner());

    List<ExoSocialActivity> activitiesList = new ArrayList<ExoSocialActivity>();
    List<Page> pages = new ArrayList<Page>();
    PageImpl homePage = (PageImpl) wiki.getWikiHome();
    pages.add(homePage);

    computeChildPages(pages, homePage);

    for (Page page : pages) {
      String activityId = ActivityTypeUtils.getActivityId(page.getJCRPageNode());
      if (activityId == null || activityId.isEmpty()) {
        continue;
      }
      ExoSocialActivity pageActivity = activityManager.getActivity(activityId);
      if (pageActivity == null || pageActivity.isComment()) {
        continue;
      }
      activitiesList.add(pageActivity);
      RealtimeListAccess<ExoSocialActivity> commentsListAccess = activityManager.getCommentsWithListAccess(pageActivity);
      if (commentsListAccess.getSize() > 0) {
        List<ExoSocialActivity> comments = commentsListAccess.loadAsList(0, commentsListAccess.getSize());
        activitiesList.addAll(comments);
      }
    }
    if (!activitiesList.isEmpty()) {
      exportTasks.add(new WikiActivitiesExportTask(identityManager, activitiesList, wiki.getType(), wiki.getOwner()));
    }
  }

  private void computeChildPages(List<Page> pages, PageImpl homePage) throws Exception {
    Collection<PageImpl> chilPages = homePage.getChildPages().values();
    for (PageImpl childPageImpl : chilPages) {
      pages.add(childPageImpl);
      computeChildPages(pages, childPageImpl);
    }
  }

  private void exportNode(Node childNode, String workspace, String wikiOwner, List<ExportTask> subNodesExportTask) throws Exception {
    String path = childNode.getPath();
    boolean recursive = isRecursiveExport(childNode);
    WikiExportTask wikiExportTask = new WikiExportTask(repositoryService, wikiType, workspace, wikiOwner, path, recursive);
    subNodesExportTask.add(wikiExportTask);
    // If not export the whole node
    if (!recursive) {
      NodeIterator nodeIterator = childNode.getNodes();
      while (nodeIterator.hasNext()) {
        Node node = nodeIterator.nextNode();
        exportNode(node, workspace, wikiOwner, subNodesExportTask);
      }
    }
  }

  private boolean isRecursiveExport(Node node) throws Exception {
    // FIXME: eXo ECMS bug, items with exo:actionnable don't define manatory
    // field exo:actions. Still use this workaround. ECMS-5998
    if (node.isNodeType("exo:actionable") && !node.hasProperty("exo:actions")) {
      node.setProperty("exo:actions", "");
      node.save();
      node.getSession().refresh(true);
    }
    // END workaround

    NodeType nodeType = node.getPrimaryNodeType();
    NodeType[] nodeTypes = node.getMixinNodeTypes();
    boolean recursive = isRecursiveNT(nodeType);
    if (!recursive && nodeTypes != null && nodeTypes.length > 0) {
      int i = 0;
      while (!recursive && i < nodeTypes.length) {
        recursive = isRecursiveNT(nodeTypes[i]);
        i++;
      }
    }
    return recursive;
  }

  private boolean isRecursiveNT(NodeType nodeType) throws Exception {
    if (!isNTRecursiveMap.containsKey(nodeType.getName())) {
      boolean hasMandatoryChild = false;
      NodeDefinition[] nodeDefinitions = nodeType.getChildNodeDefinitions();
      if (nodeDefinitions != null) {
        int i = 0;
        while (!hasMandatoryChild && i < nodeDefinitions.length) {
          hasMandatoryChild = nodeDefinitions[i].isMandatory();
          i++;
        }
      }
      isNTRecursiveMap.put(nodeType.getName(), hasMandatoryChild);
    }
    return isNTRecursiveMap.get(nodeType.getName());
  }

}
