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

package org.exoplatform.management.mop.exportimport;

import org.exoplatform.management.mop.operations.MOPSiteProvider;
import org.exoplatform.management.mop.operations.page.PageUtils;
import org.exoplatform.portal.config.DataStorage;
import org.exoplatform.portal.config.model.Page;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.importer.ImportMode;
import org.exoplatform.portal.mop.page.PageContext;
import org.exoplatform.portal.mop.page.PageKey;
import org.exoplatform.portal.mop.page.PageService;
import org.gatein.mop.api.workspace.Site;

import java.util.ArrayList;
import java.util.List;

/**
 * The Class PageImportTask.
 *
 * @author <a href="mailto:nscavell@redhat.com">Nick Scavelli</a>
 * @version $Revision$
 */
public class PageImportTask extends AbstractImportTask<Page.PageSet> {
  
  /** The data storage. */
  private final DataStorage dataStorage;
  
  /** The page service. */
  private final PageService pageService;
  
  /** The site provider. */
  private final MOPSiteProvider siteProvider;
  
  /** The rollback saves. */
  private Page.PageSet rollbackSaves;
  
  /** The rollback deletes. */
  private Page.PageSet rollbackDeletes;

  /**
   * Instantiates a new page import task.
   *
   * @param data the data
   * @param siteKey the site key
   * @param dataStorage the data storage
   * @param pageService the page service
   * @param siteProvider the site provider
   */
  public PageImportTask(Page.PageSet data, SiteKey siteKey, DataStorage dataStorage, PageService pageService, MOPSiteProvider siteProvider) {
    super(data, siteKey);
    this.dataStorage = dataStorage;
    this.pageService = pageService;
    this.siteProvider = siteProvider;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void importData(ImportMode importMode) throws Exception {
    if (data == null || data.getPages() == null || data.getPages().isEmpty())
      return;

    Site site = siteProvider.getSite(siteKey);
    if (site == null)
      throw new Exception("Cannot import pages because site does not exist for " + siteKey);
    org.gatein.mop.api.workspace.Page pages = site.getRootPage().getChild("pages");
    int size = (pages == null) ? 0 : pages.getChildren().size();

    Page.PageSet dst = null;
    switch (importMode) {
    case CONSERVE:

      // No pages exist yet.
      if (size == 0) {
        dst = data;
        rollbackDeletes = data;
      } else {
        dst = null;
      }
      break;
    case INSERT:

      // No pages exist yet.
      if (size == 0) {
        dst = data;
        rollbackDeletes = data;
      } else {
        dst = new Page.PageSet();
        rollbackDeletes = new Page.PageSet();
        for (Page src : data.getPages()) {
          if (pages.getChild(src.getName()) == null) {
            dst.getPages().add(src);
            rollbackDeletes.getPages().add(src);
          }
        }
      }
      break;
    case MERGE:

      // No pages exist yet.
      if (size == 0) {
        dst = data;
        rollbackDeletes = data;
      } else {
        dst = new Page.PageSet();
        rollbackSaves = new Page.PageSet();
        rollbackDeletes = new Page.PageSet();
        for (Page src : data.getPages()) {
          dst.getPages().add(src);
          PageKey pageKey = siteKey.page(src.getName());
          if (pages.getChild(src.getName()) == null) {
            rollbackDeletes.getPages().add(src);
          } else {
            PageContext pageContext = pageService.loadPage(pageKey);
            Page existing = dataStorage.getPage(pageKey.format());
            pageContext.update(existing);
            rollbackSaves.getPages().add(PageUtils.copy(existing));
          }
        }
      }
      break;
    case OVERWRITE:

      // No pages exist yet.
      if (size == 0) {
        dst = data;
        rollbackDeletes = data;
      } else {
        List<Page> list = PageUtils.getAllPages(dataStorage, pageService, siteKey).getPages();
        rollbackSaves = new Page.PageSet();
        rollbackSaves.setPages(new ArrayList<Page>(list.size()));
        rollbackDeletes = new Page.PageSet();
        for (Page page : list) {
          Page copy = PageUtils.copy(page);
          pageService.destroyPage(siteKey.page(page.getName()));
          dataStorage.save();
          rollbackSaves.getPages().add(copy);
        }
        for (Page src : data.getPages()) {
          Page found = findPage(list, src);
          if (found == null) {
            rollbackDeletes.getPages().add(src);
          }
        }

        dst = data;
      }
      break;
    }

    if (dst != null) {
      for (Page page : dst.getPages()) {
        pageService.savePage(new PageContext(siteKey.page(page.getName()), PageUtils.toPageState(page)));
        dataStorage.save(page);
        dataStorage.save();
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void rollback() throws Exception {
    if (rollbackDeletes != null && !rollbackDeletes.getPages().isEmpty()) {
      for (Page page : rollbackDeletes.getPages()) {
        pageService.destroyPage(siteKey.page(page.getName()));
        dataStorage.save();
      }
    }
    if (rollbackSaves != null && !rollbackSaves.getPages().isEmpty()) {
      for (Page page : rollbackSaves.getPages()) {
        pageService.savePage(new PageContext(siteKey.page(page.getName()), PageUtils.toPageState(page)));
        dataStorage.save(page);
        dataStorage.save();
      }
    }
  }

  /**
   * Gets the rollback saves.
   *
   * @return the rollback saves
   */
  Page.PageSet getRollbackSaves() {
    return rollbackSaves;
  }

  /**
   * Gets the rollback deletes.
   *
   * @return the rollback deletes
   */
  Page.PageSet getRollbackDeletes() {
    return rollbackDeletes;
  }

  /**
   * Find page.
   *
   * @param pages the pages
   * @param src the src
   * @return the page
   */
  private static Page findPage(List<Page> pages, Page src) {
    Page found = null;
    for (Page page : pages) {
      if (src.getName().equals(page.getName())) {
        found = page;
      }
    }
    return found;
  }
}
