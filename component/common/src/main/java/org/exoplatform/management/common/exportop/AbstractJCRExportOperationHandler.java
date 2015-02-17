package org.exoplatform.management.common.exportop;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;

import org.apache.commons.lang.ArrayUtils;
import org.exoplatform.services.cms.templates.TemplateService;
import org.gatein.management.api.operation.model.ExportTask;

public abstract class AbstractJCRExportOperationHandler extends AbstractExportOperationHandler {

  protected TemplateService templateService = null;

  private Map<String, Boolean> isNTRecursiveMap = new HashMap<String, Boolean>();

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

  protected final boolean isRecursiveExport(Node node) throws Exception {
    // FIXME: eXo ECMS bug, items with exo:actionnable don't define manatory
    // field exo:actions. Still use this workaround. ECMS-5998
    if (node.isNodeType("exo:actionable") && !node.hasProperty("exo:actions")) {
      node.setProperty("exo:actions", "");
      node.save();
      node.getSession().refresh(true);
    }
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

  protected abstract void addJCRNodeExportTask(Node childNode, List<ExportTask> subNodesExportTask, boolean recursive, String... params) throws Exception;
}
