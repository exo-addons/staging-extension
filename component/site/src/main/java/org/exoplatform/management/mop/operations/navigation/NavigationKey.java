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

package org.exoplatform.management.mop.operations.navigation;

import org.exoplatform.portal.mop.SiteKey;

/**
 * The Class NavigationKey.
 *
 * @author <a href="mailto:nscavell@redhat.com">Nick Scavelli</a>
 * @version $Revision$
 */
public class NavigationKey {
  
  /** The site key. */
  private SiteKey siteKey;
  
  /** The nav uri. */
  private String navUri;

  /**
   * Instantiates a new navigation key.
   *
   * @param siteKey the site key
   */
  public NavigationKey(SiteKey siteKey) {
    this.siteKey = siteKey;
  }

  /**
   * Instantiates a new navigation key.
   *
   * @param siteKey the site key
   * @param navUri the nav uri
   */
  public NavigationKey(SiteKey siteKey, String navUri) {
    this.siteKey = siteKey;
    this.navUri = navUri;
  }

  /**
   * Gets the site key.
   *
   * @return the site key
   */
  public SiteKey getSiteKey() {
    return siteKey;
  }

  /**
   * Gets the nav uri.
   *
   * @return the nav uri
   */
  public String getNavUri() {
    return navUri;
  }
}
