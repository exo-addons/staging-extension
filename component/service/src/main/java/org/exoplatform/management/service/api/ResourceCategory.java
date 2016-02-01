package org.exoplatform.management.service.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User: Thomas Delhoménie
 */
public class ResourceCategory implements Comparable<ResourceCategory> {
  private String label;
  private String path;
  private List<ResourceCategory> subResourceCategories;
  private List<Resource> resources;
  private Map<String, String> exportOptions;
  private Map<String, String> importOptions;
  private short order;

  public ResourceCategory(String path) {
    this.path = path;
    this.subResourceCategories = new ArrayList<ResourceCategory>();
    this.resources = new ArrayList<Resource>();
    this.exportOptions = new HashMap<String, String>();
    this.importOptions = new HashMap<String, String>();
    this.order = getOrder(path);
  }

  public ResourceCategory(String label, String path) {
    this(path);
    this.label = label;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public List<ResourceCategory> getSubResourceCategories() {
    return subResourceCategories;
  }

  public void setSubResourceCategories(List<ResourceCategory> subResourceCategories) {
    this.subResourceCategories = subResourceCategories;
  }

  public List<Resource> getResources() {
    return resources;
  }

  public void setResources(List<Resource> resources) {
    this.resources = resources;
  }

  public Map<String, String> getExportOptions() {
    return exportOptions;
  }

  public void setExportOptions(Map<String, String> exportOptions) {
    this.exportOptions = exportOptions;
  }

  public Map<String, String> getImportOptions() {
    return importOptions;
  }

  public void setImportOptions(Map<String, String> importOptions) {
    this.importOptions = importOptions;
  }

  public short getOrder() {
    return order;
  }

  public static short getOrder(String path) {
    short i = 0;
    // JCR NodeType and Namespaces has to be imported at first place
    if (path.startsWith(StagingService.ECM_NODETYPE_PATH)) {
      return i;
    }
    i++;
    // SCRIPTS has to be imported before action script
    if (path.startsWith(StagingService.ECM_SCRIPT_PATH)) {
      return i;
    }
    i++;
    // Gadgets has to be imported before sites
    if (path.startsWith(StagingService.GADGET_PATH)) {
      return i;
    }
    i++;
    // Sites has to be imported before nodetypes
    if (path.startsWith(StagingService.SITES_PORTAL_PATH)) {
      return i;
    }
    i++;
    // Sites has to be imported before nodetypes
    if (path.startsWith(StagingService.SITES_GROUP_PATH)) {
      return i;
    }
    i++;
    // Sites has to be imported before nodetypes
    if (path.startsWith(StagingService.SITES_USER_PATH)) {
      return i;
    }
    i++;
    // View templates has to be imported before View Configuration
    if (path.startsWith(StagingService.ECM_VIEW_TEMPLATES_PATH)) {
      return i;
    }
    i++;
    // View templates Configuration has to be imported before Drives
    // configuration
    if (path.startsWith(StagingService.ECM_VIEW_CONFIGURATION_PATH)) {
      return i;
    }
    return 100;
  }

  @Override
  public int compareTo(ResourceCategory o) {
    if (o == null) {
      return 1;
    }
    return order - o.getOrder();
  }

  @Override
  public String toString() {
    return (resources != null && !resources.isEmpty() ? resources.toString() : ((subResourceCategories != null && !subResourceCategories.isEmpty()) ? subResourceCategories.toString() : ""));
  }
}
