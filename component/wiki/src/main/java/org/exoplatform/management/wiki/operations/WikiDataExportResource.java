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
package org.exoplatform.management.wiki.operations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;

import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.wiki.mow.api.Wiki;
import org.exoplatform.wiki.mow.api.WikiType;
import org.exoplatform.wiki.mow.core.api.MOWService;
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
 * Created by The eXo Platform SAS Author : eXoPlatform exo@exoplatform.com Mar
 * 5, 2014
 */
public class WikiDataExportResource implements OperationHandler {

  final private static Logger log = LoggerFactory.getLogger(WikiDataExportResource.class);

  private MOWService mowService;

  private WikiType wikiType;

  private Map<String, Boolean> isNTRecursiveMap = new HashMap<String, Boolean>();

  private RepositoryService repositoryService;
  private SpaceService spaceService;

  public WikiDataExportResource(WikiType wikiType) {
    this.wikiType = wikiType;
  }

  @Override
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws ResourceNotFoundException, OperationException {
    spaceService = operationContext.getRuntimeContext().getRuntimeComponent(SpaceService.class);
    mowService = operationContext.getRuntimeContext().getRuntimeComponent(MOWService.class);
    repositoryService = operationContext.getRuntimeContext().getRuntimeComponent(RepositoryService.class);

    String wikiOwner = operationContext.getAttributes().getValue("filter");

    List<ExportTask> exportTasks = new ArrayList<ExportTask>();
    if (wikiOwner == null || wikiOwner.isEmpty()) {
      log.info("Exporting all WIKI of type: " + wikiType);
      for (Wiki wiki : mowService.getModel().getWikiStore().getWikiContainer(wikiType).getAllWikis()) {
        exportWiki(exportTasks, wiki);
      }
    } else {
      exportWiki(exportTasks, mowService.getModel().getWikiStore().getWikiContainer(wikiType).contains(wikiOwner));
    }
    resultHandler.completed(new ExportResourceModel(exportTasks));
  }

  private void exportWiki(List<ExportTask> exportTasks, Wiki wiki) {
    if (wiki == null) {
      log.warn("Operation exportWiki: Wiki is null.");
      return;
    }
    try {
      Node wikiNode = wiki.getWikiHome().getJCRPageNode().getParent();
      String workspace = wikiNode.getSession().getWorkspace().getName();
      exportNode(wikiNode, wiki.getOwner(), workspace, exportTasks);

      if (WikiType.GROUP.name().equalsIgnoreCase(wiki.getType()) && wiki.getOwner().startsWith("/spaces/")) {
        Space space = spaceService.getSpaceByGroupId(wiki.getOwner());
        exportTasks.add(new SpaceMetadataExportTask(space, wiki.getOwner()));
      }
    } catch (Exception exception) {
      throw new OperationException(OperationNames.EXPORT_RESOURCE, "Error while exporting wiki", exception);
    }
  }

  private void exportNode(Node childNode, String workspace, String wikiOwner, List<ExportTask> subNodesExportTask) throws Exception {
    String path = childNode.getPath();
    boolean recursive = isRecursiveExport(childNode);
    WikiExportTask wikiExportTask = new WikiExportTask(repositoryService, wikiType, workspace, wikiOwner, path, recursive);
    subNodesExportTask.add(wikiExportTask);
    // If not export the whole node
    if (!recursive) {
      NodeIterator nodeIterator = childNode.getNodes();
      while (nodeIterator.hasNext()) {
        Node node = nodeIterator.nextNode();
        exportNode(node, workspace, wikiOwner, subNodesExportTask);
      }
    }
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
