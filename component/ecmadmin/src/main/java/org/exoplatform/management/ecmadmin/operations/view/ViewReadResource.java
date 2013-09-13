package org.exoplatform.management.ecmadmin.operations.view;

import java.util.HashSet;
import java.util.Set;

import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.OperationHandler;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.ReadResourceModel;

/**
 * @author <a href="mailto:boubaker.khanfir@exoplatform.com">Boubaker
 *         Khanfir</a>
 * @version $Revision$
 */
public class ViewReadResource implements OperationHandler {
  @Override
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws OperationException {
    Set<String> viewConfigurations = new HashSet<String>();
    viewConfigurations.add("templates");
    viewConfigurations.add("configuration");

    resultHandler.completed(new ReadResourceModel("Sites Explorer Views.", viewConfigurations));
  }
}