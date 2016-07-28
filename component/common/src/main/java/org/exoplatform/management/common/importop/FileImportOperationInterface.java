package org.exoplatform.management.common.importop;

import java.io.File;
import java.util.List;

import org.exoplatform.management.common.FileEntry;

public interface FileImportOperationInterface {
  String getManagedFilesPrefix();

  boolean isUnKnownFileFormat(String filePath);

  boolean addSpecialFile(List<FileEntry> fileEntries, String filePath, File file);

  String extractIdFromPath(String path);

  String getNodePath(String filePath);

  boolean isNodeOptional(String filePath);

}
