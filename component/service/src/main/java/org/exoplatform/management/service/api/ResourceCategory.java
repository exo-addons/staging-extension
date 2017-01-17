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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User: Thomas Delhoménie.
 */
public class ResourceCategory implements Comparable<ResourceCategory> {
  
  /** The label. */
  private String label;
  
  /** The path. */
  private String path;
  
  /** The sub resource categories. */
  private List<ResourceCategory> subResourceCategories;
  
  /** The resources. */
  private List<Resource> resources;
  
  /** The export options. */
  private Map<String, String> exportOptions;
  
  /** The import options. */
  private Map<String, String> importOptions;
  
  /** The order. */
  private short order;

  /**
   * Instantiates a new resource category.
   *
   * @param path the path
   */
  public ResourceCategory(String path) {
    this.path = path;
    this.subResourceCategories = new ArrayList<ResourceCategory>();
    this.resources = new ArrayList<Resource>();
    this.exportOptions = new HashMap<String, String>();
    this.importOptions = new HashMap<String, String>();
    this.order = getOrder(path);
  }

  /**
   * Instantiates a new resource category.
   *
   * @param label the label
   * @param path the path
   */
  public ResourceCategory(String label, String path) {
    this(path);
    this.label = label;
  }

  /**
   * Gets the label.
   *
   * @return the label
   */
  public String getLabel() {
    return label;
  }

  /**
   * Sets the label.
   *
   * @param label the new label
   */
  public void setLabel(String label) {
    this.label = label;
  }

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
   * Gets the sub resource categories.
   *
   * @return the sub resource categories
   */
  public List<ResourceCategory> getSubResourceCategories() {
    return subResourceCategories;
  }

  /**
   * Sets the sub resource categories.
   *
   * @param subResourceCategories the new sub resource categories
   */
  public void setSubResourceCategories(List<ResourceCategory> subResourceCategories) {
    this.subResourceCategories = subResourceCategories;
  }

  /**
   * Gets the resources.
   *
   * @return the resources
   */
  public List<Resource> getResources() {
    return resources;
  }

  /**
   * Sets the resources.
   *
   * @param resources the new resources
   */
  public void setResources(List<Resource> resources) {
    this.resources = resources;
  }

  /**
   * Gets the export options.
   *
   * @return the export options
   */
  public Map<String, String> getExportOptions() {
    return exportOptions;
  }

  /**
   * Sets the export options.
   *
   * @param exportOptions the export options
   */
  public void setExportOptions(Map<String, String> exportOptions) {
    this.exportOptions = exportOptions;
  }

  /**
   * Gets the import options.
   *
   * @return the import options
   */
  public Map<String, String> getImportOptions() {
    return importOptions;
  }

  /**
   * Sets the import options.
   *
   * @param importOptions the import options
   */
  public void setImportOptions(Map<String, String> importOptions) {
    this.importOptions = importOptions;
  }

  /**
   * Gets the order.
   *
   * @return the order
   */
  public short getOrder() {
    return order;
  }

  /**
   * Gets the order.
   *
   * @param path the path
   * @return the order
   */
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

  /**
   * {@inheritDoc}
   */
  @Override
  public int compareTo(ResourceCategory o) {
    if (o == null) {
      return 1;
    }
    return order - o.getOrder();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return (resources != null && !resources.isEmpty() ? resources.toString() : ((subResourceCategories != null && !subResourceCategories.isEmpty()) ? subResourceCategories.toString() : ""));
  }
}
