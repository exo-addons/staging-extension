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
package org.exoplatform.management.common;

import java.io.ByteArrayInputStream;

/**
 * The Class InputStreamWrapper.
 */
public class InputStreamWrapper extends ByteArrayInputStream {
  
  /**
   * Instantiates a new input stream wrapper.
   */
  public InputStreamWrapper() {
    super(new byte[0]);
  }

  /**
   * Instantiates a new input stream wrapper.
   *
   * @param buf the buf
   */
  public InputStreamWrapper(byte[] buf) {
    super(buf);
  }
}