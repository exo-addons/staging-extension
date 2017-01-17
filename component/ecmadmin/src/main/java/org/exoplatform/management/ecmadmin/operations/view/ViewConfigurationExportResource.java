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
import org.exoplatform.services.cms.views.ViewConfig.Tab;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.operation.OperationAttributes;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.ExportResourceModel;
import org.gatein.management.api.operation.model.ExportTask;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;

/**
 * The Class ViewConfigurationExportResource.
 *
 * @author <a href="mailto:boubaker.khanfir@exoplatform.com">Boubaker
 *         Khanfir</a>
 * @version $Revision$
 */
public class ViewConfigurationExportResource extends AbstractOperationHandler {

  /**
   * {@inheritDoc}
   */
  @Override
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws OperationException {
    try {
      OperationAttributes attributes = operationContext.getAttributes();
      List<String> filters = attributes.getValues("filter");

      ManageViewService manageViewService = operationContext.getRuntimeContext().getRuntimeComponent(ManageViewService.class);

      List<ExportTask> exportTasks = new ArrayList<ExportTask>();

      List<ViewConfig> viewConfigs = null;
      List<ViewConfig> configs = manageViewService.getAllViews();
      if (filters != null && !filters.isEmpty()) {
        viewConfigs = new ArrayList<ViewConfig>();
        for (ViewConfig config : configs) {
          if (filters.contains(config.getName())) {
            viewConfigs.add(config);
            Node node = manageViewService.getViewByName(config.getName(), SessionProvider.createSystemProvider());
            for (Tab tab : config.getTabList()) {
              Node tabNode = node.getNode(tab.getTabName());
              if (tabNode.hasProperty("exo:buttons")) {
                tab.setButtons(tabNode.getProperty("exo:buttons").getValue().getString());
              }
            }
          }
        }
      } else {
        viewConfigs = configs;
        for (ViewConfig config : configs) {
          Node node = manageViewService.getViewByName(config.getName(), SessionProvider.createSystemProvider());
          for (Tab tab : config.getTabList()) {
            Node tabNode = node.getNode(tab.getTabName());
            if (tabNode.hasProperty("exo:buttons")) {
              tab.setButtons(tabNode.getProperty("exo:buttons").getValue().getString());
            }
          }
        }
      }

      // shared queries
      if (viewConfigs != null && !viewConfigs.isEmpty()) {
        InitParams initParams = new InitParams();

        for (ViewConfig viewConfig : viewConfigs) {
          ObjectParameter objectParam = new ObjectParameter();
          objectParam.setName(viewConfig.getName());
          objectParam.setObject(viewConfig);
          initParams.addParam(objectParam);
        }
        exportTasks.add(new ViewConfigurationExportTask(initParams, "views-configurations.xml"));
      }

      resultHandler.completed(new ExportResourceModel(exportTasks));
    } catch (Exception e) {
      throw new OperationException(OperationNames.EXPORT_RESOURCE, "Unable to export View Configurations : ", e);
    }
  }
}
