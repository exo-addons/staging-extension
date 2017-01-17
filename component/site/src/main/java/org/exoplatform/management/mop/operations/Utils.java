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

import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.SiteType;
import org.gatein.mop.api.workspace.ObjectType;
import org.gatein.mop.api.workspace.Site;

/**
 * The Class Utils.
 *
 * @author <a href="mailto:nscavell@redhat.com">Nick Scavelli</a>
 * @version $Revision$
 */
public class Utils {

  /**
   * Instantiates a new utils.
   */
  private Utils() {}

  /**
   * Gets the object type.
   *
   * @param siteType the site type
   * @return the object type
   */
  public static ObjectType<Site> getObjectType(SiteType siteType) {
    switch (siteType) {
    case PORTAL:
      return ObjectType.PORTAL_SITE;
    case GROUP:
      return ObjectType.GROUP_SITE;
    case USER:
      return ObjectType.USER_SITE;
    default:
      return null;
    }
  }

  /**
   * Gets the site type.
   *
   * @param objectType the object type
   * @return the site type
   */
  public static SiteType getSiteType(ObjectType<? extends Site> objectType) {
    if (ObjectType.PORTAL_SITE == objectType) {
      return SiteType.PORTAL;
    } else if (ObjectType.GROUP_SITE == objectType) {
      return SiteType.GROUP;
    } else if (ObjectType.USER_SITE == objectType) {
      return SiteType.USER;
    } else {
      return null;
    }
  }

  /**
   * Gets the site type.
   *
   * @param siteType the site type
   * @return the site type
   */
  public static SiteType getSiteType(String siteType) {
    if (siteType == null)
      return null;

    return SiteType.valueOf(siteType.toUpperCase());
  }

  /**
   * Site key.
   *
   * @param siteType the site type
   * @param siteName the site name
   * @return the site key
   */
  public static SiteKey siteKey(String siteType, String siteName) {
    SiteType st = getSiteType(siteType);
    return siteKey(st, siteName);
  }

  /**
   * Site key.
   *
   * @param siteType the site type
   * @param siteName the site name
   * @return the site key
   */
  public static SiteKey siteKey(SiteType siteType, String siteName) {
    switch (siteType) {
    case PORTAL:
      return SiteKey.portal(siteName);
    case GROUP:
      if (siteName.charAt(0) != '/')
        siteName = "/" + siteName;
      return SiteKey.group(siteName);
    case USER:
      return SiteKey.user(siteName);
    default:
      return null;

    }
  }

  /**
   * Site key.
   *
   * @param site the site
   * @return the site key
   */
  public static SiteKey siteKey(Site site) {
    return siteKey(getSiteType(site.getObjectType()), site.getName());
  }
}
