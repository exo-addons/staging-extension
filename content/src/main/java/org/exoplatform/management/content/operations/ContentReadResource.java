package org.exoplatform.management.content.operations;

import java.util.LinkedHashSet;
import java.util.Set;

import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.exceptions.ResourceNotFoundException;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.OperationHandler;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.ReadResourceModel;

public class ContentReadResource implements OperationHandler {

  @Override
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws ResourceNotFoundException,
      OperationException {
    Set<String> children = new LinkedHashSet<String>(1);
    children.add("sites");
    resultHandler.completed(new ReadResourceModel("Content", children));
  }

}
