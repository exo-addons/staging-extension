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
package org.exoplatform.management.backup.service.idm;

import org.exoplatform.services.organization.Membership;
import org.exoplatform.services.organization.MembershipEventListener;

/**
 * The listener interface for receiving backupMembership events.
 * The class that is interested in processing a backupMembership
 * event implements this interface, and the object created
 * with that class is registered with a component using the
 * component's <code>addBackupMembershipListener</code> method. When
 * the backupMembership event occurs, that object's appropriate
 * method is invoked.
 *
 */
public class BackupMembershipListener extends MembershipEventListener {

  /**
   * {@inheritDoc}
   */
  @Override
  public void preSave(Membership membership, boolean isNew) throws Exception {
    IDMBackup.interceptIDMModificationOperation();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void preDelete(Membership membership) throws Exception {
    IDMBackup.interceptIDMModificationOperation();
  }
}
