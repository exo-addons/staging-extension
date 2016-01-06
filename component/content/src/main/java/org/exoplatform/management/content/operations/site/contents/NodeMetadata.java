package org.exoplatform.management.content.operations.site.contents;

import java.io.Serializable;
import java.util.Calendar;

public class NodeMetadata implements Serializable {

  private static final long serialVersionUID = -7083164711313946955L;

  private String path;
  private String title;
  private Calendar liveDate;
  private Calendar lastModificationDate;
  private String lastModifier;
  private boolean published;

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public Calendar getLiveDate() {
    return liveDate;
  }

  public void setLiveDate(Calendar dateModified) {
    this.liveDate = dateModified;
  }

  public String getLastModifier() {
    return lastModifier;
  }

  public void setLastModifier(String lastModifier) {
    this.lastModifier = lastModifier;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public boolean isPublished() {
    return published;
  }

  public void setPublished(boolean published) {
    this.published = published;
  }

  public void setLastModificationDate(Calendar lastModificationDate) {
    this.lastModificationDate = lastModificationDate;
  }

  public Calendar getLastModificationDate() {
    return lastModificationDate;
  }

}
