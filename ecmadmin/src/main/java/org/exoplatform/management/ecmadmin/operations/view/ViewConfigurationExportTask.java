package org.exoplatform.management.ecmadmin.operations.view;

import java.io.IOException;
import java.io.OutputStream;

import org.exoplatform.container.xml.InitParams;
import org.gatein.management.api.operation.model.ExportTask;

/**
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class ViewConfigurationExportTask implements ExportTask {
  private final InitParams initParams;
  private final String name;

  public ViewConfigurationExportTask(InitParams initParams, String name) {
    this.initParams = initParams;
    this.name = name;
  }

  @Override
  public String getEntry() {
    return "view/" + name;
  }

  @Override
  public void export(OutputStream outputStream) throws IOException {
    try {
      String content = new String(Utils.toXML(initParams));
      content = content.replaceAll("<field name=\"([A-z])*\"/>", "");
      outputStream.write(content.getBytes());
    } catch (Exception exception) {
      throw new RuntimeException(exception);
    }
  }
}