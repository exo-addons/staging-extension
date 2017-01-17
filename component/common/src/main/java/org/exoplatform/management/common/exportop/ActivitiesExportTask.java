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

import com.thoughtworks.xstream.XStream;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.model.Profile;
import org.exoplatform.social.core.manager.IdentityManager;
import org.gatein.management.api.operation.model.ExportTask;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * The Class ActivitiesExportTask.
 *
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class ActivitiesExportTask implements ExportTask {
  
  /** The Constant log. */
  protected static final Log log = ExoLogger.getLogger(ActivitiesExportTask.class);

  /** The Constant FILENAME. */
  public static final String FILENAME = "activities.metadata";

  /** The Constant EMPTY_STRING_ARRAY. */
  protected static final String[] EMPTY_STRING_ARRAY = new String[0];

  /** The identity manager. */
  protected final IdentityManager identityManager;
  
  /** The activities. */
  protected final List<ExoSocialActivity> activities;
  
  /** The entry name. */
  protected final String entryName;

  /**
   * Instantiates a new activities export task.
   *
   * @param identityManager the identity manager
   * @param activities the activities
   * @param prefix the prefix
   */
  public ActivitiesExportTask(IdentityManager identityManager, List<ExoSocialActivity> activities, String prefix) {
    this.identityManager = identityManager;
    this.activities = activities;
    this.entryName = (prefix.endsWith("/") ? prefix : (prefix + "/")) + FILENAME;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getEntry() {
    return entryName;
  }

  /**
   * {@inheritDoc}
   */
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

  /**
   * Change identity id to username.
   *
   * @param ids the ids
   * @return the string[]
   */
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
