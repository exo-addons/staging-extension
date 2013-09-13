package org.exoplatform.management.registry.tasks;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.exoplatform.application.registry.ApplicationCategory;
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
public class CategoryExportTask implements ExportTask {
  public static final String CATEGORY_FILE_SUFFIX = "_Category.xml";
  private ApplicationCategory category;

  public CategoryExportTask(String categoryName, ApplicationRegistryService applicationRegistryService) {
    this.category = applicationRegistryService.getApplicationCategory(categoryName);
    if (this.category == null) {
      throw new OperationException(OperationNames.EXPORT_RESOURCE, "Category not found: " + categoryName);
    }
  }

  public CategoryExportTask(ApplicationCategory category) {
    this.category = category;
  }

  @Override
  public String getEntry() {
    return category.getName() + "_" + CATEGORY_FILE_SUFFIX;
  }

  @Override
  public void export(OutputStream outputStream) throws IOException {
    try {
      byte[] bytes = toXML(category);
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
