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
package org.exoplatform.management.common;

import org.exoplatform.portal.config.DataStorage;
import org.exoplatform.portal.config.model.Application;
import org.exoplatform.portal.config.model.Container;
import org.exoplatform.portal.config.model.ModelObject;
import org.exoplatform.portal.config.model.Page;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.Visibility;
import org.exoplatform.portal.mop.navigation.Scope;
import org.exoplatform.portal.mop.page.PageContext;
import org.exoplatform.portal.mop.page.PageService;
import org.exoplatform.portal.mop.user.UserNavigation;
import org.exoplatform.portal.mop.user.UserNode;
import org.exoplatform.portal.mop.user.UserNodeFilterConfig;
import org.exoplatform.portal.mop.user.UserPortal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.jcr.RepositoryException;

/**
 * The Class NavigationUtils.
 */
public class NavigationUtils {

  /** The Constant FILTER_CONFIG. */
  private final static UserNodeFilterConfig FILTER_CONFIG;

  /** The Constant URIS_CACHE. */
  private final static Map<String, String> URIS_CACHE = new HashMap<String, String>();

  static {
    UserNodeFilterConfig.Builder builder = UserNodeFilterConfig.builder();
    builder.withAuthMode(UserNodeFilterConfig.AUTH_READ);
    builder.withVisibility(Visibility.DISPLAYED);
    FILTER_CONFIG = builder.build();
  }

  /**
   * Gets the nav URI with application.
   *
   * @param pageService the page service
   * @param dataStorage the data storage
   * @param userPortal the user portal
   * @param siteKey the site key
   * @param applicationName the application name
   * @return the corresponding Navigation URI (without "/portal/{siteName}" as
   *         prefix)
   * @throws Exception the exception
   */
  public static String getNavURIWithApplication(PageService pageService, DataStorage dataStorage, UserPortal userPortal, SiteKey siteKey, String applicationName) throws Exception {
    if (URIS_CACHE.containsKey(siteKey.toString() + applicationName)) {
      return URIS_CACHE.get(siteKey.toString() + applicationName);
    }
    Iterator<PageContext> pagesQueryResult = pageService.findPages(0, Integer.MAX_VALUE, siteKey.getType(), siteKey.getName(), null, null).iterator();
    // Map of (key: JCR Node path, Value: Page that has a portlet that
    // references the JCR Path)

    UserNavigation navigation = userPortal.getNavigation(siteKey);

    // Iterate on pages defined for this site to search on SCV portlets
    while (pagesQueryResult.hasNext()) {
      PageContext pageContext = (PageContext) pagesQueryResult.next();
      Page page = dataStorage.getPage(pageContext.getKey().format());
      ArrayList<ModelObject> children = page.getChildren();
      if (!pageContainsApplication(children, applicationName)) {
        continue;
      }
      // Search navigation node that references the page
      UserNode node = searchUserNodeByPageReference(userPortal, navigation, page.getPageId());
      if (node != null) {
        URIS_CACHE.put(siteKey.toString() + applicationName, node.getURI());
        return node.getURI();
      }
    }
    return null;
  }

  /**
   * Search user node by page reference.
   *
   * @param userPortal the user portal
   * @param nav the nav
   * @param pageReference the page reference
   * @return the user node
   */
  private static UserNode searchUserNodeByPageReference(UserPortal userPortal, UserNavigation nav, String pageReference) {
    if (nav != null) {
      try {
        UserNode rootNode = userPortal.getNode(nav, Scope.ALL, FILTER_CONFIG, null);
        if (rootNode.getPageRef() != null && pageReference.equals(rootNode.getPageRef())) {
          return rootNode;
        }

        if (rootNode.getChildren() != null && !rootNode.getChildren().isEmpty()) {
          return searchUserNodeByPageReference(rootNode.getChildren(), pageReference);
        }
      } catch (Exception exp) {
        // Ignore
      }
    }
    return null;
  }

  /**
   * Search user node by page reference.
   *
   * @param userNodes the user nodes
   * @param pageReference the page reference
   * @return the user node
   */
  private static UserNode searchUserNodeByPageReference(Collection<UserNode> userNodes, String pageReference) {
    if (userNodes == null || userNodes.isEmpty()) {
      return null;
    }
    for (UserNode userNode : userNodes) {
      if (userNode.getPageRef() != null && userNode.getPageRef().format().equals(pageReference)) {
        return userNode;
      } else if (userNode.getChildren() != null && !userNode.getChildren().isEmpty()) {
        UserNode childNode = searchUserNodeByPageReference(userNode.getChildren(), pageReference);
        if (childNode != null) {
          return childNode;
        }
      }
    }
    return null;
  }

  /**
   * Page contains application.
   *
   * @param children the children
   * @param applicationName the application name
   * @return true, if successful
   * @throws Exception the exception
   * @throws RepositoryException the repository exception
   */
  private static boolean pageContainsApplication(ArrayList<ModelObject> children, String applicationName) throws Exception, RepositoryException {
    if (children != null && !children.isEmpty()) {
      for (ModelObject modelObject : children) {
        if (modelObject instanceof Application) {
          String name = ((Application<?>) modelObject).getType().getName();
          if (name.equals(applicationName)) {
            return true;
          }
        } else if (modelObject instanceof Container) {
          boolean found = pageContainsApplication(((Container) modelObject).getChildren(), applicationName);
          if (found) {
            return true;
          }
        }
      }
    }
    return false;
  }
}
