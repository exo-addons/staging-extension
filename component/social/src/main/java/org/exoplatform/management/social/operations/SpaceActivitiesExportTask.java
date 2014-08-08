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
package org.exoplatform.management.social.operations;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.List;

import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.model.Profile;
import org.exoplatform.social.core.manager.IdentityManager;
import org.gatein.management.api.operation.model.ExportTask;

import com.thoughtworks.xstream.XStream;

/**
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class SpaceActivitiesExportTask implements ExportTask {

  public static final String FILENAME = "activities";

  private final IdentityManager identityManager;
  private final List<ExoSocialActivity> activities;
  private final String spacePrettyName;
  private final Integer index;

  public SpaceActivitiesExportTask(IdentityManager identityManager, List<ExoSocialActivity> activitiesList, String spacePrettyName, int index) {
    this.index = index;
    this.spacePrettyName = spacePrettyName;
    this.activities = activitiesList;
    this.identityManager = identityManager;
  }

  @Override
  public String getEntry() {
    return new StringBuilder("social/space/").append(spacePrettyName).append("/").append(FILENAME).append(index).append(".metadata").toString();
  }

  @Override
  public void export(OutputStream outputStream) throws IOException {
    XStream xStream = new XStream();
    OutputStreamWriter writer = new OutputStreamWriter(outputStream, "UTF-8");
    if (activities != null && activities.size() > 0) {
      for (ExoSocialActivity activity : activities) {
        activity.setId(null);
        Identity identity = identityManager.getIdentity(activity.getUserId(), true);
        String username = (String) identity.getProfile().getProperty(Profile.USERNAME);
        activity.setUserId(username);

        identity = identityManager.getIdentity(activity.getPosterId(), true);
        username = (String) identity.getProfile().getProperty(Profile.USERNAME);
        activity.setPosterId(username);
      }
    }
    xStream.toXML(activities, writer);
    writer.flush();
  }
}
