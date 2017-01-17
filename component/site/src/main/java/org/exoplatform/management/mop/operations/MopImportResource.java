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
package org.exoplatform.management.mop.operations;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.commons.chromattic.ChromatticLifeCycle;
import org.exoplatform.commons.chromattic.ChromatticManager;
import org.exoplatform.management.common.AbstractOperationHandler;
import org.exoplatform.management.mop.exportimport.NavigationExportTask;
import org.exoplatform.management.mop.exportimport.NavigationImportTask;
import org.exoplatform.management.mop.exportimport.PageExportTask;
import org.exoplatform.management.mop.exportimport.PageImportTask;
import org.exoplatform.management.mop.exportimport.SiteLayoutExportTask;
import org.exoplatform.management.mop.exportimport.SiteLayoutImportTask;
import org.exoplatform.portal.config.DataStorage;
import org.exoplatform.portal.config.model.Page;
import org.exoplatform.portal.config.model.PageNavigation;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.description.DescriptionService;
import org.exoplatform.portal.mop.importer.ImportMode;
import org.exoplatform.portal.mop.navigation.NavigationService;
import org.exoplatform.portal.mop.page.PageService;
import org.exoplatform.portal.pom.config.POMSession;
import org.exoplatform.portal.pom.config.POMSessionManager;
import org.exoplatform.services.cache.CacheService;
import org.exoplatform.services.cache.ExoCache;
import org.exoplatform.services.jcr.impl.core.SessionImpl;
import org.exoplatform.web.application.RequestContext;
import org.gatein.common.logging.Logger;
import org.gatein.common.logging.LoggerFactory;
import org.gatein.management.api.ContentType;
import org.gatein.management.api.binding.Marshaller;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.exceptions.ResourceNotFoundException;
import org.gatein.management.api.operation.OperationAttachment;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.NoResultModel;
import org.gatein.mop.api.workspace.Site;
import org.gatein.mop.api.workspace.Workspace;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;

import javax.jcr.Session;

/**
 * The Class MopImportResource.
 */
public class MopImportResource extends AbstractOperationHandler {
  
  /** The Constant RESOURCE_PATH_PATTERN. */
  private static final Pattern RESOURCE_PATH_PATTERN = Pattern.compile("^(portal|group|user)/(.*)/(pages\\.xml|navigation\\.xml|portal\\.xml|user\\.xml|group\\.xml)");
  
  /** The Constant log. */
  private static final Logger log = LoggerFactory.getLogger(MopImportResource.class);

  // TODO: Would like to see the step operations be handled by mgmt core.

