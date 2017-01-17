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
package org.exoplatform.management.ecmadmin.operations.templates.applications;

import java.util.HashMap;
import java.util.Map;

/**
 * The Class ApplicationTemplatesMetadata.
 *
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class ApplicationTemplatesMetadata {

  /** Metadata of Application Templates: Map of (Path,Title). */
  private Map<String, String> titleMap;

  /**
   * Instantiates a new application templates metadata.
   */
  public ApplicationTemplatesMetadata() {
    this.titleMap = new HashMap<String, String>();
  }

  /**
   * Gets the metadata of Application Templates: Map of (Path,Title).
   *
   * @return the metadata of Application Templates: Map of (Path,Title)
   */
  public Map<String, String> getTitleMap() {
    return titleMap;
  }

  /**
   * Adds the title.
   *
   * @param path the path
   * @param title the title
   */
  public void addTitle(String path, String title) {
    titleMap.put(path, title);
  }

  /**
   * Gets the title.
   *
   * @param path the path
   * @return the title
   */
  public String getTitle(String path) {
    return titleMap.get(path);
  }

}
