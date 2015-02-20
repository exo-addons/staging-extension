/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.management.uiextension;

import java.util.List;
import java.util.Map;

import javax.jcr.Node;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.services.wcm.extensions.publication.PublicationManager;
import org.exoplatform.services.wcm.extensions.publication.lifecycle.impl.LifecyclesConfig.Lifecycle;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.webui.ext.filter.UIExtensionFilter;
import org.exoplatform.webui.ext.filter.UIExtensionFilterType;

public class CanPushContentFilter implements UIExtensionFilter {

  private static final String PERMISSIONS_VARIABLE = "exo.staging.explorer.content.permissions";
  private static final Log LOG = ExoLogger.getLogger(CanPushContentFilter.class.getName());

  /**
   * This method checks if the current node is of the right type
   */
  public boolean accept(Map<String, Object> context) throws Exception {
    // Retrieve the current node from the context
    Node currentNode = (Node) context.get(Node.class.getName());
    ConversationState state = ConversationState.getCurrent();
    if (state == null || state.getIdentity() == null) {
      return false;
    }
    String userId = state.getIdentity().getUserId();
    if (StringUtils.isEmpty(userId)) {
      return false;
    } else if (userId.equals(IdentityConstants.ANONIM)) {
      return false;
    }
    String pushContentPermmission = System.getProperty(PERMISSIONS_VARIABLE, null);
    if (pushContentPermmission != null && pushContentPermmission.trim().length() > 0) {
      return Utils.hasPushButtonPermission(PERMISSIONS_VARIABLE);
    } else if (currentNode.hasProperty("publication:currentState") && currentNode.hasProperty("publication:lifecycle")) {
      String currentState = currentNode.getProperty("publication:currentState").getString();
      if ("published".equals(currentState)) {
        String nodeLifecycle = currentNode.getProperty("publication:lifecycle").getString();
        PublicationManager publicationManager = WCMCoreUtils.getService(PublicationManager.class);
        List<Lifecycle> lifecycles = publicationManager.getLifecyclesFromUser(userId, "published");
        for (Lifecycle lifecycle : lifecycles) {
          if (nodeLifecycle.equals(lifecycle.getName())) {
            return true;
          }
        }
      }
    } else {
      PublicationManager publicationManager = WCMCoreUtils.getService(PublicationManager.class);
      List<Lifecycle> lifecycles = publicationManager.getLifecyclesFromUser(userId, "published");
      return lifecycles != null && !lifecycles.isEmpty();
    }
    return false;
  }

  /**
   * This is the type of the filter
   */
  public UIExtensionFilterType getType() {
    return UIExtensionFilterType.MANDATORY;
  }

  /**
   * This is called when the filter has failed
   */
  public void onDeny(Map<String, Object> context) throws Exception {
    if (LOG.isWarnEnabled()) {
      LOG.warn("You can add a category in a exo:taxonomy node only.");
    }
  }
}
