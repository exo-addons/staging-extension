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

import org.exoplatform.container.xml.ComponentPlugin;
import org.exoplatform.container.xml.Configuration;
import org.exoplatform.container.xml.ExternalComponentPlugins;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ObjectParameter;
import org.exoplatform.management.common.AbstractOperationHandler;
import org.exoplatform.services.cms.drives.DriveData;
import org.exoplatform.services.cms.drives.ManageDriveService;
import org.exoplatform.services.cms.drives.impl.ManageDrivePlugin;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.operation.OperationAttributes;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.ExportResourceModel;

import java.util.ArrayList;
import java.util.List;

/**
 * The Class DriveExportResource.
 *
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class DriveExportResource extends AbstractOperationHandler {

  /** The Constant EXPORT_BASE_PATH. */
  private static final String EXPORT_BASE_PATH = "ecmadmin/drive";

  /** The drive service. */
  private ManageDriveService driveService;

  /**
   * {@inheritDoc}
   */
  @Override
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws OperationException {
    OperationAttributes attributes = operationContext.getAttributes();
    List<String> filters = attributes.getValues("filter");
    if (driveService == null) {
      driveService = operationContext.getRuntimeContext().getRuntimeComponent(ManageDriveService.class);
    }
    Configuration configuration = null;
    try {
      ExternalComponentPlugins externalComponentPlugins = new ExternalComponentPlugins();
      externalComponentPlugins.setTargetComponent(ManageDriveService.class.getName());
      ArrayList<ComponentPlugin> componentPluginsList = new ArrayList<ComponentPlugin>();
      externalComponentPlugins.setComponentPlugins(componentPluginsList);

      ComponentPlugin templatesComponentPlugin = new ComponentPlugin();
      templatesComponentPlugin.setName("manage.drive.plugin");
      templatesComponentPlugin.setSetMethod("setManageDrivePlugin");
      templatesComponentPlugin.setType(ManageDrivePlugin.class.getName());

      InitParams templatesPluginInitParams = new InitParams();
      templatesComponentPlugin.setInitParams(templatesPluginInitParams);
      componentPluginsList.add(templatesComponentPlugin);

      List<DriveData> driveDataList = driveService.getAllDrives(true);
      for (DriveData driveData : driveDataList) {
        if (filters.isEmpty() || filters.contains(driveData.getName())) {
          ObjectParameter objectParam = new ObjectParameter();
          objectParam.setName(driveData.getName());
          objectParam.setObject(driveData);
          templatesPluginInitParams.addParam(objectParam);
        }
      }

      configuration = new Configuration();
      configuration.addExternalComponentPlugins(externalComponentPlugins);

    } catch (Exception exception) {
      throw new OperationException(OperationNames.EXPORT_RESOURCE, "Error while retrieving drives: ", exception);
    }
    resultHandler.completed(new ExportResourceModel(new DriveExportTask(configuration, EXPORT_BASE_PATH)));
  }

}