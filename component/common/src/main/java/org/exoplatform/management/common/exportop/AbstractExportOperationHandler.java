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
package org.exoplatform.management.common.exportop;

import org.exoplatform.management.common.AbstractOperationHandler;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.common.RealtimeListAccess;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.identity.provider.SpaceIdentityProvider;
import org.exoplatform.social.core.manager.ActivityManager;
import org.exoplatform.social.core.manager.IdentityManager;
import org.gatein.management.api.operation.model.ExportTask;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The Class AbstractExportOperationHandler.
 */
public abstract class AbstractExportOperationHandler extends AbstractOperationHandler {

  /** The Constant log. */
  protected static final Log log = ExoLogger.getLogger(AbstractExportOperationHandler.class);

  /** The activity manager. */
  protected ActivityManager activityManager;
  
  /** The identity manager. */
  protected IdentityManager identityManager;

  /**
   * Export activities.
   *
   * @param exportTasks the export tasks
   * @param identityId the identity id
   * @param pathPrefix the path prefix
   * @param type the type
   * @throws Exception the exception
   */
  protected void exportActivities(List<ExportTask> exportTasks, String identityId, String pathPrefix, String... type) throws Exception {
    List<ExoSocialActivity> activitiesList = new ArrayList<ExoSocialActivity>();
    Identity identity = getIdentity(identityId);
    if (identity == null) {
      log.warn("Can't export activities of null identity for id = " + identityId);
      return;
    }
    RealtimeListAccess<ExoSocialActivity> listAccess = null;
    if (identity.getProviderId().equals(SpaceIdentityProvider.NAME)) {
      listAccess = activityManager.getActivitiesOfSpaceWithListAccess(identity);
    } else if (identity.getProviderId().equals(OrganizationIdentityProvider.NAME)) {
      if(type != null && type.length > 0) {
        listAccess = activityManager.getActivitiesByPoster(identity, type);
      } else {
        listAccess = activityManager.getActivitiesByPoster(identity);
      }
    }
    if (listAccess == null) {
      return;
    }
    listAccess.getNumberOfUpgrade();
    if (listAccess.getSize() == 0) {
      return;
    }
    List<String> types = Arrays.asList(type);
    ExoSocialActivity[] activities = listAccess.load(0, listAccess.getSize());
    for (ExoSocialActivity activity : activities) {
      if(type.length > 0) {
        if (activity.getType() != null && types.contains(activity.getType())) {
          if (!activity.isComment() && ((ActivityExportOperationInterface) this).isActivityValid(activity)) {
            addActivityWithComments(activitiesList, activity);
          }
        }
      } else {
        addActivityWithComments(activitiesList, activity);
      }
    }
    if (!activitiesList.isEmpty()) {
      exportTasks.add(new ActivitiesExportTask(identityManager, activitiesList, pathPrefix));
    }
  }

  /**
   * Adds the activity with comments.
   *
   * @param activitiesList the activities list
   * @param activityId the activity id
   */
  protected final void addActivityWithComments(List<ExoSocialActivity> activitiesList, String activityId) {
    if (activityId == null || activityId.isEmpty()) {
      return;
    }
    ExoSocialActivity parentActivity = activityManager.getActivity(activityId);
    if (parentActivity != null) {
      addActivityWithComments(activitiesList, parentActivity);
    }
  }

  /**
   * Adds the activity with comments.
   *
   * @param activitiesList the activities list
   * @param parentActivity the parent activity
   */
  protected void addActivityWithComments(List<ExoSocialActivity> activitiesList, ExoSocialActivity parentActivity) {
    // FIXME getParentId not compatible with 4.0.7
    // if (parentActivity != null && parentActivity.getParentId() == null &&
    // !parentActivity.isComment()) {
    if (parentActivity != null && !parentActivity.isComment()) {
      parentActivity.isComment(false);
      activitiesList.add(parentActivity);
      RealtimeListAccess<ExoSocialActivity> commentsListAccess = activityManager.getCommentsWithListAccess(parentActivity);
      if (commentsListAccess.getSize() > 0) {
        List<ExoSocialActivity> comments = commentsListAccess.loadAsList(0, commentsListAccess.getSize());
        for (ExoSocialActivity comment : comments) {
          this.refactorActivityComment(parentActivity, comment);
          comment.isComment(true);
          // FIXME setParentId not compatible with 4.0.7
          // comment.setParentId(parentActivity.getId());
        }
        activitiesList.addAll(comments);
      }
    }
  }

  /**
   * Refactor activity comment.
   *
   * @param parentActivity the parent activity
   * @param comment the comment
   */
  protected void refactorActivityComment(ExoSocialActivity parentActivity, ExoSocialActivity comment) {}
}
