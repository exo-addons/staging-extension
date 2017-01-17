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

/**
 * The Class AbstractImportTask.
 *
 * @author <a href="mailto:nscavell@redhat.com">Nick Scavelli</a>
 * @version $Revision$
 * @param <T> the generic type
 */
public abstract class AbstractImportTask<T> extends ImportTask<T> {
  
  /** The site key. */
  protected final SiteKey siteKey;

  /**
   * Instantiates a new abstract import task.
   *
   * @param data the data
   * @param siteKey the site key
   */
  public AbstractImportTask(T data, SiteKey siteKey) {
    super(data);
    this.siteKey = siteKey;
  }

  /**
   * Gets the site key.
   *
   * @return the site key
   */
  public SiteKey getSiteKey() {
    return siteKey;
  }
}
