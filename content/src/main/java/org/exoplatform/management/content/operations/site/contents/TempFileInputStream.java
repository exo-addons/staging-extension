package org.exoplatform.management.content.operations.site.contents;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

class TempFileInputStream extends FileInputStream {

  private final File file;

  public TempFileInputStream(File file) throws FileNotFoundException {
    super(file);
    this.file = file;
    try {
      file.deleteOnExit();
    } catch (Exception e) {
      // ignore me
    }
  }

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