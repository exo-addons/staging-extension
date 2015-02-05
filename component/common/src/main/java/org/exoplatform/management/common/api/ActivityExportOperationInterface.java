package org.exoplatform.management.common.api;

import org.exoplatform.social.core.activity.model.ExoSocialActivity;

public interface ActivityExportOperationInterface {
  public boolean isActivityValid(ExoSocialActivity activity) throws Exception;

}
