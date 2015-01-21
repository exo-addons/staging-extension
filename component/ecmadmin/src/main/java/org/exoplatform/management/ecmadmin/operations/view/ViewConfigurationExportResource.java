package org.exoplatform.management.ecmadmin.operations.view;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;

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

/**
 * @author <a href="mailto:boubaker.khanfir@exoplatform.com">Boubaker
 *         Khanfir</a>
 * @version $Revision$
 */
public class ViewConfigurationExportResource extends AbstractOperationHandler {

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
