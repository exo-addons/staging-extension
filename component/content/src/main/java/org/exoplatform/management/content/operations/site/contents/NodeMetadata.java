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
package org.exoplatform.management.content.operations.site.contents;

import java.io.Serializable;
import java.util.Calendar;

/**
 * The Class NodeMetadata.
 */
public class NodeMetadata implements Serializable {

  /** The Constant serialVersionUID. */
  private static final long serialVersionUID = -7083164711313946955L;

  /** The path. */
  private String path;
  
  /** The title. */
  private String title;
  
  /** The live date. */
  private Calendar liveDate;
  
  /** The last modification date. */
  private Calendar lastModificationDate;
  
  /** The last modifier. */
  private String lastModifier;
  
  /** The published. */
  private boolean published;

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
   * Gets the live date.
   *
   * @return the live date
   */
  public Calendar getLiveDate() {
    return liveDate;
  }

  /**
   * Sets the live date.
   *
   * @param dateModified the new live date
   */
  public void setLiveDate(Calendar dateModified) {
    this.liveDate = dateModified;
  }

  /**
   * Gets the last modifier.
   *
   * @return the last modifier
   */
  public String getLastModifier() {
    return lastModifier;
  }

  /**
   * Sets the last modifier.
   *
   * @param lastModifier the new last modifier
   */
  public void setLastModifier(String lastModifier) {
    this.lastModifier = lastModifier;
  }

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
   * Checks if is published.
   *
   * @return true, if is published
   */
  public boolean isPublished() {
    return published;
  }

  /**
   * Sets the published.
   *
   * @param published the new published
   */
  public void setPublished(boolean published) {
    this.published = published;
  }

  /**
   * Sets the last modification date.
   *
   * @param lastModificationDate the new last modification date
   */
  public void setLastModificationDate(Calendar lastModificationDate) {
    this.lastModificationDate = lastModificationDate;
  }

  /**
   * Gets the last modification date.
   *
   * @return the last modification date
   */
  public Calendar getLastModificationDate() {
    return lastModificationDate;
  }

}
