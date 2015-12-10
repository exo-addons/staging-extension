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
import java.util.List;

import javax.jcr.Node;

import org.apache.commons.lang.ArrayUtils;
import org.exoplatform.management.common.exportop.AbstractJCRExportOperationHandler;
import org.exoplatform.management.common.exportop.ActivityExportOperationInterface;
import org.exoplatform.management.common.exportop.JCRNodeExportTask;
import org.exoplatform.management.common.exportop.SpaceMetadataExportTask;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.manager.ActivityManager;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.space.SpaceUtils;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.social.core.storage.api.IdentityStorage;
import org.exoplatform.wiki.mow.api.Page;
import org.exoplatform.wiki.mow.api.Wiki;
import org.exoplatform.wiki.mow.api.WikiType;
import org.exoplatform.wiki.mow.core.api.MOWService;
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
public class WikiDataExportResource extends AbstractJCRExportOperationHandler implements ActivityExportOperationInterface {

  final private static Logger log = LoggerFactory.getLogger(WikiDataExportResource.class);

  private MOWService mowService;

  private WikiType wikiType;
  private RepositoryService repositoryService;

  private WikiService wikiService;
  private ThreadLocal<String> wikiOwnerThreadLocal = new ThreadLocal<String>();

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
    identityStorage = operationContext.getRuntimeContext().getRuntimeComponent(IdentityStorage.class);

    increaseCurrentTransactionTimeOut(operationContext);
    try {
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
    } finally {
      restoreDefaultTransactionTimeOut(operationContext);
    }
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
      wikiOwnerThreadLocal.set(wiki.getOwner());
      String prefix = "wiki/" + wiki.getType().toString().toLowerCase() + "/___" + wiki.getOwner() + "---/";
      exportActivities(exportTasks, wiki.getOwner(), prefix, WIKI_ACTIVITY_TYPE);

    } catch (Exception exception) {
      throw new OperationException(OperationNames.EXPORT_RESOURCE, "Error while exporting wiki", exception);
    }
  }

  @Override
  public boolean isActivityValid(ExoSocialActivity activity) throws Exception {
    if (activity.isComment()) {
      return true;
    } else {
      String pageId = activity.getTemplateParams().get("page_id");
      String pageOwner = activity.getTemplateParams().get("page_owner");
      String pageType = activity.getTemplateParams().get("page_type");
      Page page = null;
      if (pageId != null && pageOwner != null && pageType != null) {
        page = wikiService.getPageById(pageType, pageOwner, pageId);
      }
      if (page == null) {
        log.warn("Wiki page not found. Cannot import activity '" + activity.getTitle() + "'.");
        return false;
      }
      return page.getWiki().getOwner().equals(wikiOwnerThreadLocal.get());
    }
  }
}
