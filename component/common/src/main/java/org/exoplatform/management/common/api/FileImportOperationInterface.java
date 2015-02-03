package org.exoplatform.management.common.api;

import java.io.File;
import java.util.List;

public interface FileImportOperationInterface {
  String getManagedFilesPrefix();

  boolean isUnKnownFileFormat(String filePath);

  boolean addSpecialFile(List<FileEntry> fileEntries, String filePath, File file);

  String extractIdFromPath(String path);

  String getNodePath(String filePath);
}
