/*
 * Copyright (C) 2003-2017 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.exoplatform.management.ecmadmin.operations;

import org.apache.commons.io.IOUtils;
import org.exoplatform.management.common.AbstractOperationHandler;
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;

/**
 * The Class ECMAdminContentImportResource.
 *
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class ECMAdminContentImportResource extends AbstractOperationHandler {

  /**
   * {@inheritDoc}
   */
  @Override
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws ResourceNotFoundException, OperationException {

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
      if (tmpFile != null) {
        tmpFile.delete();
      }
    }
    resultHandler.completed(NoResultModel.INSTANCE);
  }

  /**
   * Import data.
   *
   * @param operationHandlerClass the operation handler class
   * @param operationContext the operation context
   * @param resultHandler the result handler
   * @param constructorArguments the constructor arguments
   * @throws Exception the exception
   */
  private void importData(Class<? extends OperationHandler> operationHandlerClass, OperationContext operationContext, ResultHandler resultHandler, Object... constructorArguments) throws Exception {
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
