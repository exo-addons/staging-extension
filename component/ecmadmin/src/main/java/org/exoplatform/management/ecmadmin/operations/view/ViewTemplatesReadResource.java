package org.exoplatform.management.ecmadmin.operations.view;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.jcr.Node;

import org.exoplatform.management.common.AbstractOperationHandler;
import org.exoplatform.services.cms.BasePath;
import org.exoplatform.services.cms.views.ManageViewService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
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
public class ViewTemplatesReadResource extends AbstractOperationHandler {
  final private static Logger log = LoggerFactory.getLogger(ViewTemplatesReadResource.class);

  @Override
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws OperationException {
    ManageViewService manageViewService = operationContext.getRuntimeContext().getRuntimeComponent(ManageViewService.class);

    try {
      List<Node> nodes = manageViewService.getAllTemplates(BasePath.ECM_EXPLORER_TEMPLATES, SessionProvider.createSystemProvider());
      Set<String> viewConfigurations = new HashSet<String>();
      for (Node node : nodes) {
        viewConfigurations.add(node.getName());
      }

      resultHandler.completed(new ReadResourceModel("ECMS Explorer View Templates.", viewConfigurations));
    } catch (Exception e) {
      log.error("Error occured while retrieving Sites Explorer View Templates", e);
    }
  }
}