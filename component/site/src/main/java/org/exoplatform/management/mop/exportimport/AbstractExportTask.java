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

import org.exoplatform.portal.mop.SiteKey;
import org.gatein.management.api.operation.model.ExportTask;

/**
 * The Class AbstractExportTask.
 *
 * @author <a href="mailto:nscavell@redhat.com">Nick Scavelli</a>
 * @version $Revision$
 */
public abstract class AbstractExportTask implements ExportTask {
  
  /** The site key. */
  protected SiteKey siteKey;

  /**
   * Instantiates a new abstract export task.
   *
   * @param siteKey the site key
   */
  protected AbstractExportTask(SiteKey siteKey) {
    this.siteKey = siteKey;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getEntry() {
    String siteType = siteKey.getTypeName();

    String siteName = siteKey.getName();
    if (siteName.charAt(0) == '/')
      siteName = siteName.substring(1, siteName.length());

    return new StringBuilder().append(siteType).append("/").append(siteName).append("/").append(getXmlFileName()).toString();
  }

  /**
   * Gets the xml file name.
   *
   * @return the xml file name
   */
  protected abstract String getXmlFileName();
}
