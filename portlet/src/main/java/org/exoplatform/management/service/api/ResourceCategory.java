package org.exoplatform.management.service.api;

import java.util.ArrayList;
import java.util.List;

/**
 * User: Thomas Delhoménie
 */
public class ResourceCategory {
  private String id;
  private String label;
  private String path;
  private List<ResourceCategory> subResourceCategories;

  public ResourceCategory(String id, String label, String path) {
    this.id = id;
    this.label = label;
    this.path = path;
    this.subResourceCategories = new ArrayList<ResourceCategory>();
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
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
}
