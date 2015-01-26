package org.exoplatform.management.wiki.operations;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.exoplatform.commons.utils.ActivityTypeUtils;
import org.exoplatform.management.common.AbstractJCROperationHandler;
import org.exoplatform.management.common.ActivitiesExportTask;
import org.exoplatform.management.common.SpaceMetadataExportTask;
import org.exoplatform.portal.config.UserACL;
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
import org.exoplatform.wiki.mow.core.api.wiki.PageImpl;
import org.exoplatform.wiki.service.WikiService;
import org.gatein.common.logging.Logger;
import org.gatein.common.logging.LoggerFactory;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.operation.OperationAttributes;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.NoResultModel;

/**
 * Created by The eXo Platform SAS Author : eXoPlatform exo@exoplatform.com Mar
 * 5, 2014
 */
public class WikiDataImportResource extends AbstractJCROperationHandler {

  final private static Logger log = LoggerFactory.getLogger(WikiDataImportResource.class);

  private WikiType wikiType;
  private MOWService mowService;
  private WikiService wikiService;

  public WikiDataImportResource(WikiType wikiType) {
    this.wikiType = wikiType;
  }

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

    increaseCurrentTransactionTimeOut(operationContext);

    OperationAttributes attributes = operationContext.getAttributes();
    List<String> filters = attributes.getValues("filter");

    // "replace-existing" attribute. Defaults to false.
    boolean replaceExisting = filters.contains("replace-existing:true");

    // "create-space" attribute. Defaults to false.
    boolean createSpace = filters.contains("create-space:true");

    InputStream attachmentInputStream = getAttachementInputStream(operationContext);

    try {
      // extract data from zip
      Map<String, List<FileEntry>> contentsByOwner = extractDataFromZip(attachmentInputStream);
      for (String wikiOwner : contentsByOwner.keySet()) {
        List<FileEntry> fileEntries = contentsByOwner.get(wikiOwner);

        if (WikiType.GROUP.equals(wikiType)) {
          FileEntry spaceMetadataFile = getAndRemoveFileByPath(fileEntries, SpaceMetadataExportTask.FILENAME);
          if (spaceMetadataFile != null && spaceMetadataFile.getFile().exists()) {
            boolean spaceCreatedOrAlreadyExists = createSpaceIfNotExists(spaceMetadataFile.getFile(), wikiOwner, createSpace);
            if (!spaceCreatedOrAlreadyExists) {
              log.warn("Import of wiki '" + wikiOwner + "' is ignored. Turn on 'create-space:true' option if you want to automatically create the space.");
              continue;
            }
          }
        }

        FileEntry activitiesFile = getAndRemoveFileByPath(fileEntries, ActivitiesExportTask.FILENAME);

        Wiki wiki = mowService.getModel().getWikiStore().getWikiContainer(wikiType).contains(wikiOwner);
        if (wiki != null) {
          if (replaceExisting) {
            log.info("Overwrite existing wiki for owner : '" + wikiType + ":" + wikiOwner + "' (replace-existing=true). Delete: " + wiki.getWikiHome().getJCRPageNode().getPath());
            deleteActivities(wiki.getType(), wiki.getOwner());
          } else {
            log.info("Ignore existing wiki for owner : '" + wikiType + ":" + wikiOwner + "' (replace-existing=false).");
            continue;
          }
        } else {
          wiki = mowService.getModel().getWikiStore().getWikiContainer(wikiType).addWiki(wikiOwner);
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
          importActivities(activitiesFile.getFile(), spacePrettyName);
        }
      }
    } catch (Exception e) {
      throw new OperationException(OperationNames.IMPORT_RESOURCE, "Unable to import wiki content of type: " + wikiType, e);
    } finally {
      if (attachmentInputStream != null) {
        try {
          attachmentInputStream.close();
        } catch (IOException e) {
          // Nothing to do
        }
      }
    }
    resultHandler.completed(NoResultModel.INSTANCE);
  }

  protected String getManagedFilesPrefix() {
    return "wiki/" + wikiType.name().toLowerCase() + "/";
  }

  protected boolean isUnKnownFileFormat(String filePath) {
    return !filePath.endsWith(".xml") && !filePath.endsWith(SpaceMetadataExportTask.FILENAME) && !filePath.contains(ActivitiesExportTask.FILENAME);
  }

  protected boolean addSpecialFile(List<FileEntry> fileEntries, String filePath, File file) {
    if (filePath.endsWith(SpaceMetadataExportTask.FILENAME)) {
      fileEntries.add(new FileEntry(SpaceMetadataExportTask.FILENAME, file));
      return true;
    } else if (filePath.endsWith(ActivitiesExportTask.FILENAME)) {
      fileEntries.add(new FileEntry(ActivitiesExportTask.FILENAME, file));
      return true;
    }
    return false;
  }

  protected String extractIdFromPath(String path) {
    int beginIndex = ("wiki/" + wikiType + "/___").length();
    int endIndex = path.indexOf("---/", beginIndex);
    return path.substring(beginIndex, endIndex);
  }

  protected void attachActivityToEntity(ExoSocialActivity activity, ExoSocialActivity comment) throws Exception {
    if (comment != null) {
      return;
    }
    String pageId = activity.getTemplateParams().get("page_id");
    String pageOwner = activity.getTemplateParams().get("page_owner");
    String pageType = activity.getTemplateParams().get("page_type");
    Page page = wikiService.getPageById(pageType, pageOwner, pageId);
    ActivityTypeUtils.attachActivityId(page.getJCRPageNode(), activity.getId());
    page.getJCRPageNode().getSession().save();
  }

  protected boolean isActivityNotValid(ExoSocialActivity activity, ExoSocialActivity comment) throws Exception {
    if (comment == null) {
      String pageId = activity.getTemplateParams().get("page_id");
      String pageOwner = activity.getTemplateParams().get("page_owner");
      String pageType = activity.getTemplateParams().get("page_type");
      Page page = null;
      if (pageId != null && pageOwner != null && pageType != null) {
        page = wikiService.getPageById(pageType, pageOwner, pageId);
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

  private void deleteActivities(String wikiType, String wikiOwner) throws Exception {
    // Delete Forum activity stream
    Wiki wiki = wikiService.getWiki(wikiType, wikiOwner);
    if (wiki == null) {
      return;
    }
    PageImpl homePage = (PageImpl) wiki.getWikiHome();
    List<Page> pages = new ArrayList<Page>();
    pages.add(homePage);
    computeChildPages(pages, homePage);

    List<String> activityIds = new ArrayList<String>();
    for (Page page : pages) {
      String activityId = ActivityTypeUtils.getActivityId(page.getJCRPageNode());
      activityIds.add(activityId);
    }
    deleteActivitiesById(activityIds);
  }

  private void computeChildPages(List<Page> pages, PageImpl homePage) throws Exception {
    Collection<PageImpl> chilPages = homePage.getChildPages().values();
    for (PageImpl childPageImpl : chilPages) {
      pages.add(childPageImpl);
      computeChildPages(pages, childPageImpl);
    }
  }

}
