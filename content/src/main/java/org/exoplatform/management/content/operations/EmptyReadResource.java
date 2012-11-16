package org.exoplatform.management.content.operations;

import java.util.HashSet;

import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.exceptions.ResourceNotFoundException;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.OperationHandler;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.ReadResourceModel;

public class EmptyReadResource implements OperationHandler {

  @Override
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws ResourceNotFoundException,
      OperationException {
    resultHandler.completed(new ReadResourceModel("Empty", new HashSet<String>()));
  }

}
