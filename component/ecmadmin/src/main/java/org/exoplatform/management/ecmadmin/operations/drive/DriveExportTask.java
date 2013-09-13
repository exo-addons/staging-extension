package org.exoplatform.management.ecmadmin.operations.drive;

import java.io.IOException;
import java.io.OutputStream;

import org.exoplatform.container.xml.Configuration;
import org.gatein.management.api.operation.model.ExportTask;

/**
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class DriveExportTask implements ExportTask {
  private static final String CONFIGURATION_FILE_XSD = "<configuration " + "\r\n   xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
      + "\r\n   xsi:schemaLocation=\"http://www.exoplatform.org/xml/ns/kernel_1_2.xsd http://www.exoplatform.org/xml/ns/kernel_1_2.xsd\""
      + "\r\n   xmlns=\"http://www.exoplatform.org/xml/ns/kernel_1_2.xsd\">";

  private String basepath = null;
  private Configuration configuration = null;

  public DriveExportTask(Configuration configuration, String basepath) {
    this.basepath = basepath;
    this.configuration = configuration;
  }

  @Override
  public String getEntry() {
    return basepath + "/drives-configuration.xml";
  }

  @Override
  public void export(OutputStream outputStream) throws IOException {
    try {
      String content = configuration.toXML();
      content = content.replace("<configuration>", CONFIGURATION_FILE_XSD);
      content = content.replaceAll("<field name=\"([A-z])*\"/>", "");
      outputStream.write(content.getBytes());
    } catch (Exception exception) {
      throw new RuntimeException(exception);
    }
  }

}