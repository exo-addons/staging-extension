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
package org.exoplatform.management.common;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
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
public class ActivitiesExportTask implements ExportTask {
  protected static final Log log = ExoLogger.getLogger(ActivitiesExportTask.class);

  public static final String FILENAME = "activities.metadata";

  protected static final String[] EMPTY_STRING_ARRAY = new String[0];

  protected final IdentityManager identityManager;
  protected final List<ExoSocialActivity> activities;
  protected final String entryName;

  public ActivitiesExportTask(IdentityManager identityManager, List<ExoSocialActivity> activities, String prefix) {
    this.identityManager = identityManager;
    this.activities = activities;
    this.entryName = (prefix.endsWith("/") ? prefix : (prefix + "/")) + FILENAME;
  }

  @Override
  public String getEntry() {
    return entryName;
  }

  @Override
  public final void export(OutputStream outputStream) throws IOException {
    try {
      XStream xStream = new XStream();
      OutputStreamWriter writer = new OutputStreamWriter(outputStream, "UTF-8");
      if (activities != null && activities.size() > 0) {
        for (ExoSocialActivity activity : activities) {
          Identity identity = identityManager.getIdentity(activity.getUserId(), true);
          if (identity != null) {
            String username = (String) identity.getProfile().getProperty(Profile.USERNAME);
            activity.setUserId(username);
          }

          identity = identityManager.getIdentity(activity.getPosterId(), true);
          if (identity != null) {
            String username = (String) identity.getProfile().getProperty(Profile.USERNAME);
            activity.setPosterId(username);
          }

          String[] commentedIds = activity.getCommentedIds();
          commentedIds = changeIdentityIdToUsername(commentedIds);
          activity.setCommentedIds(commentedIds);

          String[] mentionedIds = activity.getMentionedIds();
          mentionedIds = changeIdentityIdToUsername(mentionedIds);
          activity.setMentionedIds(mentionedIds);

          String[] likeIdentityIds = activity.getLikeIdentityIds();
          likeIdentityIds = changeIdentityIdToUsername(likeIdentityIds);
          activity.setLikeIdentityIds(likeIdentityIds);
        }
      }
      xStream.toXML(activities, writer);
      writer.flush();
    } catch (Exception e) {
      log.warn("Can't export activities", e);
    }
  }

  private String[] changeIdentityIdToUsername(String[] ids) {
    List<String> resultIds = new ArrayList<String>();
    if (ids != null && ids.length > 0) {
      for (int i = 0; i < ids.length; i++) {
        String[] id = ids[i].split("@");
        Identity identity = identityManager.getIdentity(id[0], true);
        if (identity != null) {
          id[0] = (String) identity.getProfile().getProperty(Profile.USERNAME);
          if (id.length == 2) {
            ids[i] = id[0] + "@" + id[1];
          } else {
            ids[i] = id[0];
          }
          resultIds.add(ids[i]);
        } else {
          log.warn("Cannot get identity : " + ids[i]);
        }
      }
      ids = resultIds.toArray(EMPTY_STRING_ARRAY);
    }
    return ids;
  }

}
