package org.exoplatform.management.content.operations.site;

import org.exoplatform.management.content.ContentManagementExtension;

public class SiteUtil {

  /**
   * Builds the base path for content sites in the exported zip
   *
   * @return
   */
  public static final String getSitesBasePath() {
    return new StringBuilder(20).append(ContentManagementExtension.PATH_CONTENT).append("/").append(ContentManagementExtension.PATH_CONTENT_SITES).toString();
  }

  /**
   * Builds the base path of the site in the exported zip
   * 
   * @param site
   *          Site's name
   * @return
   */
  public static final String getSiteBasePath(String site) {
    return new StringBuilder(30).append(getSitesBasePath()).append("/").append(site).toString();
  }

  /**
   * Builds the base path of the site's contents in the exported zip
   * 
   * @param site
   *          Site's name
   * @return
   */
  public static final String getSiteContentsBasePath(String site) {
    return new StringBuilder(30).append(getSiteBasePath(site)).append("/").append(ContentManagementExtension.PATH_CONTENT_SITES_CONTENTS).toString();
  }

}
