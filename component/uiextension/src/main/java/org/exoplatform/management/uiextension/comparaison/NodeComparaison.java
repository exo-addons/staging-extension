package org.exoplatform.management.uiextension.comparaison;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.ResourceBundle;

public class NodeComparaison implements Comparable<NodeComparaison>, Serializable {
  private static final long serialVersionUID = -3678314428635211056L;

  private static final DateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy (HH:mm:ss)");

  public static ResourceBundle resourceBundle;

  String title;
  String path;
  String lastModifierUserName;
  Calendar sourceModificationDateCalendar;
  Calendar targetModificationDateCalendar;

  String sourceModificationDate;
  String targetModificationDate;
  boolean publishedOnSource;
  NodeComparaisonState state;

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getLastModifierUserName() {
    return lastModifierUserName;
  }

  public void setLastModifierUserName(String lastModifierUserName) {
    this.lastModifierUserName = lastModifierUserName;
  }

  public String getSourceModificationDate() {
    return sourceModificationDate;
  }

  public String getTargetModificationDate() {
    return targetModificationDate;
  }

  public String getStateLocalized() {
    return state.getLabel(resourceBundle);
  }

  // fakeMethod
  public void setStateLocalized(String fake) {
    // nothinh to do here
  }

  public NodeComparaisonState getState() {
    return state;
  }

  public void setState(NodeComparaisonState state) {
    this.state = state;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public boolean isPublishedOnSource() {
    return publishedOnSource;
  }

  public void setPublishedOnSource(boolean publishedOnSource) {
    this.publishedOnSource = publishedOnSource;
  }

  public void setSourceModificationDateCalendar(Calendar sourceModificationDateCalendar) {
    this.sourceModificationDate = (sourceModificationDateCalendar != null ? dateFormat.format(sourceModificationDateCalendar.getTime()) : null);
    this.sourceModificationDateCalendar = sourceModificationDateCalendar;
  }

  public Calendar getSourceModificationDateCalendar() {
    return sourceModificationDateCalendar;
  }

  public void setTargetModificationDateCalendar(Calendar targetModificationDateCalendar) {
    this.targetModificationDate = (targetModificationDateCalendar != null ? dateFormat.format(targetModificationDateCalendar.getTime()) : null);
    this.targetModificationDateCalendar = targetModificationDateCalendar;
  }

  public Calendar getTargetModificationDateCalendar() {
    return targetModificationDateCalendar;
  }

  @Override
  public int compareTo(NodeComparaison o) {
    return path.compareTo(o.getPath());
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null || !(obj instanceof NodeComparaison)) {
      return false;
    }
    String otherPath = ((NodeComparaison) obj).getPath();
    return path != null && otherPath != null && otherPath.equals(path);
  }

}
