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
package org.exoplatform.management.ecmadmin.operations.templates.nodetypes;

import org.exoplatform.management.ecmadmin.operations.templates.NodeTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The Class NodeTypeTemplatesMetaData.
 */
public class NodeTypeTemplatesMetaData {

  /** The label. */
  private String label;
  
  /** The node type name. */
  private String nodeTypeName;
  
  /** The document template. */
  private boolean documentTemplate;
  
  /** The templates. */
  private Map<String, List<NodeTemplate>> templates;

  /**
   * Instantiates a new node type templates meta data.
   */
  public NodeTypeTemplatesMetaData() {
    this.templates = new HashMap<String, List<NodeTemplate>>();
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
   * Checks if is document template.
   *
   * @return true, if is document template
   */
  public boolean isDocumentTemplate() {
    return documentTemplate;
  }

  /**
   * Sets the document template.
   *
   * @param documentTemplate the new document template
   */
  public void setDocumentTemplate(boolean documentTemplate) {
    this.documentTemplate = documentTemplate;
  }

  /**
   * Gets the templates.
   *
   * @return the templates
   */
  public Map<String, List<NodeTemplate>> getTemplates() {
    return templates;
  }

  /**
   * Sets the templates.
   *
   * @param templates the templates
   */
  public void setTemplates(Map<String, List<NodeTemplate>> templates) {
    this.templates = templates;
  }

  /**
   * Adds the template.
   *
   * @param type the type
   * @param template the template
   */
  public void addTemplate(String type, NodeTemplate template) {
    List<NodeTemplate> typeTemplates = templates.get(type);
    if (typeTemplates == null) {
      typeTemplates = new ArrayList<NodeTemplate>();
    }

    typeTemplates.add(template);
    templates.put(type, typeTemplates);
  }

  /**
   * Gets the node type name.
   *
   * @return the node type name
   */
  public String getNodeTypeName() {
    return nodeTypeName;
  }

  /**
   * Sets the node type name.
   *
   * @param nodeTypeName the new node type name
   */
  public void setNodeTypeName(String nodeTypeName) {
    this.nodeTypeName = nodeTypeName;
  }

}
