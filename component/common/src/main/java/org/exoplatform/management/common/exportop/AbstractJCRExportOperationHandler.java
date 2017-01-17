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
package org.exoplatform.management.common.exportop;

import org.apache.commons.lang.ArrayUtils;
import org.exoplatform.services.cms.templates.TemplateService;
import org.gatein.management.api.operation.model.ExportTask;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;

/**
 * The Class AbstractJCRExportOperationHandler.
 */
public abstract class AbstractJCRExportOperationHandler extends AbstractExportOperationHandler {

  /** The template service. */
  protected TemplateService templateService = null;

  /** The is NT recursive map. */
  private Map<String, Boolean> isNTRecursiveMap = new HashMap<String, Boolean>();

  /**
   * Export node.
   *
   * @param childNode the child node
   * @param subNodesExportTask the sub nodes export task
   * @param params the params
   * @throws Exception the exception
   */
  protected final void exportNode(Node childNode, List<ExportTask> subNodesExportTask, String... params) throws Exception {
    String path = childNode.getPath();
    boolean recursive = isRecursiveExport(childNode);
    addJCRNodeExportTask(childNode, subNodesExportTask, recursive, ((String[]) ArrayUtils.add(params, path)));

    // If not export the whole node
    if (!recursive) {
      NodeIterator nodeIterator = childNode.getNodes();
      while (nodeIterator.hasNext()) {
        Node node = nodeIterator.nextNode();
        exportNode(node, subNodesExportTask, params);
      }
    }
  }

  /**
   * Checks if is recursive export.
   *
   * @param node the node
   * @return true, if is recursive export
   * @throws Exception the exception
   */
  protected final boolean isRecursiveExport(Node node) throws Exception {
    NodeType nodeType = node.getPrimaryNodeType();
    NodeType[] nodeTypes = node.getMixinNodeTypes();
    boolean recursive = isRecursiveNT(nodeType);
    if (!recursive && nodeTypes != null && nodeTypes.length > 0) {
      int i = 0;
      while (!recursive && i < nodeTypes.length) {
        recursive = isRecursiveNT(nodeTypes[i]);
        i++;
      }
    }
    return recursive;
  }

  /**
   * Checks if is recursive NT.
   *
   * @param nodeType the node type
   * @return true, if is recursive NT
   * @throws Exception the exception
   */
  protected final boolean isRecursiveNT(NodeType nodeType) throws Exception {
    if (nodeType.getName().equals("exo:actionStorage")) {
      return true;
    }
    if (!isNTRecursiveMap.containsKey(nodeType.getName())) {
      boolean hasMandatoryChild = false;
      NodeDefinition[] nodeDefinitions = nodeType.getChildNodeDefinitions();
      if (nodeDefinitions != null) {
        int i = 0;
        while (!hasMandatoryChild && i < nodeDefinitions.length) {
          hasMandatoryChild = nodeDefinitions[i].isMandatory();
          i++;
        }
      }
      boolean recursive = hasMandatoryChild;
      if (templateService != null) {
        recursive |= templateService.isManagedNodeType(nodeType.getName());
      }
      isNTRecursiveMap.put(nodeType.getName(), recursive);
    }
    return isNTRecursiveMap.get(nodeType.getName());
  }

  /**
   * Adds the JCR node export task.
   *
   * @param childNode the child node
   * @param subNodesExportTask the sub nodes export task
   * @param recursive the recursive
   * @param params the params
   * @throws Exception the exception
   */
  protected abstract void addJCRNodeExportTask(Node childNode, List<ExportTask> subNodesExportTask, boolean recursive, String... params) throws Exception;
}
