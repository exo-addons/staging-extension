package org.exoplatform.management.content.operations.site.contents;

import java.util.HashMap;
import java.util.Map;

public class SiteData {
  private SiteMetaData siteMetadata;
  private Map<String, String> nodeExportFiles = new HashMap<String, String>();
  private Map<String, String> nodeExportHistoryFiles = new HashMap<String, String>();

  public SiteMetaData getSiteMetadata() {
    return siteMetadata;
  }

  public void setSiteMetadata(SiteMetaData siteMetadata) {
    this.siteMetadata = siteMetadata;
  }

  public Map<String, String> getNodeExportFiles() {
    return nodeExportFiles;
  }

  public Map<String, String> getNodeExportHistoryFiles() {
    return nodeExportHistoryFiles;
  }

}
