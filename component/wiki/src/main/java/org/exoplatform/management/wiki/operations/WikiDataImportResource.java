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

import org.exoplatform.management.common.FileEntry;
import org.exoplatform.management.common.exportop.ActivitiesExportTask;
import org.exoplatform.management.common.exportop.SpaceMetadataExportTask;
import org.exoplatform.management.common.importop.AbstractJCRImportOperationHandler;
import org.exoplatform.management.common.importop.ActivityImportOperationInterface;
import org.exoplatform.management.common.importop.FileImportOperationInterface;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.services.cache.CacheService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.manager.ActivityManager;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.social.core.storage.api.ActivityStorage;
import org.exoplatform.social.core.storage.api.IdentityStorage;
import org.exoplatform.wiki.mow.api.Page;
import org.exoplatform.wiki.mow.api.Wiki;
import org.exoplatform.wiki.mow.api.WikiType;
import org.exoplatform.wiki.mow.core.api.MOWService;
import org.exoplatform.wiki.mow.core.api.wiki.WikiImpl;
import org.exoplatform.wiki.service.WikiService;
import org.gatein.common.logging.Logger;
import org.gatein.common.logging.LoggerFactory;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.operation.OperationAttributes;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.NoResultModel;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Created by The eXo Platform SAS Author : eXoPlatform exo@exoplatform.com Mar
 * 5, 2014
 */
public class WikiDataImportResource extends AbstractJCRImportOperationHandler implements ActivityImportOperationInterface, FileImportOperationInterface {

  /** The Constant log. */
  final private static Logger log = LoggerFactory.getLogger(WikiDataImportResource.class);

  /** The wiki type. */
  private WikiType wikiType;
  
  /** The mow service. */
  private MOWService mowService;
  
  /** The wiki service. */
  private WikiService wikiService;
  
  /** The cache service. */
  private CacheService cacheService;

