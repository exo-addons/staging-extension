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
package org.exoplatform.management.ecmadmin.operations.view;

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ObjectParameter;
import org.exoplatform.management.common.AbstractOperationHandler;
import org.exoplatform.services.cms.views.ManageViewService;
import org.exoplatform.services.cms.views.ViewConfig;
import org.gatein.management.api.PathAddress;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.ExportResourceModel;
import org.gatein.management.api.operation.model.ExportTask;

import java.util.ArrayList;
import java.util.List;

/**
 * The Class ViewExportResource.
 *
 * @author <a href="mailto:boubaker.khanfir@exoplatform.com">Boubaker
 *         Khanfir</a>
 * @version $Revision$
 */
public class ViewExportResource extends AbstractOperationHandler {

  /**
   * {@inheritDoc}
   */
  @Override
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws OperationException {
    try {
      ManageViewService manageViewService = operationContext.getRuntimeContext().getRuntimeComponent(ManageViewService.class);
      PathAddress address = operationContext.getAddress();

      List<ExportTask> exportTasks = new ArrayList<ExportTask>();

      String configurationName = address.resolvePathTemplate("configuration-name");
      List<ViewConfig> viewConfigs = null;
      List<ViewConfig> configs = manageViewService.getAllViews();
      if (configurationName != null && !configurationName.trim().isEmpty()) {
        viewConfigs = new ArrayList<ViewConfig>();
        for (ViewConfig config : configs) {
          if (config.getName().equals(configurationName)) {
            viewConfigs.add(config);
            break;
          }
        }
      } else {
        viewConfigs = configs;
      }

      if (viewConfigs != null && !viewConfigs.isEmpty()) {
        InitParams initParams = new InitParams();

        for (ViewConfig viewConfig : viewConfigs) {
          ObjectParameter objectParam = new ObjectParameter();
          objectParam.setName(viewConfig.getName());
          objectParam.setObject(viewConfig);
          initParams.addParam(objectParam);
        }
        exportTasks.add(new ViewConfigurationExportTask(initParams, "view-configuration.xml"));
      }

      resultHandler.completed(new ExportResourceModel(exportTasks));
    } catch (Exception e) {
      throw new OperationException(OperationNames.EXPORT_RESOURCE, "Unable to export View Configurations : ", e);
    }
  }
}
