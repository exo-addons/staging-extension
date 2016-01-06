package org.exoplatform.management.ecmadmin.operations.drive;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

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

/**
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class DriveImportResource extends ECMAdminImportResource {
  private static final Log log = ExoLogger.getLogger(DriveImportResource.class);
  private ManageDriveService driveService;

  public DriveImportResource() {
    super(null);
  }

  public DriveImportResource(String filePath) {
    super(filePath);
  }

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
              if(replaceExisting || driveService.getDriveByName(drive.getName()) == null) {
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
