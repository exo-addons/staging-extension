package org.exoplatform.management.content.operations.site;

import java.util.HashSet;
import java.util.Set;

import org.exoplatform.portal.config.UserPortalConfigService;
import org.exoplatform.services.wcm.core.WCMConfigurationService;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.OperationHandler;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.ReadResourceModel;

/**
 * @author <a href="mailto:thomas.delhomenie@exoplatform.com">Thomas
 *         Delhoménie</a>
 * @version $Revision$
 */
public class LiveSitesReadResource implements OperationHandler {
  @Override
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws OperationException {
    try {
      UserPortalConfigService portalConfigService = operationContext.getRuntimeContext().getRuntimeComponent(
          UserPortalConfigService.class);
      Set<String> sites = new HashSet<String>(portalConfigService.getAllPortalNames());

      WCMConfigurationService wcmConfigurationService = operationContext.getRuntimeContext().getRuntimeComponent(
          WCMConfigurationService.class);
      sites.add(wcmConfigurationService.getSharedPortalName());

      resultHandler.completed(new ReadResourceModel("Available sites.", sites));
    } catch (Exception e) {
      throw new OperationException(OperationNames.READ_RESOURCE, "Unable to retrieve the list of the sites : " + e.getMessage());
    }
  }
}
