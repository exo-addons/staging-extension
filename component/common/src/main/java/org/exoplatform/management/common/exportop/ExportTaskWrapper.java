package org.exoplatform.management.common.exportop;

import java.io.IOException;
import java.io.OutputStream;

import org.gatein.management.api.operation.model.ExportTask;

public class ExportTaskWrapper implements ExportTask {

  private String basePath;
  private ExportTask exportTask;

  public ExportTaskWrapper(ExportTask exportTask, String basePath) {
    this.exportTask = exportTask;
    this.basePath = basePath;
  }

  @Override
  public String getEntry() {
    return basePath + exportTask.getEntry();
  }

  @Override
  public void export(OutputStream outputStream) throws IOException {
    exportTask.export(outputStream);
  }
}
