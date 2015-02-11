package org.exoplatform.management.common;

import java.io.File;

public class FileEntry implements Comparable<FileEntry> {
  String nodePath;
  File file;
  File historyFile;

  public FileEntry(String nodePath, File file) {
    super();
    this.nodePath = nodePath;
    this.file = file;
  }

  public String getNodePath() {
    return nodePath;
  }

  public void setNodePath(String nodePath) {
    this.nodePath = nodePath;
  }

  public File getFile() {
    return file;
  }

  public void setFile(File file) {
    this.file = file;
  }

  public File getHistoryFile() {
    return historyFile;
  }

  public void setHistoryFile(File historyFile) {
    this.historyFile = historyFile;
  }

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