  /**
   * Instantiates a new wiki data import resource.
   *
   * @param wikiType the wiki type
   */
  public WikiDataImportResource(WikiType wikiType) {
    this.wikiType = wikiType;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws OperationException {
    spaceService = operationContext.getRuntimeContext().getRuntimeComponent(SpaceService.class);
    userACL = operationContext.getRuntimeContext().getRuntimeComponent(UserACL.class);
    mowService = operationContext.getRuntimeContext().getRuntimeComponent(MOWService.class);
    repositoryService = operationContext.getRuntimeContext().getRuntimeComponent(RepositoryService.class);
    activityManager = operationContext.getRuntimeContext().getRuntimeComponent(ActivityManager.class);
    activityStorage = operationContext.getRuntimeContext().getRuntimeComponent(ActivityStorage.class);
    identityStorage = operationContext.getRuntimeContext().getRuntimeComponent(IdentityStorage.class);
    wikiService = operationContext.getRuntimeContext().getRuntimeComponent(WikiService.class);
    cacheService = operationContext.getRuntimeContext().getRuntimeComponent(CacheService.class);

    OperationAttributes attributes = operationContext.getAttributes();
    List<String> filters = attributes.getValues("filter");

    // "replace-existing" attribute. Defaults to false.
    boolean replaceExisting = filters.contains("replace-existing:true");

    // "create-space" attribute. Defaults to false.
    boolean createSpace = filters.contains("create-space:true");

    InputStream attachmentInputStream = getAttachementInputStream(operationContext);

    increaseCurrentTransactionTimeOut(operationContext);
    try {
      // extract data from zip
      Map<String, List<FileEntry>> contentsByOwner = extractDataFromZip(attachmentInputStream);
      for (String wikiOwner : contentsByOwner.keySet()) {
        List<FileEntry> fileEntries = contentsByOwner.get(wikiOwner);

        if (WikiType.GROUP.equals(wikiType)) {
          FileEntry spaceMetadataFile = getAndRemoveFileByPath(fileEntries, SpaceMetadataExportTask.FILENAME);
          if (spaceMetadataFile != null && spaceMetadataFile.getFile().exists()) {
            boolean spaceCreatedOrAlreadyExists = createSpaceIfNotExists(spaceMetadataFile.getFile(), createSpace);
            if (!spaceCreatedOrAlreadyExists) {
              log.warn("Import of wiki '" + wikiOwner + "' is ignored. Turn on 'create-space:true' option if you want to automatically create the space.");
              continue;
            }
          }
        }

        FileEntry activitiesFile = getAndRemoveFileByPath(fileEntries, ActivitiesExportTask.FILENAME);

        WikiImpl wiki = mowService.getWikiStore().getWikiContainer(wikiType).contains(wikiOwner);
        if (wiki != null) {
          if (replaceExisting) {
            log.info("Overwrite existing wiki for owner : '" + wikiType + ":" + wikiOwner + "' (replace-existing=true). Delete: " + wiki.getWikiHome().getJCRPageNode().getPath());
            deleteActivities(wiki.getType(), wiki.getOwner());
          } else {
            log.info("Ignore existing wiki for owner : '" + wikiType + ":" + wikiOwner + "' (replace-existing=false).");
            continue;
          }
        } else {
          wiki = mowService.getWikiStore().addWiki(wikiType, wikiOwner);
        }

        String workspace = mowService.getSession().getJCRSession().getWorkspace().getName();

        Collections.sort(fileEntries);
        for (FileEntry fileEntry : fileEntries) {
          importNode(fileEntry, workspace, false);
        }

        // Import activities
        if (activitiesFile != null && activitiesFile.getFile().exists()) {
          String spacePrettyName = null;
          if (WikiType.GROUP.equals(wikiType)) {
            Space space = spaceService.getSpaceByGroupId(wikiOwner);
            spacePrettyName = space.getPrettyName();
          }
          log.info("Importing Wiki activities");
          importActivities(activitiesFile.getFile(), spacePrettyName, true);
        }
      }
    } catch (Exception e) {
      throw new OperationException(OperationNames.IMPORT_RESOURCE, "Unable to import wiki content of type: " + wikiType, e);
    } finally {
      restoreDefaultTransactionTimeOut(operationContext);
      if (attachmentInputStream != null) {
        try {
          attachmentInputStream.close();
        } catch (IOException e) {
          // Nothing to do
        }
      }
    }
    clearCaches(cacheService, "wiki");
    resultHandler.completed(NoResultModel.INSTANCE);
  }

  /**
   * {@inheritDoc}
   */
  public String getManagedFilesPrefix() {
    return "wiki/" + wikiType.name().toLowerCase() + "/";
  }

  /**
   * {@inheritDoc}
   */
  public boolean isUnKnownFileFormat(String filePath) {
    return !filePath.endsWith(".xml") && !filePath.endsWith(SpaceMetadataExportTask.FILENAME) && !filePath.contains(ActivitiesExportTask.FILENAME);
  }

  /**
   * {@inheritDoc}
   */
  public boolean addSpecialFile(List<FileEntry> fileEntries, String filePath, File file) {
    if (filePath.endsWith(SpaceMetadataExportTask.FILENAME)) {
      fileEntries.add(new FileEntry(SpaceMetadataExportTask.FILENAME, file));
      return true;
    } else if (filePath.endsWith(ActivitiesExportTask.FILENAME)) {
      fileEntries.add(new FileEntry(ActivitiesExportTask.FILENAME, file));
      return true;
    }
    return false;
  }

  /**
   * {@inheritDoc}
   */
  public String extractIdFromPath(String path) {
    int beginIndex = ("wiki/" + wikiType + "/___").length();
    int endIndex = path.indexOf("---/", beginIndex);
    return path.substring(beginIndex, endIndex);
  }

  /**
   * {@inheritDoc}
   */
  public void attachActivityToEntity(ExoSocialActivity activity, ExoSocialActivity comment) throws Exception {
    if (comment != null) {
      return;
    }
    String pageId = activity.getTemplateParams().get("page_id");
    String pageOwner = activity.getTemplateParams().get("page_owner");
    String pageType = activity.getTemplateParams().get("page_type");

    Page page = wikiService.getPageOfWikiByName(pageType, pageOwner, pageId);
    page.setActivityId(activity.getId());
    wikiService.updatePage(page, null);
  }

  /**
   * {@inheritDoc}
   */
  public boolean isActivityNotValid(ExoSocialActivity activity, ExoSocialActivity comment) throws Exception {
    if (comment == null) {
      String pageId = activity.getTemplateParams().get("page_id");
      String pageOwner = activity.getTemplateParams().get("page_owner");
      String pageType = activity.getTemplateParams().get("page_type");
      Page page = null;
      if (pageId != null && pageOwner != null && pageType != null) {
        page = wikiService.getPageOfWikiByName(pageType, pageOwner, pageId);
      }
      if (page == null) {
        log.warn("Wiki page not found. Cannot import activity '" + activity.getTitle() + "'.");
        return true;
      }
      return false;
    } else {
      return false;
    }
  }

  /**
   * Delete activities.
   *
   * @param wikiType the wiki type
   * @param wikiOwner the wiki owner
   * @throws Exception the exception
   */
  private void deleteActivities(String wikiType, String wikiOwner) throws Exception {
    // Delete Forum activity stream
    Wiki wiki = wikiService.getWikiByTypeAndOwner(wikiType, wikiOwner);
    if (wiki == null) {
      return;
    }

    Page homePage = wiki.getWikiHome();
    List<Page> pages = new ArrayList<Page>();
    computeChildPages(pages, homePage);

    List<String> activityIds = new ArrayList<String>();
    for (Page page : pages) {
      activityIds.add(page.getActivityId());
    }
    deleteActivitiesById(activityIds);
  }

  /**
   * Compute child pages.
   *
   * @param pages the pages
   * @param parentPage the parent page
   * @throws Exception the exception
   */
  private void computeChildPages(List<Page> pages, Page parentPage) throws Exception {
    pages.add(parentPage);
    List<Page> chilPages = wikiService.getChildrenPageOf(parentPage);
    for (Page childPage : chilPages) {
      computeChildPages(pages, childPage);
    }
  }

}
