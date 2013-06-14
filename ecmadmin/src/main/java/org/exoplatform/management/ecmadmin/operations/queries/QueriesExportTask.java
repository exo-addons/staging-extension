package org.exoplatform.management.ecmadmin.operations.queries;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.exoplatform.container.xml.Configuration;
import org.gatein.management.api.operation.model.ExportTask;
import org.jibx.runtime.BindingDirectory;
import org.jibx.runtime.IBindingFactory;
import org.jibx.runtime.IMarshallingContext;

/**
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class QueriesExportTask implements ExportTask {
  private final Configuration configuration;
  private final String userId;

  public QueriesExportTask(Configuration configuration, String userId) {
    this.configuration = configuration;
    this.userId = userId;
  }

  
  public String getEntry() {
    if (userId != null) {
      return "queries/users/" + this.userId + "-queries-configuration.xml";
    } else {
      return "queries/shared-queries-configuration.xml";
    }
  }

  
  public void export(OutputStream outputStream) throws IOException {
    try {
      ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream();

      IBindingFactory bfact = BindingDirectory.getFactory(Configuration.class);
      IMarshallingContext mctx = bfact.createMarshallingContext();
      mctx.setIndent(2);
      mctx.marshalDocument(configuration, "UTF-8", false, arrayOutputStream);

      // Use ByteArrayOutputStream because the outputStream have to be
      // open, but 'marshalDocument' closes automatically the stream
      outputStream.write(arrayOutputStream.toByteArray());
    } catch (Exception exception) {
      throw new RuntimeException(exception);
    }
  }
}