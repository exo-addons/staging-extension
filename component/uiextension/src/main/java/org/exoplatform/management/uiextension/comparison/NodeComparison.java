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
package org.exoplatform.management.uiextension.comparison;

import org.exoplatform.management.uiextension.PushContentPopupComponent;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * The Class NodeComparison.
 */
public class NodeComparison implements Comparable<NodeComparison>, Serializable {
  
  /** The Constant serialVersionUID. */
  private static final long serialVersionUID = -3678314428635211056L;

  /** The Constant dateFormat. */
  private static final DateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy (HH:mm:ss)");

  /** The title. */
  String title;
  
  /** The path. */
  String path;
  
  /** The last modifier user name. */
  String lastModifierUserName;
  
  /** The source modification date calendar. */
  Calendar sourceModificationDateCalendar;
  
  /** The source publication date calendar. */
  Calendar sourcePublicationDateCalendar;
  
  /** The target publication date calendar. */
  Calendar targetPublicationDateCalendar;
  
  /** The target modification date calendar. */
  Calendar targetModificationDateCalendar;

  /** The source modification date. */
  String sourceModificationDate;
  
  /** The target modification date. */
  String targetModificationDate;
  
  /** The published. */
  Boolean published;
  
  /** The state. */
  NodeComparisonState state;

  /**
   * Gets the title.
   *
   * @return the title
   */
  public String getTitle() {
    return title;
  }

  /**
   * Sets the title.
   *
   * @param title the new title
   */
  public void setTitle(String title) {
    this.title = title;
  }

  /**
   * Gets the last modifier user name.
   *
   * @return the last modifier user name
   */
  public String getLastModifierUserName() {
    return lastModifierUserName;
  }

  /**
   * Sets the last modifier user name.
   *
   * @param lastModifierUserName the new last modifier user name
   */
  public void setLastModifierUserName(String lastModifierUserName) {
    this.lastModifierUserName = lastModifierUserName;
  }

  /**
   * Gets the source modification date.
   *
   * @return the source modification date
   */
  public String getSourceModificationDate() {
    return sourceModificationDate;
  }

  /**
   * Gets the source publication date calendar.
   *
   * @return the source publication date calendar
   */
  public Calendar getSourcePublicationDateCalendar() {
    return sourcePublicationDateCalendar;
  }

  /**
   * Sets the source modification date.
   *
   * @param sourceModificationDate the new source modification date
   */
  public void setSourceModificationDate(String sourceModificationDate) {
    this.sourceModificationDate = sourceModificationDate;
  }

  /**
   * Gets the target publication date calendar.
   *
   * @return the target publication date calendar
   */
  public Calendar getTargetPublicationDateCalendar() {
    return targetPublicationDateCalendar;
  }

  /**
   * Sets the target modification date.
   *
   * @param targetModificationDate the new target modification date
   */
  public void setTargetModificationDate(String targetModificationDate) {
    this.targetModificationDate = targetModificationDate;
  }

  /**
   * Sets the source publication date calendar.
   *
   * @param sourcePublicationDateCalendar the new source publication date calendar
   */
  public void setSourcePublicationDateCalendar(Calendar sourcePublicationDateCalendar) {
    this.sourcePublicationDateCalendar = sourcePublicationDateCalendar;
  }

  /**
   * Sets the target publication date calendar.
   *
   * @param targetPublicationDateCalendar the new target publication date calendar
   */
  public void setTargetPublicationDateCalendar(Calendar targetPublicationDateCalendar) {
    this.targetPublicationDateCalendar = targetPublicationDateCalendar;
  }

  /**
   * Gets the target modification date.
   *
   * @return the target modification date
   */
  public String getTargetModificationDate() {
    return targetModificationDate;
  }

  /**
   * Gets the state localized.
   *
   * @return the state localized
   */
  public String getStateLocalized() {
    return state.getLabel(PushContentPopupComponent.getResourceBundle());
  }

  /**
   * Gets the action localized.
   *
   * @return the action localized
   */
  public String getActionLocalized() {
    return state.getAction(PushContentPopupComponent.getResourceBundle());
  }

  /**
   * Sets the state localized.
   *
   * @param fake the new state localized
   */
  // fakeMethod
  public void setStateLocalized(String fake) {
    // nothing to do here
  }

  /**
   * Sets the action localized.
   *
   * @param fake the new action localized
   */
  // fakeMethod
  public void setActionLocalized(String fake) {
    // nothing to do here
  }

  /**
   * Gets the state.
   *
   * @return the state
   */
  public NodeComparisonState getState() {
    return state;
  }

  /**
   * Sets the state.
   *
   * @param state the new state
   */
  public void setState(NodeComparisonState state) {
    this.state = state;
  }

  /**
   * Gets the path.
   *
   * @return the path
   */
  public String getPath() {
    return path;
  }

  /**
   * Sets the path.
   *
   * @param path the new path
   */
  public void setPath(String path) {
    this.path = path;
  }

  /**
   * Checks if is published.
   *
   * @return the boolean
   */
  public Boolean isPublished() {
    return published;
  }

  /**
   * Sets the published.
   *
   * @param publishedOnSource the new published
   */
  public void setPublished(Boolean publishedOnSource) {
    this.published = publishedOnSource;
  }

  /**
   * Sets the source modification date calendar.
   *
   * @param sourceModificationDateCalendar the new source modification date calendar
   */
  public void setSourceModificationDateCalendar(Calendar sourceModificationDateCalendar) {
    this.sourceModificationDate = (sourceModificationDateCalendar != null ? dateFormat.format(sourceModificationDateCalendar.getTime()) : null);
    this.sourceModificationDateCalendar = sourceModificationDateCalendar;
  }

  /**
   * Gets the source modification date calendar.
   *
   * @return the source modification date calendar
   */
  public Calendar getSourceModificationDateCalendar() {
    return sourceModificationDateCalendar;
  }

  /**
   * Sets the target modification date calendar.
   *
   * @param targetModificationDateCalendar the new target modification date calendar
   */
  public void setTargetModificationDateCalendar(Calendar targetModificationDateCalendar) {
    this.targetModificationDate = (targetModificationDateCalendar != null ? dateFormat.format(targetModificationDateCalendar.getTime()) : null);
    this.targetModificationDateCalendar = targetModificationDateCalendar;
  }

  /**
   * Gets the target modification date calendar.
   *
   * @return the target modification date calendar
   */
  public Calendar getTargetModificationDateCalendar() {
    return targetModificationDateCalendar;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int compareTo(NodeComparison o) {
    if (o.getState().equals(getState())) {
      return title.compareTo(o.getTitle());
    } else {
      return getState().compareTo(o.getState());
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(Object obj) {
    if (obj == null || !(obj instanceof NodeComparison)) {
      return false;
    }
    String otherPath = ((NodeComparison) obj).getPath();
    return path != null && otherPath != null && otherPath.equals(path);
  }

}
