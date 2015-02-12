package org.exoplatform.management.common.exportop;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

public abstract class AbstractExportOperationHandler extends AbstractOperationHandler {

  protected static final Log log = ExoLogger.getLogger(AbstractExportOperationHandler.class);

  protected ActivityManager activityManager;
  protected IdentityManager identityManager;

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
      listAccess = activityManager.getActivitiesByPoster(identity, type);
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
      if (activity.getType() != null && types.contains(activity.getType())) {
        if (!activity.isComment() && ((ActivityExportOperationInterface) this).isActivityValid(activity)) {
          addActivityWithComments(activitiesList, activity);
        }
      }
    }
    if (!activitiesList.isEmpty()) {
      exportTasks.add(new ActivitiesExportTask(identityManager, activitiesList, pathPrefix));
    }
  }

  protected final void addActivityWithComments(List<ExoSocialActivity> activitiesList, String activityId) {
    if (activityId == null || activityId.isEmpty()) {
      return;
    }
    ExoSocialActivity parentActivity = activityManager.getActivity(activityId);
    if (parentActivity != null) {
      addActivityWithComments(activitiesList, parentActivity);
    }
  }

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

  protected void refactorActivityComment(ExoSocialActivity parentActivity, ExoSocialActivity comment) {}
}
