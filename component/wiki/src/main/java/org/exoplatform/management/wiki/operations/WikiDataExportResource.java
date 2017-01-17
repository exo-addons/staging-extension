/*
 * Copyright (C) 2003-2017 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.exoplatform.management.wiki.operations;

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
import org.exoplatform.wiki.mow.api.WikiType;
import org.exoplatform.wiki.mow.core.api.MOWService;
import org.exoplatform.wiki.mow.core.api.wiki.WikiImpl;
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

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;

/**
 * Created by The eXo Platform SAS Author : eXoPlatform exo@exoplatform.com Mar
 * 5, 2014
 */
public class WikiDataExportResource extends AbstractJCRExportOperationHandler implements ActivityExportOperationInterface {

  /** The Constant log. */
  final private static Logger log = LoggerFactory.getLogger(WikiDataExportResource.class);

  /** The mow service. */
  private MOWService mowService;

  /** The wiki type. */
  private WikiType wikiType;
  
  /** The repository service. */
  private RepositoryService repositoryService;

  /** The wiki service. */
  private WikiService wikiService;
  
  /** The wiki owner thread local. */
  private ThreadLocal<String> wikiOwnerThreadLocal = new ThreadLocal<String>();

  /**
   * Instantiates a new wiki data export resource.
   *
   * @param wikiType the wiki type
   */
  public WikiDataExportResource(WikiType wikiType) {
    this.wikiType = wikiType;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws ResourceNotFoundException, OperationException {
    spaceService = operationContext.getRuntimeContext().getRuntimeComponent(SpaceService.class);
    mowService = operationContext.getRuntimeContext().getRuntimeComponent(MOWService.class);
    repositoryService = operationContext.getRuntimeContext().getRuntimeComponent(RepositoryService.class);
    wikiService = operationContext.getRuntimeContext().getRuntimeComponent(WikiService.class);
    activityManager = operationContext.getRuntimeContext().getRuntimeComponent(ActivityManager.class);
    identityManager = operationContext.getRuntimeContext().getRuntimeComponent(IdentityManager.class);
    identityStorage = operationContext.getRuntimeContext().getRuntimeComponent(IdentityStorage.class);

    String wikiOwner = operationContext.getAttributes().getValue("filter");

    String excludeSpaceMetadataString = operationContext.getAttributes().getValue("exclude-space-metadata");
    boolean exportSpaceMetadata = excludeSpaceMetadataString == null || excludeSpaceMetadataString.trim().equalsIgnoreCase("false");

    List<ExportTask> exportTasks = new ArrayList<ExportTask>();
    increaseCurrentTransactionTimeOut(operationContext);
    try {
      if (wikiOwner == null || wikiOwner.isEmpty()) {
        log.info("Exporting all WIKI of type: " + wikiType);
        for (WikiImpl wiki : mowService.getWikiStore().getWikiContainer(wikiType).getAllWikis()) {
          exportWiki(exportTasks, wiki, exportSpaceMetadata);
        }
      } else {
        if (wikiType.equals(WikiType.GROUP)) {
          Space space = spaceService.getSpaceByDisplayName(wikiOwner);
          if (space != null) {
            wikiOwner = space.getGroupId();
          }
        }
        WikiImpl wiki = mowService.getWikiStore().getWikiContainer(wikiType).contains(wikiOwner);
        exportWiki(exportTasks, wiki, exportSpaceMetadata);
      }
      resultHandler.completed(new ExportResourceModel(exportTasks));
    } finally {
      restoreDefaultTransactionTimeOut(operationContext);
    }
  }

  /**
   * {@inheritDoc}
   */
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

  /**
   * Export wiki.
   *
   * @param exportTasks the export tasks
   * @param wiki the wiki
   * @param exportSpaceMetadata the export space metadata
   */
  private void exportWiki(List<ExportTask> exportTasks, WikiImpl wiki, boolean exportSpaceMetadata) {
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

  /**
   * {@inheritDoc}
   */
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
        page = wikiService.getPageOfWikiByName(pageType, pageOwner, pageId);
      }
      if (page == null) {
        log.warn("Wiki page not found. Cannot import activity '" + activity.getTitle() + "'.");
        return false;
      }
      return page.getWikiOwner().equals(wikiOwnerThreadLocal.get());
    }
  }
}
