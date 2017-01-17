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

import java.io.File;

/**
 * The Class FileEntry.
 */
public class FileEntry implements Comparable<FileEntry> {
  
  /** The node path. */
  String nodePath;
  
  /** The file. */
  File file;
  
  /** The history file. */
  File historyFile;

  /**
   * Instantiates a new file entry.
   *
   * @param nodePath the node path
   * @param file the file
   */
  public FileEntry(String nodePath, File file) {
    super();
    this.nodePath = nodePath;
    this.file = file;
  }

  /**
   * Gets the node path.
   *
   * @return the node path
   */
  public String getNodePath() {
    return nodePath;
  }

  /**
   * Sets the node path.
   *
   * @param nodePath the new node path
   */
  public void setNodePath(String nodePath) {
    this.nodePath = nodePath;
  }

  /**
   * Gets the file.
   *
   * @return the file
   */
  public File getFile() {
    return file;
  }

  /**
   * Sets the file.
   *
   * @param file the new file
   */
  public void setFile(File file) {
    this.file = file;
  }

  /**
   * Gets the history file.
   *
   * @return the history file
   */
  public File getHistoryFile() {
    return historyFile;
  }

  /**
   * Sets the history file.
   *
   * @param historyFile the new history file
   */
  public void setHistoryFile(File historyFile) {
    this.historyFile = historyFile;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (obj instanceof FileEntry) {
      if (nodePath != null) {
        return ((FileEntry) obj).getNodePath().equals(nodePath);
      }
    } else if (obj instanceof String) {
      if (nodePath != null) {
        return nodePath.equals(obj);
      }
    } else if (obj instanceof File) {
      if (file != null) {
        return file.equals(obj);
      }
    }
    return false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int compareTo(FileEntry o) {
    // aactions should be imported at last place
    if (this.getNodePath().contains("/exo:actions") && !o.getNodePath().contains("/exo:actions")) {
      return 1;
    }
    if (o.getNodePath().contains("/exo:actions") && !this.getNodePath().contains("/exo:actions")) {
      return -1;
    }
    return this.getNodePath().compareTo(o.getNodePath());
  }
}