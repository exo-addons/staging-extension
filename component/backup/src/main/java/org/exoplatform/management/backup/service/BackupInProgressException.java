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
package org.exoplatform.management.backup.service;

/**
 * The Class BackupInProgressException.
 */
public class BackupInProgressException extends RuntimeException {
  
  /** The Constant serialVersionUID. */
  public static final long serialVersionUID = 6455071395384858777L;

  /** The untreated exception. */
  public static ThreadLocal<Boolean> untreatedException = new ThreadLocal<Boolean>();

  /**
   * Instantiates a new backup in progress exception.
   */
  public BackupInProgressException() {
    this("Backup or a restore is in progress");
  }

  /**
   * Instantiates a new backup in progress exception.
   *
   * @param message the message
   */
  public BackupInProgressException(String message) {
    super(message);
    untreatedException.set(true);
  }

}
