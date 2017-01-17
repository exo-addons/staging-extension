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
package org.exoplatform.management.content.operations.site.seo;

import com.thoughtworks.xstream.XStream;

import org.exoplatform.management.content.operations.site.SiteUtil;
import org.exoplatform.services.seo.PageMetadataModel;
import org.gatein.management.api.operation.model.ExportTask;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

/**
 * The Class SiteSEOExportTask.
 *
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class SiteSEOExportTask implements ExportTask {
  
  /** The Constant FILENAME. */
  public static final String FILENAME = "seo.xml";

  /** The models. */
  private final List<PageMetadataModel> models;
  
  /** The site name. */
  private final String siteName;
  
  /** The lang. */
  private final String lang;

  /**
   * Instantiates a new site SEO export task.
   *
   * @param models the models
   * @param siteName the site name
   * @param lang the lang
   */
  public SiteSEOExportTask(List<PageMetadataModel> models, String siteName, String lang) {
    this.models = models;
    this.siteName = siteName;
    this.lang = lang;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getEntry() {
    return SiteUtil.getSiteBasePath(siteName) + "/" + lang + "_" + FILENAME;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void export(OutputStream outputStream) throws IOException {
    XStream xStream = new XStream();
    xStream.alias("seo", List.class);
    String xmlContent = xStream.toXML(models);
    outputStream.write(xmlContent.getBytes());
  }
}