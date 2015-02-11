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
import java.util.List;

import javax.jcr.Node;

import org.apache.commons.lang.ArrayUtils;
import org.exoplatform.commons.utils.ActivityTypeUtils;
import org.exoplatform.management.common.exportop.AbstractJCRExportOperationHandler;
import org.exoplatform.management.common.exportop.ActivitiesExportTask;
import org.exoplatform.management.common.exportop.JCRNodeExportTask;
import org.exoplatform.management.common.exportop.SpaceMetadataExportTask;
import org.exoplatform.services.jcr.RepositoryService;
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
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.ExportResourceModel;
import org.gatein.management.api.operation.model.ExportTask;

/**
 * Created by The eXo Platform SAS Author : eXoPlatform exo@exoplatform.com Mar
 * 5, 2014
 */
public class WikiDataExportResource extends AbstractJCRExportOperationHandler {

  final private static Logger log = LoggerFactory.getLogger(WikiDataExportResource.class);

  private MOWService mowService;

  private WikiType wikiType;
  private RepositoryService repositoryService;

  private WikiService wikiService;

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

    increaseCurrentTransactionTimeOut(operationContext);

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

  @Override
  protected void addJCRNodeExportTask(Node childNode, List<ExportTask> subNodesExportTask, boolean recursive, String... params) {
    if (params.length != 3) {
      log.warn("Cannot add Wiki Export Task, 3 parameters was expected, got: " + ArrayUtils.toString(params));
      return;
    }

    String prefix = "wiki/" + wikiType.toString().toLowerCase() + "/___" + params[1] + "---/";
    JCRNodeExportTask wikiExportTask = new JCRNodeExportTask(repositoryService, params[0], params[2], prefix, recursive, true);
    subNodesExportTask.add(wikiExportTask);
  }

  private void exportWiki(List<ExportTask> exportTasks, Wiki wiki, boolean exportSpaceMetadata) {
    if (wiki == null) {
      log.warn("Cannot export Resource: Wiki wasn't found.");
      return;
    }
    try {
      Node wikiNode = wiki.getWikiHome().getJCRPageNode().getParent();
      String workspace = wikiNode.getSession().getWorkspace().getName();
      exportNode(wikiNode, exportTasks, workspace, wiki.getOwner());

      if (exportSpaceMetadata && WikiType.GROUP.name().equalsIgnoreCase(wiki.getType()) && wiki.getOwner().startsWith(SpaceUtils.SPACE_GROUP + "/")) {
        Space space = spaceService.getSpaceByGroupId(wiki.getOwner());
        String prefix = "wiki/" + WikiType.GROUP.toString().toLowerCase() + "/___" + wiki.getOwner() + "---/";
        exportTasks.add(new SpaceMetadataExportTask(space, prefix));
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
      addActivityWithComments(activitiesList, activityId);
    }
    if (!activitiesList.isEmpty()) {
      String prefix = "wiki/" + wiki.getType().toString().toLowerCase() + "/___" + wiki.getOwner() + "---/";
      exportTasks.add(new ActivitiesExportTask(identityManager, activitiesList, prefix));
    }
  }

  private void computeChildPages(List<Page> pages, PageImpl homePage) throws Exception {
    Collection<PageImpl> chilPages = homePage.getChildPages().values();
    for (PageImpl childPageImpl : chilPages) {
      pages.add(childPageImpl);
      computeChildPages(pages, childPageImpl);
    }
  }

}
