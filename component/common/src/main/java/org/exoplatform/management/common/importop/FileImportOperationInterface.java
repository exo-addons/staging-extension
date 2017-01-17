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

import org.exoplatform.management.common.FileEntry;

import java.io.File;
import java.util.List;

/**
 * The Interface FileImportOperationInterface.
 */
public interface FileImportOperationInterface {
  
  /**
   * Gets the managed files prefix.
   *
   * @return the managed files prefix
   */
  String getManagedFilesPrefix();

  /**
   * Checks if is un known file format.
   *
   * @param filePath the file path
   * @return true, if is un known file format
   */
  boolean isUnKnownFileFormat(String filePath);

  /**
   * Adds the special file.
   *
   * @param fileEntries the file entries
   * @param filePath the file path
   * @param file the file
   * @return true, if successful
   */
  boolean addSpecialFile(List<FileEntry> fileEntries, String filePath, File file);

  /**
   * Extract id from path.
   *
   * @param path the path
   * @return the string
   */
  String extractIdFromPath(String path);

  /**
   * Gets the node path.
   *
   * @param filePath the file path
   * @return the node path
   */
  String getNodePath(String filePath);
}
