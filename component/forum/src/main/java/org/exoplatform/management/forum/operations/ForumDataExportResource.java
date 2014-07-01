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
package org.exoplatform.management.forum.operations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;

import org.exoplatform.forum.common.jcr.KSDataLocation;
import org.exoplatform.forum.service.Category;
import org.exoplatform.forum.service.ForumService;
import org.exoplatform.forum.service.Utils;
import org.exoplatform.management.forum.ForumExtension;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
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
public class ForumDataExportResource implements OperationHandler {

  final private static Logger log = LoggerFactory.getLogger(ForumDataExportResource.class);

  private RepositoryService repositoryService;
  private SpaceService spaceService;
  private ForumService forumService;
  private KSDataLocation dataLocation;

  private boolean isSpaceForumType;
  private String type;

  private Map<String, Boolean> isNTRecursiveMap = new HashMap<String, Boolean>();

  public ForumDataExportResource(boolean isSpaceForumType) {
    this.isSpaceForumType = isSpaceForumType;
    this.type = isSpaceForumType ? ForumExtension.SPACE_FORUM_TYPE : ForumExtension.PUBLIC_FORUM_TYPE;
  }

  @Override
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws ResourceNotFoundException, OperationException {
    spaceService = operationContext.getRuntimeContext().getRuntimeComponent(SpaceService.class);
    forumService = operationContext.getRuntimeContext().getRuntimeComponent(ForumService.class);
    repositoryService = operationContext.getRuntimeContext().getRuntimeComponent(RepositoryService.class);
    dataLocation = operationContext.getRuntimeContext().getRuntimeComponent(KSDataLocation.class);

    String name = operationContext.getAttributes().getValue("filter");

    String excludeSpaceMetadataString = operationContext.getAttributes().getValue("exclude-space-metadata");
    boolean exportSpaceMetadata = excludeSpaceMetadataString == null || excludeSpaceMetadataString.trim().equalsIgnoreCase("false");

    List<ExportTask> exportTasks = new ArrayList<ExportTask>();

    Category spaceCategory = forumService.getCategoryIncludedSpace();
    String workspace = dataLocation.getWorkspace();
    String categoryHomePath = dataLocation.getForumCategoriesLocation();

    if (name == null || name.isEmpty()) {
      log.info("Exporting all Forums of type: " + (isSpaceForumType ? "Spaces" : "Non Spaces"));
      List<Category> categories = forumService.getCategories();
      for (Category category : categories) {
        if (spaceCategory != null && category.getId().equals(spaceCategory.getId())) {
          continue;
        }
        exportForum(exportTasks, workspace, categoryHomePath, category.getId(), null, exportSpaceMetadata);
      }
    } else {
      if (isSpaceForumType) {
        if (spaceCategory != null) {
          Space space = spaceService.getSpaceByDisplayName(name);
          exportForum(exportTasks, workspace, categoryHomePath, spaceCategory.getId(), space.getPrettyName(), exportSpaceMetadata);
        }
      } else {
        List<Category> categories = forumService.getCategories();
        for (Category category : categories) {
          if (!category.getCategoryName().equals(name)) {
            continue;
          }
          exportForum(exportTasks, workspace, categoryHomePath, category.getId(), null, exportSpaceMetadata);
        }
      }
    }
    resultHandler.completed(new ExportResourceModel(exportTasks));
  }

  private void exportForum(List<ExportTask> exportTasks, String workspace, String categoryHomePath, String categoryId, String spacePrettyName, boolean exportSpaceMetadata) {
    try {
      String forumId = (spacePrettyName == null ? "" : Utils.FORUM_SPACE_ID_PREFIX + spacePrettyName);
      String parentNodePath = "/" + categoryHomePath + "/" + categoryId + (forumId.isEmpty() ? "" : "/" + forumId);
      Session session = getSession(workspace);
      Node parentNode = (Node) session.getItem(parentNodePath);
      exportNode(workspace, parentNode, categoryId, forumId, exportTasks);

      if (exportSpaceMetadata && isSpaceForumType) {
        Space space = spaceService.getSpaceByPrettyName(spacePrettyName);
        exportTasks.add(new SpaceMetadataExportTask(space, forumId));
      }
    } catch (Exception exception) {
      throw new OperationException(OperationNames.EXPORT_RESOURCE, "Error while exporting forum", exception);
    }
  }

  private void exportNode(String workspace, Node node, String categoryId, String forumId, List<ExportTask> subNodesExportTask) throws Exception {
    boolean recursive = isRecursiveExport(node);
    ForumExportTask forumExportTask = new ForumExportTask(repositoryService, type, categoryId, forumId, workspace, node.getPath(), recursive);
    subNodesExportTask.add(forumExportTask);
    // If not export the whole node
    if (!recursive) {
      NodeIterator nodeIterator = node.getNodes();
      while (nodeIterator.hasNext()) {
        Node childNode = nodeIterator.nextNode();
        exportNode(workspace, childNode, categoryId, forumId, subNodesExportTask);
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
