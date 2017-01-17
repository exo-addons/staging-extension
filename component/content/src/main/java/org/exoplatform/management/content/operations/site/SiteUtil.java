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
package org.exoplatform.management.content.operations.site;

import org.exoplatform.management.content.ContentManagementExtension;

/**
 * The Class SiteUtil.
 */
public class SiteUtil {

  /**
   * Builds the base path for content sites in the exported zip.
   *
   * @return the sites base path
   */
  public static final String getSitesBasePath() {
    return new StringBuilder(20).append(ContentManagementExtension.PATH_CONTENT).append("/").append(ContentManagementExtension.PATH_CONTENT_SITES).toString();
  }

  /**
   * Builds the base path of the site in the exported zip.
   *
   * @param site Site's name
   * @return the site base path
   */
  public static final String getSiteBasePath(String site) {
    return new StringBuilder(30).append(getSitesBasePath()).append("/").append(site).toString();
  }

  /**
   * Builds the base path of the site's contents in the exported zip.
   *
   * @param site Site's name
   * @return the site contents base path
   */
  public static final String getSiteContentsBasePath(String site) {
    return new StringBuilder(30).append(getSiteBasePath(site)).append("/").append(ContentManagementExtension.PATH_CONTENT_SITES_CONTENTS).toString();
  }

}
