package org.exoplatform.management.registry.tasks;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.exoplatform.application.registry.Application;
import org.exoplatform.application.registry.ApplicationRegistryService;
import org.exoplatform.container.xml.ObjectParameter;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.model.ExportTask;
import org.jibx.runtime.BindingDirectory;
import org.jibx.runtime.IBindingFactory;
import org.jibx.runtime.IMarshallingContext;

/**
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class ApplicationExportTask implements ExportTask {
  public static final String APPLICATION_FILE_SUFFIX = "_ApplicationRegistry.xml";
  private ApplicationRegistryService applicationRegistryService;
  private String categoryName;
  private String applicationName;

  public ApplicationExportTask(String applicationName, String categoryName, ApplicationRegistryService applicationRegistryService) {
    this.categoryName = categoryName;
    this.applicationName = applicationName;
    this.applicationRegistryService = applicationRegistryService;
  }

  @Override
  public String getEntry() {
    return categoryName + "_" + applicationName + APPLICATION_FILE_SUFFIX;
  }

  @Override
  public void export(OutputStream outputStream) throws IOException {
    Application application = applicationRegistryService.getApplication(categoryName, applicationName);
    if (application == null) {
      throw new OperationException(OperationNames.EXPORT_RESOURCE, "Error while exporting application data: " + getEntry());
    }
    try {
      byte[] bytes = toXML(application);
      outputStream.write(bytes);
    } catch (Exception e) {
      throw new OperationException(OperationNames.EXPORT_RESOURCE, "Error while exporting application data: " + getEntry(), e);
    }
  }

  protected byte[] toXML(Object obj) throws Exception {
    ObjectParameter objectParameter = new ObjectParameter();
    objectParameter.setName("object");
    objectParameter.setObject(obj);
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    try {
      IBindingFactory bfact = BindingDirectory.getFactory(objectParameter.getClass());
      IMarshallingContext mctx = bfact.createMarshallingContext();
      mctx.setIndent(2);
      mctx.marshalDocument(objectParameter, "UTF-8", null, out);
      return out.toByteArray();
    } catch (Exception ie) {
      throw new OperationException(OperationNames.EXPORT_RESOURCE, "Error while exporting application data: " + getEntry(), ie);
    }
  }
}
