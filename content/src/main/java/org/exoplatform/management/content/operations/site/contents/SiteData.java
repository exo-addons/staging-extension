package org.exoplatform.management.content.operations.site.contents;

import java.util.Map;

public class SiteData {
  private SiteMetaData siteMetadata;
  private Map<String, String> nodeExportFiles;

  public SiteMetaData getSiteMetadata() {
    return siteMetadata;
  }

  public void setSiteMetadata(SiteMetaData siteMetadata) {
    this.siteMetadata = siteMetadata;
  }

  public Map<String, String> getNodeExportFiles() {
    return nodeExportFiles;
  }

  public void setNodeExportFiles(Map<String, String> nodeExportFiles) {
    this.nodeExportFiles = nodeExportFiles;
  }
}
