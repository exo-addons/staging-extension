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
package org.exoplatform.management.forum.operations;

import org.exoplatform.forum.service.Category;
import org.exoplatform.forum.service.Forum;
import org.exoplatform.forum.service.ForumService;
import org.exoplatform.forum.service.Utils;
import org.exoplatform.management.common.AbstractOperationHandler;
import org.exoplatform.social.core.space.SpaceUtils;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.gatein.common.logging.Logger;
import org.gatein.common.logging.LoggerFactory;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.exceptions.ResourceNotFoundException;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.ReadResourceModel;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The Class ForumDataReadResource.
 *
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class ForumDataReadResource extends AbstractOperationHandler {

  /** The Constant log. */
  final private static Logger log = LoggerFactory.getLogger(ForumDataReadResource.class);

  /** The is space forum type. */
  private boolean isSpaceForumType;

  /**
   * Instantiates a new forum data read resource.
   *
   * @param isSpaceForumType the is space forum type
   */
  public ForumDataReadResource(boolean isSpaceForumType) {
    this.isSpaceForumType = isSpaceForumType;
  }

  /**
   * {@inheritDoc}
   */
  @SuppressWarnings("deprecation")
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
          forums = forumService.getForums(spaceCategory.getId(), null);
        } catch (Exception e) {
          throw new OperationException(OperationNames.READ_RESOURCE, "Error while getting space forums", e);
        }
        for (Forum forum : forums) {
          String spaceDisplayName = getSpaceDisplayName(spaceService, forum);
          if (spaceDisplayName == null) {
            log.warn("Cannot find space for forum: " + forum.getForumName());
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

  /**
   * Gets the space display name.
   *
   * @param spaceService the space service
   * @param forum the forum
   * @return the space display name
   */
  private String getSpaceDisplayName(SpaceService spaceService, Forum forum) {
    if (forum.getId().startsWith(Utils.FORUM_SPACE_ID_PREFIX)) {
      String spaceGroupId = SpaceUtils.SPACE_GROUP + "/" + forum.getId().replace(Utils.FORUM_SPACE_ID_PREFIX, "");
      Space space = spaceService.getSpaceByGroupId(spaceGroupId);
      if (space != null) {
        return space.getDisplayName();
      }
    }
    return null;
  }
}
