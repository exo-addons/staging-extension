package org.exoplatform.management.gadget.operations;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.exoplatform.application.gadget.Gadget;
import org.exoplatform.application.gadget.GadgetRegistryService;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.OperationHandler;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.ReadResourceModel;

/**
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class GadgetReadResource implements OperationHandler {
  private GadgetRegistryService gadgetRegistryService;

  @Override
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws OperationException {
    if (gadgetRegistryService == null) {
      gadgetRegistryService = operationContext.getRuntimeContext().getRuntimeComponent(GadgetRegistryService.class);
      if (gadgetRegistryService == null) {
        throw new OperationException(OperationNames.EXPORT_RESOURCE, "DataInjectorService doesn't exist.");
      }
    }
    Set<String> result = new HashSet<String>();;
    try {
      List<Gadget> gadgets = gadgetRegistryService.getAllGadgets();
      for (Gadget gadget : gadgets) {
        result.add(gadget.getName());
      }
    } catch (Exception e) {
      throw new OperationException(OperationNames.READ_RESOURCE, "Error while retrieving gadget list.", e);
    }

    resultHandler.completed(new ReadResourceModel("Available users.", result));
  }
}
