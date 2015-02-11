package org.exoplatform.management.common.exportop;

import org.exoplatform.social.core.activity.model.ExoSocialActivity;

public interface ActivityExportOperationInterface {
  public boolean isActivityValid(ExoSocialActivity activity) throws Exception;

}
