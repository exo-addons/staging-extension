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
package org.exoplatform.management.content.operations.site;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.management.common.AbstractOperationHandler;
import org.exoplatform.management.common.DataTransformerPlugin;
import org.exoplatform.portal.config.DataStorage;
import org.exoplatform.portal.config.model.Application;
import org.exoplatform.portal.config.model.ApplicationType;
import org.exoplatform.portal.config.model.Container;
import org.exoplatform.portal.config.model.Page;
import org.exoplatform.portal.pom.spi.portlet.Portlet;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.wcm.webui.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.jcr.Node;

/**
 * The Class PageCLVTransformer.
 */
public class PageCLVTransformer implements DataTransformerPlugin {

  /** The Constant LOG. */
  private static final Log LOG = ExoLogger.getLogger(PageCLVTransformer.class);

  /**
   * {@inheritDoc}
   */
  @Override
  public void exportData(Object... objects) {
    if (objects == null || objects.length == 0) {
      LOG.warn("Can't transform data of empty pages.");
      return;
    }
    for (Object object : objects) {
      if (object != null && object instanceof Page) {
        Page page = (Page) object;
        convert(page.getChildren());
      } else {
        LOG.warn("Can't convert object of type " + object.getClass());
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void importData(Object... objects) {}

  /**
   * Convert.
   *
   * @param children the children
   */
  private static void convert(List<?> children) {
    if ((children == null) || (children.size() == 0)) {
      return;
    }
    for (Object child : children) {
      if (child instanceof Container) {
        Container container = (Container) child;
        convert(container.getChildren());
      } else if (child instanceof Application && ((Application<?>) child).getType().equals(ApplicationType.PORTLET)) {
        @SuppressWarnings("unchecked")
        Application<Portlet> application = (Application<Portlet>) child;
        try {
          Portlet portlet = getPreferences(application);

          String folderPath = portlet == null || portlet.getPreference("folderPath") == null ? null : portlet.getPreference("folderPath").getValue();
          if (!StringUtils.isEmpty(folderPath)) {
            String newFolderPath = convertPath(folderPath);
            portlet.setValue("folderPath", newFolderPath);
          }
        } catch (Exception e) {
          LOG.error("Can't convert CLV application preferences " + application.getId(), e);
        }
      }
    }
  }

  /**
   * Convert path.
   *
   * @param originalPath the original path
   * @return the string
   */
  public static String convertPath(String originalPath) {
    // check if the path is alive
    if (StringUtils.isEmpty(originalPath)) {
      return originalPath;
    }
    List<String> tmpItems = new ArrayList<String>();
    StringBuffer itemsBuffer = new StringBuffer();
    if (originalPath.contains(";")) {
      tmpItems = Arrays.asList(originalPath.split(";"));
    } else {
      tmpItems.add(originalPath);
    }
    // only add exist Node
    for (String item : tmpItems) {
      try {
        Node realNode = getRealNode(item);
        if (realNode != null) {
          String repository = ((ManageableRepository) realNode.getSession().getRepository()).getConfiguration().getName();
          String workspace = realNode.getSession().getWorkspace().getName();
          if (!item.contains(realNode.getPath())) {
            LOG.info("Transform 'folderPath' parameter from'" + item + "' to '" + realNode.getPath() + "'");
          }
          itemsBuffer.append(repository).append(":").append(workspace).append(":").append(realNode.getPath()).append(";");
        }
      } catch (Exception e) {
        if (LOG.isDebugEnabled()) {
          LOG.debug(e.getMessage());
        }
      }
    }
    return itemsBuffer.toString();
  }

  /**
   * Gets the real node.
   *
   * @param itemPath the item path
   * @return the real node
   * @throws Exception the exception
   */
  private static Node getRealNode(String itemPath) throws Exception {
    String workspace, identifier;
    int repoIndex, wsIndex;
    if (itemPath == null || itemPath.length() == 0)
      return null;
    repoIndex = itemPath.indexOf(':');
    wsIndex = itemPath.lastIndexOf(':');
    workspace = itemPath.substring(repoIndex + 1, wsIndex);
    identifier = itemPath.substring(wsIndex + 1);
    RepositoryService repositoryService = (RepositoryService) PortalContainer.getInstance().getComponentInstanceOfType(RepositoryService.class);
    Node node = null;
    if (identifier.startsWith("/")) {
      node = (Node) AbstractOperationHandler.getSession(repositoryService, workspace).getItem(identifier);
    } else {
      node = AbstractOperationHandler.getSession(repositoryService, workspace).getNodeByUUID(identifier);
    }
    return Utils.getRealNode(node);
  }

  /**
   * Gets the preferences.
   *
   * @param application the application
   * @return the preferences
   * @throws Exception the exception
   */
  private static Portlet getPreferences(Application<Portlet> application) throws Exception {
    DataStorage dataStorage = (DataStorage) PortalContainer.getInstance().getComponentInstanceOfType(DataStorage.class);
    return dataStorage.load(application.getState(), application.getType());
  }
}