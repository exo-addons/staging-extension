package org.exoplatform.management.service.handler.mop;

import org.exoplatform.management.service.api.AbstractResourceHandler;
import org.exoplatform.portal.mop.SiteType;

public class MOPSiteHandler extends AbstractResourceHandler {

  String parentPath;

  public MOPSiteHandler(SiteType siteType) {
    parentPath = "/site";
  }

  @Override
  public String getPath() {
    return parentPath;
  }
}
