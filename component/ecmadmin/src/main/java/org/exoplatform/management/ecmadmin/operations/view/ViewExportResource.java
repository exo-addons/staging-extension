package org.exoplatform.management.ecmadmin.operations.view;

import java.util.ArrayList;
import java.util.List;

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ObjectParameter;
import org.exoplatform.services.cms.views.ManageViewService;
import org.exoplatform.services.cms.views.ViewConfig;
import org.gatein.management.api.PathAddress;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.OperationHandler;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.ExportResourceModel;
import org.gatein.management.api.operation.model.ExportTask;

/**
 * @author <a href="mailto:boubaker.khanfir@exoplatform.com">Boubaker
 *         Khanfir</a>
 * @version $Revision$
 */
public class ViewExportResource implements OperationHandler {

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
