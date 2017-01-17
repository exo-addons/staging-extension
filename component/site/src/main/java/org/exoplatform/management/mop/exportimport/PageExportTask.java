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

import org.exoplatform.management.common.DataTransformerService;
import org.exoplatform.management.mop.operations.page.PageUtils;
import org.exoplatform.portal.config.DataStorage;
import org.exoplatform.portal.config.model.Page;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.page.PageKey;
import org.exoplatform.portal.mop.page.PageService;
import org.gatein.management.api.binding.Marshaller;
import org.gatein.management.api.operation.model.ExportTask;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The Class PageExportTask.
 *
 * @author <a href="mailto:nscavell@redhat.com">Nick Scavelli</a>
 * @version $Revision$
 */
public class PageExportTask extends AbstractExportTask implements ExportTask {
  
  /** The Constant FILE. */
  public static final String FILE = "pages.xml";

  /** The data storage. */
  private final DataStorage dataStorage;
  
  /** The page service. */
  private final PageService pageService;
  
  /** The marshaller. */
  private final Marshaller<Page.PageSet> marshaller;
  
  /** The page names. */
  private final List<String> pageNames;

  /**
   * Instantiates a new page export task.
   *
   * @param siteKey the site key
   * @param dataStorage the data storage
   * @param pageService the page service
   * @param marshaller the marshaller
   */
  public PageExportTask(SiteKey siteKey, DataStorage dataStorage, PageService pageService, Marshaller<Page.PageSet> marshaller) {
    super(siteKey);
    this.dataStorage = dataStorage;
    this.pageService = pageService;
    this.marshaller = marshaller;
    pageNames = new ArrayList<String>();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void export(OutputStream outputStream) throws IOException {
    Page.PageSet pages = new Page.PageSet();
    pages.setPages(new ArrayList<Page>(pageNames.size()));
    for (String pageName : pageNames) {
      try {
        PageKey pageKey = new PageKey(siteKey, pageName);
        Page page = PageUtils.getPage(dataStorage, pageService, pageKey);
        DataTransformerService.exportData("Page", page);
        pages.getPages().add(page);
      } catch (Exception e) {
        throw new IOException("Could not retrieve page name " + pageName + " for site " + siteKey, e);
      }
    }

    marshaller.marshal(pages, outputStream, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected String getXmlFileName() {
    return FILE;
  }

  /**
   * Adds the page name.
   *
   * @param pageName the page name
   */
  public void addPageName(String pageName) {
    pageNames.add(pageName);
  }

  /**
   * Gets the page names.
   *
   * @return the page names
   */
  public List<String> getPageNames() {
    return Collections.unmodifiableList(pageNames);
  }
}
