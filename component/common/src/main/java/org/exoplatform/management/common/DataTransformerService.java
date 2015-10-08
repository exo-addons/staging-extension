package org.exoplatform.management.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

public class DataTransformerService {
  private static final Log LOG = ExoLogger.getLogger(DataTransformerService.class);
  private static final Map<String, List<DataTransformerPlugin>> PLUGINS = new HashMap<String, List<DataTransformerPlugin>>();

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
