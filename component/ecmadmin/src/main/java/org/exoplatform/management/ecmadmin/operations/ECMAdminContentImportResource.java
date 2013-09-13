package org.exoplatform.management.ecmadmin.operations;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;

import org.apache.commons.io.IOUtils;
import org.exoplatform.management.ecmadmin.operations.drive.DriveImportResource;
import org.exoplatform.management.ecmadmin.operations.nodetype.NodeTypeImportResource;
import org.exoplatform.management.ecmadmin.operations.queries.QueriesImportResource;
import org.exoplatform.management.ecmadmin.operations.script.ScriptImportResource;
import org.exoplatform.management.ecmadmin.operations.taxonomy.TaxonomyImportResource;
import org.exoplatform.management.ecmadmin.operations.templates.applications.ApplicationsTemplatesImportResource;
import org.exoplatform.management.ecmadmin.operations.templates.metadata.MetadataTemplatesImportResource;
import org.exoplatform.management.ecmadmin.operations.templates.nodetypes.NodeTypesTemplatesImportResource;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.exceptions.ResourceNotFoundException;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.OperationHandler;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.NoResultModel;

/**
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class ECMAdminContentImportResource implements OperationHandler {

  @Override
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws ResourceNotFoundException,
      OperationException {

    InputStream inputStream = operationContext.getAttachment(false).getStream();
    FileOutputStream outputStream = null;
    File tmpFile = null;
    try {
      tmpFile = File.createTempFile("ecmadmin-content-", ".zip");
      tmpFile.deleteOnExit();
      outputStream = new FileOutputStream(tmpFile);
      IOUtils.copy(inputStream, outputStream);
      outputStream.close();
      inputStream.close();
      inputStream = null;
      outputStream = null;

      // import drives
      importData(DriveImportResource.class, operationContext, resultHandler, tmpFile.getAbsolutePath());
      // import nodetypes
      importData(NodeTypeImportResource.class, operationContext, resultHandler, "nodetype", tmpFile.getAbsolutePath());
      // import actions
      importData(NodeTypeImportResource.class, operationContext, resultHandler, "action", tmpFile.getAbsolutePath());
      // import queries
      importData(QueriesImportResource.class, operationContext, resultHandler, tmpFile.getAbsolutePath());
      // import scripts
      importData(ScriptImportResource.class, operationContext, resultHandler, tmpFile.getAbsolutePath());
      // import taxonomies
      importData(TaxonomyImportResource.class, operationContext, resultHandler, tmpFile.getAbsolutePath());
      // import application templates
      importData(ApplicationsTemplatesImportResource.class, operationContext, resultHandler, tmpFile.getAbsolutePath());
      // import metadata templates
      importData(MetadataTemplatesImportResource.class, operationContext, resultHandler, tmpFile.getAbsolutePath());
      // import nodetype templates
      importData(NodeTypesTemplatesImportResource.class, operationContext, resultHandler, tmpFile.getAbsolutePath());

    } catch (Exception e) {
      throw new OperationException(OperationNames.IMPORT_RESOURCE, "Error while importing Data", e);
    } finally {
      if (inputStream != null) {
        try {
          inputStream.close();
        } catch (IOException exception) {
          throw new OperationException(OperationNames.IMPORT_RESOURCE, "Error while closing input stream", exception);
        }
      }
      if (outputStream != null) {
        try {
          outputStream.close();
        } catch (IOException exception) {
          throw new OperationException(OperationNames.IMPORT_RESOURCE, "Error while closing input stream", exception);
        }
      }
      if(tmpFile != null) {
        tmpFile.delete();
      }
    }
    resultHandler.completed(NoResultModel.INSTANCE);
  }

  private void importData(Class<? extends OperationHandler> operationHandlerClass, OperationContext operationContext,
      ResultHandler resultHandler, Object... constructorArguments) throws Exception {
    Class<?>[] argumentClasses = new Class<?>[constructorArguments.length];
    for (int i = 0; i < constructorArguments.length; i++) {
      Object object = constructorArguments[i];
      argumentClasses[i] = object.getClass();
    }
    Constructor<? extends OperationHandler> constructor = operationHandlerClass.getConstructor(argumentClasses);
    OperationHandler operationHandler = constructor.newInstance(constructorArguments);
    operationHandler.execute(operationContext, resultHandler);
  }
}
