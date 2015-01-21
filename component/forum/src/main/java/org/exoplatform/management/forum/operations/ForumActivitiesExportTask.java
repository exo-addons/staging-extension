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

import java.util.List;

import org.exoplatform.management.common.AbstractActivitiesExportTask;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.manager.IdentityManager;

/**
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class ForumActivitiesExportTask extends AbstractActivitiesExportTask {

  public static final String FILENAME = "/ForumActivities.metadata";

  private final String type;
  private final String categoryId;
  private final String forumId;

  public ForumActivitiesExportTask(IdentityManager identityManager, List<ExoSocialActivity> activitiesList, String type, String categoryId, String forumId) {
    super(identityManager, activitiesList);
    this.categoryId = categoryId;
    this.forumId = forumId;
    this.type = type;
  }

  @Override
  public String getEntry() {
    return new StringBuilder("forum/").append(type).append("/").append(forumId == null || forumId.isEmpty() ? categoryId : forumId).append(FILENAME).toString();
  }

}
