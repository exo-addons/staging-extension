package org.exoplatform.management.backup.operations;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.RootContainer;
import org.exoplatform.management.common.AbstractOperationHandler;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.exceptions.ResourceNotFoundException;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.ReadResourceModel;

/**
 * @author <a href="mailto:boubaker.khanfir@exoplatform.com">Boubaker
 *         Khanfir</a>
 * @version $Revision$
 */
public class BackupReadResource extends AbstractOperationHandler {

  @Override
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws ResourceNotFoundException, OperationException {
    Set<String> children = new LinkedHashSet<String>();

    String portalContainerName = operationContext.getAddress().resolvePathTemplate("portal");
    if (StringUtils.isEmpty(portalContainerName)) {
      @SuppressWarnings("unchecked")
      List<PortalContainer> portalContainers = RootContainer.getInstance().getComponentInstancesOfType(PortalContainer.class);
      for (PortalContainer portalContainer : portalContainers) {
        children.add(portalContainer.getName());
      }
    }

    resultHandler.completed(new ReadResourceModel("portals", children));
  }
}
