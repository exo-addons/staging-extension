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
package org.exoplatform.management.common;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The Class DataTransformerService.
 */
public class DataTransformerService {
  
  /** The Constant LOG. */
  private static final Log LOG = ExoLogger.getLogger(DataTransformerService.class);
  
  /** The Constant PLUGINS. */
  private static final Map<String, List<DataTransformerPlugin>> PLUGINS = new HashMap<String, List<DataTransformerPlugin>>();

  /**
   * Adds the transformer.
   *
   * @param name the name
   * @param plugin the plugin
   * @return the data transformer plugin
   */
  public static DataTransformerPlugin addTransformer(String name, DataTransformerPlugin plugin) {
    if (StringUtils.isEmpty(name)) {
      throw new IllegalArgumentException("addTransformer: Data Transformer name can't be empty.");
    }
    if (plugin == null) {
      throw new IllegalArgumentException("addTransformer: Data Transformer Plugin can't be null.");
    }
    if (!PLUGINS.containsKey(name)) {
      PLUGINS.put(name, new ArrayList<DataTransformerPlugin>());
    }
    PLUGINS.get(name).add(plugin);
    return plugin;
  }

  /**
   * Export data.
   *
   * @param name the name
   * @param objects the objects
   */
  public static void exportData(String name, Object... objects) {
    if (StringUtils.isEmpty(name)) {
      throw new IllegalArgumentException("exportData: Data Transformer name can't be empty.");
    }
    if (!PLUGINS.containsKey(name)) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("No transformer for plugin name: " + name);
      }
      return;
    }
    for (DataTransformerPlugin plugin : PLUGINS.get(name)) {
      plugin.exportData(objects);
    }
  }

  /**
   * Import data.
   *
   * @param name the name
   * @param objects the objects
   */
  public static void importData(String name, Object... objects) {
    if (StringUtils.isEmpty(name)) {
      throw new IllegalArgumentException("exportData: Data Transformer name can't be empty.");
    }
    if (!PLUGINS.containsKey(name)) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("No transformer for plugin name: " + name);
      }
      return;
    }
    for (DataTransformerPlugin plugin : PLUGINS.get(name)) {
      plugin.importData(objects);
    }
  }
}
