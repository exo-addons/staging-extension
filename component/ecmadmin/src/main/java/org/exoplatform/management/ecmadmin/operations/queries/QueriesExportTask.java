package org.exoplatform.management.ecmadmin.operations.queries;

import java.io.IOException;
import java.io.OutputStream;

import org.exoplatform.container.xml.Configuration;
import org.gatein.management.api.operation.model.ExportTask;

/**
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class QueriesExportTask implements ExportTask {
  public static final String CONFIGURATION_FILE_XSD = "<configuration " + "\r\n   xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
      + "\r\n   xsi:schemaLocation=\"http://www.exoplatform.org/xml/ns/kernel_1_2.xsd http://www.exoplatform.org/xml/ns/kernel_1_2.xsd\""
      + "\r\n   xmlns=\"http://www.exoplatform.org/xml/ns/kernel_1_2.xsd\">";
  private final Configuration configuration;
  private final String userId;

  public QueriesExportTask(Configuration configuration, String userId) {
    this.configuration = configuration;
    this.userId = userId;
  }

  @Override
  public String getEntry() {
    if (userId != null) {
      return "queries/users/" + this.userId + "-queries-configuration.xml";
    } else {
      return "queries/shared-queries-configuration.xml";
    }
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