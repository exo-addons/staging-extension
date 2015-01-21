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
package org.exoplatform.management.content.operations.site.contents;

import java.util.List;

import org.exoplatform.management.common.AbstractActivitiesExportTask;
import org.exoplatform.management.content.operations.site.SiteUtil;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.manager.IdentityManager;

/**
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class SiteContentsActivitiesExportTask extends AbstractActivitiesExportTask {
  public static final String FILENAME = "/DocumentActivities.metadata";

  private final String siteName;

  public SiteContentsActivitiesExportTask(IdentityManager identityManager, List<ExoSocialActivity> activitiesList, String siteName) {
    super(identityManager, activitiesList);
    this.siteName = siteName;
  }

  @Override
  public String getEntry() {
    return getEntryPath(siteName);
  }

  public static String getEntryPath(String siteName) {
    return SiteUtil.getSiteContentsBasePath(siteName) + FILENAME;
  }
}
