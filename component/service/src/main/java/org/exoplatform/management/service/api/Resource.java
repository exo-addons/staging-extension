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
package org.exoplatform.management.service.api;

import java.io.Serializable;

/**
 * The Class Resource.
 */
public class Resource implements Serializable {
  
  /** The Constant serialVersionUID. */
  private static final long serialVersionUID = -8330709372395885654L;

  /** The path. */
  private String path;
  
  /** The text. */
  private String text;
  
  /** The description. */
  private String description;

  /**
   * Instantiates a new resource.
   *
   * @param path the path
   * @param text the text
   * @param description the description
   */
  public Resource(String path, String text, String description) {
    this.text = text;
    this.path = path;
    this.description = description;
  }

  /**
   * Gets the text.
   *
   * @return the text
   */
  public String getText() {
    return text;
  }

  /**
   * Sets the text.
   *
   * @param text the new text
   */
  public void setText(String text) {
    this.text = text;
  }

  /**
   * Gets the path.
   *
   * @return the path
   */
  public String getPath() {
    return this.path;
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
   * Gets the description.
   *
   * @return the description
   */
  public String getDescription() {
    return description;
  }

  /**
   * Sets the description.
   *
   * @param description the new description
   */
  public void setDescription(String description) {
    this.description = description;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    } else if (obj instanceof Resource) {
      return text.equals(((Resource) obj).getPath());
    } else {
      return false;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    return path.hashCode();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return path;
  }
}
