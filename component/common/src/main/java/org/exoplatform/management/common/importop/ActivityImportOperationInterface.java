package org.exoplatform.management.common.importop;

import org.exoplatform.social.core.activity.model.ExoSocialActivity;

public interface ActivityImportOperationInterface {
  void attachActivityToEntity(ExoSocialActivity activity, ExoSocialActivity comment) throws Exception;

  boolean isActivityNotValid(ExoSocialActivity activity, ExoSocialActivity comment) throws Exception;
}
