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

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.exoplatform.forum.service.Category;
import org.exoplatform.forum.service.Forum;
import org.exoplatform.forum.service.ForumService;
import org.exoplatform.forum.service.Utils;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.exceptions.ResourceNotFoundException;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.OperationHandler;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.ReadResourceModel;
import org.mortbay.log.Log;

/**
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class ForumDataReadResource implements OperationHandler {

  private boolean isSpaceForumType;

  public ForumDataReadResource(boolean isSpaceForumType) {
    this.isSpaceForumType = isSpaceForumType;
  }

  @Override
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws ResourceNotFoundException, OperationException {
    Set<String> children = new LinkedHashSet<String>();
    ForumService forumService = operationContext.getRuntimeContext().getRuntimeComponent(ForumService.class);
    SpaceService spaceService = operationContext.getRuntimeContext().getRuntimeComponent(SpaceService.class);
    Category spaceCategory = forumService.getCategoryIncludedSpace();
    if (isSpaceForumType) {
      if (spaceCategory != null) {
        List<Forum> forums = null;
        try {
          forums = forumService.getForums(spaceCategory.getId(), "");
        } catch (Exception e) {
          throw new OperationException(OperationNames.READ_RESOURCE, "Error while getting space forums", e);
        }
        for (Forum forum : forums) {
          String spaceDisplayName = getSpaceDisplayName(spaceService, forum);
          if (spaceDisplayName == null) {
            Log.warn("Cannot find space for forum: " + forum.getForumName());
          } else {
            children.add(spaceDisplayName);
          }
        }
      }
    } else {
      List<Category> categories = forumService.getCategories();
      for (Category category : categories) {
        if (spaceCategory != null && category.getId().equals(spaceCategory.getId())) {
          continue;
        }
        children.add(category.getCategoryName());
      }
    }
    resultHandler.completed(new ReadResourceModel("All forums:", children));
  }

  private String getSpaceDisplayName(SpaceService spaceService, Forum forum) {
    if (forum.getId().startsWith(Utils.FORUM_SPACE_ID_PREFIX)) {
      String spaceGroupId = "/spaces/" + forum.getId().replace(Utils.FORUM_SPACE_ID_PREFIX, "");
      Space space = spaceService.getSpaceByGroupId(spaceGroupId);
      if (space != null) {
        return space.getDisplayName();
      }
    }
    return null;
  }
}
