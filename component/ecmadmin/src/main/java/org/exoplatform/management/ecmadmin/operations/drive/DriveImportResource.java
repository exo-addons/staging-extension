package org.exoplatform.management.ecmadmin.operations.drive;

import org.apache.commons.io.IOUtils;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.xml.*;
import org.exoplatform.management.ecmadmin.operations.ECMAdminImportResource;
import org.exoplatform.management.ecmadmin.operations.queries.QueriesExportTask;
import org.exoplatform.services.cms.drives.DriveData;
import org.exoplatform.services.cms.drives.ManageDriveService;
import org.exoplatform.services.cms.drives.impl.ManageDrivePlugin;
import org.exoplatform.services.cms.drives.impl.ManageDriveServiceImpl;
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

import java.io.StringReader;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

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

    try {
      ZipInputStream zin = new ZipInputStream(attachmentInputStream);
      ZipEntry ze = null;
      while ((ze = zin.getNextEntry()) != null) {
        if (!ze.getName().startsWith("drive/")) {
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
            Class<?> pluginClass = Class.forName(componentPlugin.getType());
            ManageDrivePlugin cplugin = (ManageDrivePlugin) PortalContainer.getInstance().createComponent(pluginClass, componentPlugin.getInitParams());
            cplugin.setName(componentPlugin.getName());
            cplugin.setDescription(componentPlugin.getDescription());

            // TODO add setManageDrivePlugin in Interface
            // ManageDriveService
            ((ManageDriveServiceImpl) driveService).setManageDrivePlugin(cplugin);

            // Delete existing drives if replaceExisting=true
            if (replaceExisting) {
              deleteExistingDrives(componentPlugin);
            } else {
              log.info("Ignore existing drives");
            }
          }
        }
        zin.closeEntry();
      }
      zin.close();
      driveService.init();
    } catch (Exception exception) {
      throw new OperationException(OperationNames.IMPORT_RESOURCE, "Error while importing ECMS drives.", exception);
    }
    resultHandler.completed(NoResultModel.INSTANCE);
  }

  private void deleteExistingDrives(ComponentPlugin componentPlugin) throws Exception {
    InitParams params_ = componentPlugin.getInitParams();
    @SuppressWarnings("unchecked")
    Iterator<ObjectParameter> it = params_.getObjectParamIterator();
    while (it.hasNext()) {
      DriveData data = (DriveData) it.next().getObject();
      DriveData storedDriveData = driveService.getDriveByName(data.getName());
      if (storedDriveData != null) {
        log.info("Overwrite existing drive '" + data.getName() + "'.");
        driveService.removeDrive(data.getName());
      }
    }
  }

}