  /**
   * {@inheritDoc}
   */
  // TODO: Clean this up when we have time
  @Override
  public void execute(final OperationContext operationContext, ResultHandler resultHandler) throws ResourceNotFoundException, OperationException {
    final String operationName = operationContext.getOperationName();

    increaseCurrentTransactionTimeOut(operationContext);
    increaseChromatticSessionTimeout(operationContext);
    try {
      RequestContext requestContext;
      try {
        requestContext = RequestContext.getCurrentInstance();
        if (requestContext != null) {
          requestContext.getUserPortal();
        }
      } catch (Exception e) {
        RequestContext.setCurrentInstance(null);
      }

      OperationAttachment attachment = operationContext.getAttachment(true);
      if (attachment == null)
        throw new OperationException(operationContext.getOperationName(), "No attachment available for MOP import.");

      InputStream inputStream = attachment.getStream();
      if (inputStream == null)
        throw new OperationException(operationContext.getOperationName(), "No data stream available for import.");

      final POMSessionManager mgr = operationContext.getRuntimeContext().getRuntimeComponent(POMSessionManager.class);
      POMSession session = mgr.getSession();
      if (session == null)
        throw new OperationException(operationName, "MOP session was null");

      Workspace workspace = session.getWorkspace();
      if (workspace == null)
        throw new OperationException(operationName, "MOP workspace was null");

      DataStorage dataStorage = operationContext.getRuntimeContext().getRuntimeComponent(DataStorage.class);
      if (dataStorage == null)
        throw new OperationException(operationName, "DataStorage was null");

      PageService pageService = operationContext.getRuntimeContext().getRuntimeComponent(PageService.class);
      if (pageService == null)
        throw new OperationException(operationName, "PageService was null");

      NavigationService navigationService = operationContext.getRuntimeContext().getRuntimeComponent(NavigationService.class);
      if (navigationService == null)
        throw new OperationException(operationName, "Navigation service was null");

      DescriptionService descriptionService = operationContext.getRuntimeContext().getRuntimeComponent(DescriptionService.class);
      if (descriptionService == null)
        throw new OperationException(operationName, "Description service was null");

      CacheService cacheService = operationContext.getRuntimeContext().getRuntimeComponent(CacheService.class);
      if (cacheService == null)
        throw new OperationException(operationName, "Cache service was null");

      String mode = operationContext.getAttributes().getValue("importMode");
      if (mode == null || "".equals(mode))
        mode = "merge";

      ImportMode importMode;
      try {
        importMode = ImportMode.valueOf(mode.trim().toUpperCase());
      } catch (Exception e) {
        throw new OperationException(operationName, "Unknown importMode " + mode);
      }

      Map<SiteKey, MopImport> importMap = new HashMap<SiteKey, MopImport>();
      final NonCloseableZipInputStream zis = new NonCloseableZipInputStream(inputStream);
      ZipEntry entry;
      boolean empty = false;
      try {
        log.info("Preparing data for import.");
        while ((entry = zis.getNextEntry()) != null) {
          // Skip directories
          if (entry.isDirectory())
            continue;
          // Skip empty entries (this allows empty zip files to not
          // cause exceptions).
          if (StringUtils.isEmpty(entry.getName()))
            continue;

          // Parse zip entry
          String[] parts = parseEntry(entry);
          if (parts == null) {
            continue;
          }
          SiteKey siteKey = Utils.siteKey(parts[0], parts[1]);
          String file = parts[2];

          MopImport mopImport = importMap.get(siteKey);
          if (mopImport == null) {
            mopImport = new MopImport();
            importMap.put(siteKey, mopImport);
          }

          if (SiteLayoutExportTask.FILES.contains(file)) {
            // Unmarshal site layout data
            Marshaller<PortalConfig> marshaller = operationContext.getBindingProvider().getMarshaller(PortalConfig.class, ContentType.XML);
            PortalConfig portalConfig = marshaller.unmarshal(zis);
            portalConfig.setType(siteKey.getTypeName());
            if (!portalConfig.getName().equals(siteKey.getName())) {
              throw new OperationException(operationName, "Name of site does not match that of the zip entry site name.");
            }

            // Add import task to run later
            mopImport.siteTask = new SiteLayoutImportTask(portalConfig, siteKey, dataStorage);
          } else if (file.equals(PageExportTask.FILE)) {
            // Unmarshal page data
            Marshaller<Page.PageSet> marshaller = operationContext.getBindingProvider().getMarshaller(Page.PageSet.class, ContentType.XML);
            Page.PageSet pages = marshaller.unmarshal(zis);
            for (Page page : pages.getPages()) {
              page.setOwnerType(siteKey.getTypeName());
              page.setOwnerId(siteKey.getName());
            }

            // Obtain the site from the session when it's needed.
            MOPSiteProvider siteProvider = new MOPSiteProvider() {
              @Override
              public Site getSite(SiteKey siteKey) {
                return mgr.getSession().getWorkspace().getSite(Utils.getObjectType(siteKey.getType()), siteKey.getName());
              }
            };
            // Add import task to run later.
            mopImport.pageTask = new PageImportTask(pages, siteKey, dataStorage, pageService, siteProvider);
          } else if (file.equals(NavigationExportTask.FILE)) {
            // Unmarshal navigation data
            Marshaller<PageNavigation> marshaller = operationContext.getBindingProvider().getMarshaller(PageNavigation.class, ContentType.XML);
            PageNavigation navigation = marshaller.unmarshal(zis);
            navigation.setOwnerType(siteKey.getTypeName());
            navigation.setOwnerId(siteKey.getName());

            // Add import task to run later
            mopImport.navigationTask = new NavigationImportTask(navigation, siteKey, navigationService, descriptionService, dataStorage);
          }
        }

        resultHandler.completed(NoResultModel.INSTANCE);
      } catch (Throwable t) {
        throw new OperationException(operationContext.getOperationName(), "Exception reading data for import.", t);
      } finally {
        try {
          zis.reallyClose();
        } catch (IOException e) {
          log.warn("Exception closing underlying data stream from import.");
        }
        clearCaches(cacheService);
      }

      if (empty) {
        log.info("Nothing to import, zip file empty.");
        return;
      }

      // Perform import
      Map<SiteKey, MopImport> importsRan = new HashMap<SiteKey, MopImport>();
      try {
        log.info("Performing import using importMode '" + mode + "'");
        for (Map.Entry<SiteKey, MopImport> mopImportEntry : importMap.entrySet()) {
          SiteKey siteKey = mopImportEntry.getKey();
          MopImport mopImport = mopImportEntry.getValue();
          MopImport ran = new MopImport();

          if (importsRan.containsKey(siteKey)) {
            throw new IllegalStateException("Multiple site imports for same operation.");
          }
          importsRan.put(siteKey, ran);

          log.debug("Importing data for site " + siteKey);

          // Site layout import
          if (mopImport.siteTask != null) {
            log.debug("Importing site layout data.");
            ran.siteTask = mopImport.siteTask;
            mopImport.siteTask.importData(importMode);
          }

          // Page import
          if (mopImport.pageTask != null) {
            log.debug("Importing page data.");
            ran.pageTask = mopImport.pageTask;
            mopImport.pageTask.importData(importMode);
          }

          // Navigation import
          if (mopImport.navigationTask != null) {
            log.debug("Importing navigation data.");
            ran.navigationTask = mopImport.navigationTask;
            mopImport.navigationTask.importData(importMode);
          }
        }
        log.info("Site pages and navigations imported successfully !");
      } catch (Throwable t) {
        boolean rollbackSuccess = true;
        log.error("Exception importing data.", t);
        log.info("Attempting to rollback data modified by import.");
        for (Map.Entry<SiteKey, MopImport> mopImportEntry : importsRan.entrySet()) {
          SiteKey siteKey = mopImportEntry.getKey();
          MopImport mopImport = mopImportEntry.getValue();

          log.debug("Rolling back imported data for site " + siteKey);
          if (mopImport.navigationTask != null) {
            log.debug("Rolling back navigation modified during import...");
            try {
              mopImport.navigationTask.rollback();
            } catch (Throwable t1) // Continue rolling back even
            // though there are
            // exceptions.
            {
              rollbackSuccess = false;
              log.error("Error rolling back navigation data for site " + siteKey, t1);
            }
          }
          if (mopImport.pageTask != null) {
            log.debug("Rolling back pages modified during import...");
            try {
              mopImport.pageTask.rollback();
            } catch (Throwable t1) // Continue rolling back even
            // though there are
            // exceptions.
            {
              rollbackSuccess = false;
              log.error("Error rolling back page data for site " + siteKey, t1);
            }
          }
          if (mopImport.siteTask != null) {
            log.debug("Rolling back site layout modified during import...");
            try {
              mopImport.siteTask.rollback();
            } catch (Throwable t1) // Continue rolling back even
            // though there are
            // exceptions.
            {
              rollbackSuccess = false;
              log.error("Error rolling back site layout for site " + siteKey, t1);
            }
          }
        }

        String message = (rollbackSuccess) ? "Error during import. Tasks successfully rolled back. Portal should be back to consistent state."
            : "Error during import. Errors in rollback as well. Portal may be in an inconsistent state.";

        throw new OperationException(operationName, message, t);
      } finally {
        importMap.clear();
        importsRan.clear();
      }
    } finally {
      restoreDefaultTransactionTimeOut(operationContext);
    }
  }

