package org.exoplatform.management.ecmadmin.operations.drive;

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
public class DriveExportTask implements ExportTask {

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