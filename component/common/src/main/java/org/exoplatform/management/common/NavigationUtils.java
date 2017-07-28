package org.exoplatform.management.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import javax.jcr.RepositoryException;

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

public class NavigationUtils {

  private final static UserNodeFilterConfig FILTER_CONFIG;

  static {
    UserNodeFilterConfig.Builder builder = UserNodeFilterConfig.builder();
    builder.withAuthMode(UserNodeFilterConfig.AUTH_READ);
    builder.withVisibility(Visibility.DISPLAYED);
    FILTER_CONFIG = builder.build();
  }


  public static String getNavURIWithApplication(PageService pageService, DataStorage dataStorage, UserPortal userPortal, SiteKey siteKey, String applicationName) throws Exception {
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
        return node.getURI();
      }
    }
    return null;
  }

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