  /**
   * Increase chromattic session timeout.
   *
   * @param operationContext the operation context
   */
  private void increaseChromatticSessionTimeout(OperationContext operationContext) {
    ChromatticManager manager = operationContext.getRuntimeContext().getRuntimeComponent(ChromatticManager.class);
    ChromatticLifeCycle chromatticLifeCycle = manager.getLifeCycle("mop");
    try {
      Session session = null;
      if (chromatticLifeCycle.getContext() == null) {
        session = chromatticLifeCycle.openContext().getSession().getJCRSession();
      } else {
        session = chromatticLifeCycle.getContext().getSession().getJCRSession();
      }
      ((SessionImpl) session).setTimeout(ONE_DAY_IN_MS);
    } catch (Exception e) {
      log.error("Error while increasing Chromattic MOP Session Timeout", e);
    }
  }

  /**
   * Clear caches.
   *
   * @param cacheService the cache service
   */
  @SuppressWarnings("rawtypes")
  public void clearCaches(CacheService cacheService) {
    for (Object o : cacheService.getAllCacheInstances()) {
      try {
        ((ExoCache) o).clearCache();
      } catch (Exception e) {
        if (log.isTraceEnabled()) {
          log.trace("An exception occurred: " + e.getMessage());
        }
      }
    }
  }

  /**
   * Parses the entry.
   *
   * @param entry the entry
   * @return the string[]
   * @throws IOException Signals that an I/O exception has occurred.
   */
  private static String[] parseEntry(ZipEntry entry) throws IOException {
    String name = entry.getName();
    Matcher matcher = RESOURCE_PATH_PATTERN.matcher(name);
    if (matcher.matches()) {
      String[] parts = new String[3];
      parts[0] = matcher.group(1);
      parts[1] = matcher.group(2);
      parts[2] = matcher.group(3);
      return parts;
    } else {
      return null;
    }
  }

  /**
   * The Class MopImport.
   */
  private static class MopImport {
    
    /** The site task. */
    private SiteLayoutImportTask siteTask;
    
    /** The page task. */
    private PageImportTask pageTask;
    
    /** The navigation task. */
    private NavigationImportTask navigationTask;
  }
}
