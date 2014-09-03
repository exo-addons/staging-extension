/*
 * Copyright (C) 2003-2014 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.management.answer.operations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;

import org.exoplatform.faq.service.Category;
import org.exoplatform.faq.service.FAQService;
import org.exoplatform.faq.service.Utils;
import org.exoplatform.forum.common.jcr.KSDataLocation;
import org.exoplatform.management.answer.AnswerExtension;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.social.core.space.SpaceUtils;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.gatein.common.logging.Logger;
import org.gatein.common.logging.LoggerFactory;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.exceptions.ResourceNotFoundException;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.OperationHandler;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.ExportResourceModel;
import org.gatein.management.api.operation.model.ExportTask;

/**
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class AnswerDataExportResource implements OperationHandler {

  final private static Logger log = LoggerFactory.getLogger(AnswerDataExportResource.class);

  private RepositoryService repositoryService;
  private SpaceService spaceService;
  private FAQService faqService;
  private KSDataLocation dataLocation;

  private boolean isSpaceType;
  private String type;

  private Map<String, Boolean> isNTRecursiveMap = new HashMap<String, Boolean>();

  public AnswerDataExportResource(boolean isSpaceType) {
    this.isSpaceType = isSpaceType;
    this.type = isSpaceType ? AnswerExtension.SPACE_FAQ_TYPE : AnswerExtension.PUBLIC_FAQ_TYPE;
  }

  @Override
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws ResourceNotFoundException, OperationException {
    spaceService = operationContext.getRuntimeContext().getRuntimeComponent(SpaceService.class);
    faqService = operationContext.getRuntimeContext().getRuntimeComponent(FAQService.class);
    repositoryService = operationContext.getRuntimeContext().getRuntimeComponent(RepositoryService.class);
    dataLocation = operationContext.getRuntimeContext().getRuntimeComponent(KSDataLocation.class);

    String name = operationContext.getAttributes().getValue("filter");

    String excludeSpaceMetadataString = operationContext.getAttributes().getValue("exclude-space-metadata");
    boolean exportSpaceMetadata = excludeSpaceMetadataString == null || excludeSpaceMetadataString.trim().equalsIgnoreCase("false");

    List<ExportTask> exportTasks = new ArrayList<ExportTask>();

    String workspace = null;
    try {
      workspace = dataLocation.getWorkspace();
    } catch (Exception e) {
      throw new OperationException(OperationNames.EXPORT_RESOURCE, "Can't get Answers workspace", e);
    }
    String categoryHomePath = dataLocation.getFaqCategoriesLocation();

    if (name == null || name.isEmpty()) {
      log.info("Exporting all FAQ of type: " + (isSpaceType ? "Spaces" : "Public"));
      try {
        List<Category> categories = faqService.getAllCategories();
        for (Category category : categories) {
          if (category.getId().startsWith(Utils.CATE_SPACE_ID_PREFIX)) {
            continue;
          }
          exportAnswer(exportTasks, workspace, categoryHomePath, category.getId(), null, exportSpaceMetadata);
        }
      } catch (Exception e) {
        log.error("Error while exporting FAQ categories.", e);
      }
    } else {
      if (isSpaceType) {
        Space space = spaceService.getSpaceByDisplayName(name);
        String groupName = space.getGroupId().replace(SpaceUtils.SPACE_GROUP + "/", "");
        exportAnswer(exportTasks, workspace, categoryHomePath, Utils.CATE_SPACE_ID_PREFIX + groupName, space, exportSpaceMetadata);
      } else {
        try {
          if (name.equals(AnswerExtension.ROOT_CATEGORY)) {
            // Export questions from root category
            exportAnswer(exportTasks, workspace, categoryHomePath, Utils.QUESTION_HOME, null, exportSpaceMetadata);
          } else {
            List<Category> categories = faqService.getAllCategories();
            for (Category category : categories) {
              if (!category.getName().equals(name)) {
                continue;
              }
              exportAnswer(exportTasks, workspace, categoryHomePath, category.getId(), null, exportSpaceMetadata);
            }
          }
        } catch (Exception e) {
          log.error("Error while exporting FAQ categories.", e);
        }
      }
    }
    resultHandler.completed(new ExportResourceModel(exportTasks));
  }

  private void exportAnswer(List<ExportTask> exportTasks, String workspace, String categoryHomePath, String categoryId, Space space, boolean exportSpaceMetadata) {
    try {
      String parentNodePath = "/" + categoryHomePath + "/" + categoryId;
      Session session = getSession(workspace);
      if (!session.itemExists(parentNodePath)) {
        log.warn("FAQ export: '" + parentNodePath + "' doesn't exists, ignore export this category.");
        return;
      }
      Node parentNode = (Node) session.getItem(parentNodePath);
      exportNode(workspace, parentNode, categoryId, exportTasks);

      if (exportSpaceMetadata && isSpaceType) {
        exportTasks.add(new SpaceMetadataExportTask(space, categoryId));
      }
    } catch (Exception exception) {
      throw new OperationException(OperationNames.EXPORT_RESOURCE, "Error while exporting FAQ", exception);
    }
  }

  private void exportNode(String workspace, Node node, String categoryId, List<ExportTask> subNodesExportTask) throws Exception {
    boolean recursive = isRecursiveExport(node);
    AnswerExportTask faqExportTask = new AnswerExportTask(repositoryService, type, categoryId, workspace, node.getPath(), recursive);
    subNodesExportTask.add(faqExportTask);
    // If not export the whole node
    if (!recursive) {
      NodeIterator nodeIterator = node.getNodes();
      while (nodeIterator.hasNext()) {
        Node childNode = nodeIterator.nextNode();
        exportNode(workspace, childNode, categoryId, subNodesExportTask);
      }
    }
  }

  private Session getSession(String workspace) throws Exception {
    SessionProvider provider = SessionProvider.createSystemProvider();
    ManageableRepository repository = repositoryService.getCurrentRepository();
    Session session = provider.getSession(workspace, repository);
    return session;
  }

  private boolean isRecursiveExport(Node node) throws Exception {
    // FIXME: eXo ECMS bug, items with exo:actionnable don't define manatory
    // field exo:actions. Still use this workaround. ECMS-5998
    if (node.isNodeType("exo:actionable") && !node.hasProperty("exo:actions")) {
      node.setProperty("exo:actions", "");
      node.save();
      node.getSession().refresh(true);
    }
    // END workaround

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

  private boolean isRecursiveNT(NodeType nodeType) throws Exception {
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
      isNTRecursiveMap.put(nodeType.getName(), hasMandatoryChild);
    }
    return isNTRecursiveMap.get(nodeType.getName());
  }

}
