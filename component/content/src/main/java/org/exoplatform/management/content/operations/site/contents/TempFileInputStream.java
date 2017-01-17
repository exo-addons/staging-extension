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
package org.exoplatform.management.content.operations.site.contents;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * The Class TempFileInputStream.
 */
class TempFileInputStream extends FileInputStream {

  /** The file. */
  private final File file;

  /**
   * Instantiates a new temp file input stream.
   *
   * @param file the file
   * @throws FileNotFoundException the file not found exception
   */
  public TempFileInputStream(File file) throws FileNotFoundException {
    super(file);
    this.file = file;
    try {
      file.deleteOnExit();
    } catch (Exception e) {
      // ignore me
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void finalize() throws IOException {
    try {
      file.delete();
    } catch (Exception e) {
      // ignore me
    }
    super.finalize();
  }
}