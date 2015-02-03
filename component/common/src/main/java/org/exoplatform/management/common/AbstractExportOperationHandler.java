package org.exoplatform.management.common;

import java.util.List;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.common.RealtimeListAccess;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.manager.ActivityManager;

public abstract class AbstractExportOperationHandler extends AbstractOperationHandler {

  protected static final Log log = ExoLogger.getLogger(AbstractExportOperationHandler.class);

  protected ActivityManager activityManager;

  protected final void addActivityWithComments(List<ExoSocialActivity> activitiesList, String activityId, Object... params) {
    if (activityId == null || activityId.isEmpty()) {
      return;
    }
    ExoSocialActivity parentActivity = activityManager.getActivity(activityId);
    addActivityWithComments(activitiesList, parentActivity, params);
  }

  protected void addActivityWithComments(List<ExoSocialActivity> activitiesList, ExoSocialActivity parentActivity, Object... params) {
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
          this.refactorActivityComment(parentActivity, comment, params);
          comment.isComment(true);
          // FIXME setParentId not compatible with 4.0.7
          // comment.setParentId(parentActivity.getId());
        }
        activitiesList.addAll(comments);
      }
    }
  }

  protected void refactorActivityComment(ExoSocialActivity parentActivity, ExoSocialActivity comment, Object... params) {}
}
