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

import java.util.ResourceBundle;

/**
 * The Enum NodeComparisonState.
 */
public enum NodeComparisonState
{
  
  /** The not found on target. */
  NOT_FOUND_ON_TARGET("not_found_target"),
  
  /** The modified on source. */
  MODIFIED_ON_SOURCE("modified_source"),
  
  /** The modified on target. */
  MODIFIED_ON_TARGET("modified_target"),
  
  /** The not found on source. */
  NOT_FOUND_ON_SOURCE("not_found_source"),
  
  /** The same. */
  SAME("same"),
  
  /** The unknown. */
  UNKNOWN("unkown");
  
  /** The key. */
  String key;

  /**
   * Instantiates a new node comparison state.
   *
   * @param key the key
   */
  private NodeComparisonState(String key) {
    this.key = key;
  }

  /**
   * Gets the key.
   *
   * @return the key
   */
  public String getKey() {
    return key;
  }

  /**
   * Gets the label.
   *
   * @param resourceBundle the resource bundle
   * @return the label
   */
  public String getLabel(ResourceBundle resourceBundle) {
    if (resourceBundle != null) {
      return resourceBundle.getString("PushContent.state." + key);
    }
    return key;
  }

  /**
   * Gets the action.
   *
   * @param resourceBundle the resource bundle
   * @return the action
   */
  public String getAction(ResourceBundle resourceBundle) {
    if (resourceBundle != null) {
      return resourceBundle.getString("PushContent.action." + key);
    }
    return key;
  }

}
