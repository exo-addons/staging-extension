package org.exoplatform.management.ecmadmin.operations.view;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.exoplatform.management.common.AbstractOperationHandler;
import org.exoplatform.services.cms.views.ManageViewService;
import org.exoplatform.services.cms.views.ViewConfig;
import org.gatein.common.logging.Logger;
import org.gatein.common.logging.LoggerFactory;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.ReadResourceModel;

/**
 * @author <a href="mailto:boubaker.khanfir@exoplatform.com">Boubaker
 *         Khanfir</a>
 * @version $Revision$
 */
public class ViewConfigurationReadResource extends AbstractOperationHandler {
  final private static Logger log = LoggerFactory.getLogger(ViewConfigurationReadResource.class);

  @Override
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws OperationException {
    ManageViewService manageViewService = operationContext.getRuntimeContext().getRuntimeComponent(ManageViewService.class);
    try {
      List<ViewConfig> viewConfigs = manageViewService.getAllViews();
      Set<String> viewConfigurations = new HashSet<String>();
      for (ViewConfig viewConfig : viewConfigs) {
        viewConfigurations.add(viewConfig.getName());
      }
      resultHandler.completed(new ReadResourceModel("Sites Explorer : Configured Views.", viewConfigurations));
    } catch (Exception e) {
      log.error("Error occured while retrieving Sites Explorer Configured Views", e);
    }
  }
}