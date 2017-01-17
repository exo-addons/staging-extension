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

import java.util.HashMap;
import java.util.Map;

/**
 * The Class SiteMetaData.
 */
public class SiteMetaData {

  /** The Constant SITE_PATH. */
  public static final String SITE_PATH = "site-path";
  
  /** The Constant SITE_WORKSPACE. */
  public static final String SITE_WORKSPACE = "site-workspace";
  
  /** The Constant SITE_NAME. */
  public static final String SITE_NAME = "site-name";

  /** The options. */
  Map<String, String> options = new HashMap<String, String>();
  
  /** The nodes metadata. */
  Map<String, NodeMetadata> nodesMetadata = new HashMap<String, NodeMetadata>();

  /**
   * Gets the options.
   *
   * @return the options
   */
  public Map<String, String> getOptions() {
    return this.options;
  }

  /**
   * Sets the options.
   *
   * @param options the options
   */
  public void setOptions(Map<String, String> options) {
    this.options = options;
  }

  /**
   * Gets the nodes metadata.
   *
   * @return the nodes metadata
   */
  public Map<String, NodeMetadata> getNodesMetadata() {
    return nodesMetadata;
  }

  /**
   * Sets the nodes metadata.
   *
   * @param nodesMetadata the nodes metadata
   */
  public void setNodesMetadata(Map<String, NodeMetadata> nodesMetadata) {
    this.nodesMetadata = nodesMetadata;
  }

}
