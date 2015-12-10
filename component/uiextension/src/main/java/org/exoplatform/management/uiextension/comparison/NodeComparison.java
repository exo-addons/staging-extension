package org.exoplatform.management.uiextension.comparison;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.exoplatform.management.uiextension.PushContentPopupComponent;

public class NodeComparison implements Comparable<NodeComparison>, Serializable {
  private static final long serialVersionUID = -3678314428635211056L;

  private static final DateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy (HH:mm:ss)");

  String title;
  String path;
  String lastModifierUserName;
  Calendar sourceModificationDateCalendar;
  Calendar targetModificationDateCalendar;

  String sourceModificationDate;
  String targetModificationDate;
  Boolean published;
  NodeComparisonState state;

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
    return state.getLabel(PushContentPopupComponent.getResourceBundle());
  }

  public String getActionLocalized() {
    return state.getAction(PushContentPopupComponent.getResourceBundle());
  }

  // fakeMethod
  public void setStateLocalized(String fake) {
    // nothing to do here
  }

  // fakeMethod
  public void setActionLocalized(String fake) {
    // nothing to do here
  }

  public NodeComparisonState getState() {
    return state;
  }

  public void setState(NodeComparisonState state) {
    this.state = state;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public Boolean isPublished() {
    return published;
  }

  public void setPublished(Boolean publishedOnSource) {
    this.published = publishedOnSource;
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
  public int compareTo(NodeComparison o) {
    if (o.getState().equals(getState())) {
      return title.compareTo(o.getTitle());
    } else {
      return getState().compareTo(o.getState());
    }
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null || !(obj instanceof NodeComparison)) {
      return false;
    }
    String otherPath = ((NodeComparison) obj).getPath();
    return path != null && otherPath != null && otherPath.equals(path);
  }

}
