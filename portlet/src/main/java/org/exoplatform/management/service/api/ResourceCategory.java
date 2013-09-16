package org.exoplatform.management.service.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User: Thomas Delhoménie
 */
public class ResourceCategory {
  private String label;
  private String path;
  private List<ResourceCategory> subResourceCategories;
  private List<Resource> resources;
  private Map<String, String> exportOptions;
  private Map<String, String> importOptions;

  public ResourceCategory(String path) {
    this.path = path;
    this.subResourceCategories = new ArrayList<ResourceCategory>();
    this.resources = new ArrayList<Resource>();
    this.exportOptions = new HashMap<String, String>();
    this.importOptions = new HashMap<String, String>();
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
}
