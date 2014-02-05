package org.exoplatform.management.content.operations.site.contents;

import java.io.Serializable;
import java.util.Calendar;

public class NodeMetadata implements Serializable {

  private static final long serialVersionUID = -7083164711313946955L;

  private String path;
  private String title;
  private Calendar dateModified;
  private String lastModifier;
  private String publicationHistory;

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public Calendar getDateModified() {
    return dateModified;
  }

  public void setDateModified(Calendar dateModified) {
    this.dateModified = dateModified;
  }

  public String getPublicationHistory() {
    return publicationHistory;
  }

  public void setPublicationHistory(String publicationHistory) {
    this.publicationHistory = publicationHistory;
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

}
