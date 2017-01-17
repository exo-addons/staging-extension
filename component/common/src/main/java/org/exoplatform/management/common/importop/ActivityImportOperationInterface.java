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
package org.exoplatform.management.common.importop;

import org.exoplatform.social.core.activity.model.ExoSocialActivity;

/**
 * The Interface ActivityImportOperationInterface.
 */
public interface ActivityImportOperationInterface {
  
  /**
   * Attach activity to entity.
   *
   * @param activity the activity
   * @param comment the comment
   * @throws Exception the exception
   */
  void attachActivityToEntity(ExoSocialActivity activity, ExoSocialActivity comment) throws Exception;

  /**
   * Checks if is activity not valid.
   *
   * @param activity the activity
   * @param comment the comment
   * @return true, if is activity not valid
   * @throws Exception the exception
   */
  boolean isActivityNotValid(ExoSocialActivity activity, ExoSocialActivity comment) throws Exception;
}
