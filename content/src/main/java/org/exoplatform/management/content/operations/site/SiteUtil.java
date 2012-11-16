package org.exoplatform.management.content.operations.site;

public class SiteUtil {

  /**
   * Builds the base path of the site in the exported zip
   * 
   * @param site
   *          Site's name
   * @return
   */
  public static final String getSiteBasePath(String site) {
    return SiteConstants.SITE_CONTENTS_ROOT_PATH + "/" + site;
  }

  /**
   * Builds the base path of the site's contents in the exported zip
   * 
   * @param site
   *          Site's name
   * @return
   */
  public static final String getSiteContentsBasePath(String site) {
    return getSiteBasePath(site) + "/" + SiteConstants.SITE_CONTENTS_REL_PATH;
  }

}
