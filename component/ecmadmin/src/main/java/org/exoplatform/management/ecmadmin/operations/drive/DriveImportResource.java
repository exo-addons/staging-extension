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
package org.exoplatform.management.ecmadmin.operations.drive;

import org.apache.commons.io.IOUtils;
import org.exoplatform.container.xml.ComponentPlugin;
import org.exoplatform.container.xml.Configuration;
import org.exoplatform.container.xml.ExternalComponentPlugins;
import org.exoplatform.management.ecmadmin.operations.ECMAdminImportResource;
import org.exoplatform.management.ecmadmin.operations.queries.QueriesExportTask;
import org.exoplatform.services.cms.drives.DriveData;
import org.exoplatform.services.cms.drives.ManageDriveService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.NoResultModel;
import org.jibx.runtime.BindingDirectory;
import org.jibx.runtime.IBindingFactory;
import org.jibx.runtime.IUnmarshallingContext;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * The Class DriveImportResource.
 *
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class DriveImportResource extends ECMAdminImportResource {
  
  /** The Constant log. */
  private static final Log log = ExoLogger.getLogger(DriveImportResource.class);
  
  /** The drive service. */
  private ManageDriveService driveService;

  /**
   * Instantiates a new drive import resource.
   */
  public DriveImportResource() {
    super(null);
  }

  /**
   * Instantiates a new drive import resource.
   *
   * @param filePath the file path
   */
  public DriveImportResource(String filePath) {
    super(filePath);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws OperationException {
    // get attributes and attachement inputstream
    super.execute(operationContext, resultHandler);

    if (driveService == null) {
      driveService = operationContext.getRuntimeContext().getRuntimeComponent(ManageDriveService.class);
    }

    ZipInputStream zin = new ZipInputStream(attachmentInputStream);
    try {
      ZipEntry ze = null;
      while ((ze = zin.getNextEntry()) != null) {
        try {
          if (!ze.getName().startsWith("ecmadmin/drive/")) {
            continue;
          }
          if (ze.getName().endsWith("drives-configuration.xml")) {
            IBindingFactory bfact = BindingDirectory.getFactory(Configuration.class);
            IUnmarshallingContext uctx = bfact.createUnmarshallingContext();
            String content = IOUtils.toString(zin);
            content = content.replace(QueriesExportTask.CONFIGURATION_FILE_XSD, "<configuration>");
            Configuration configuration = (Configuration) uctx.unmarshalDocument(new StringReader(content), "UTF-8");

            ExternalComponentPlugins externalComponentPlugins = configuration.getExternalComponentPlugins(ManageDriveService.class.getName());
            List<ComponentPlugin> componentPlugins = externalComponentPlugins.getComponentPlugins();
            for (ComponentPlugin componentPlugin : componentPlugins) {
              List<DriveData> drives = componentPlugin.getInitParams().getObjectParamValues(DriveData.class);

              for (DriveData drive : drives) {
                if (replaceExisting || driveService.getDriveByName(drive.getName()) == null) {
                  log.info("Overwrite existing drive : " + drive.getName());
                // The addDrive method add the drive if it does not exist or updates it if it exists
                driveService.addDrive(drive.getName(), drive.getWorkspace(), drive.getPermissions(), drive.getHomePath(), drive.getViews(), drive.getIcon(), drive.getViewPreferences(), drive.getViewNonDocument(), drive.getViewSideBar(), drive.getShowHiddenNode(), drive.getAllowCreateFolders(), drive.getAllowNodeTypesOnTree());
              } else {
                  log.info("Ignore existing drive : " + drive.getName());
                }
              }
            }

            driveService.clearAllDrivesCache();
          }
        } finally {
          zin.closeEntry();
        }
      }
    } catch (Exception exception) {
      throw new OperationException(OperationNames.IMPORT_RESOURCE, "Error while importing ECMS drives.", exception);
    } finally {
      try {
        zin.close();
      } catch (IOException e) {
        throw new OperationException(OperationNames.IMPORT_RESOURCE, "Error while closing stream.", e);
      }
    }
    resultHandler.completed(NoResultModel.INSTANCE);
  }

}
