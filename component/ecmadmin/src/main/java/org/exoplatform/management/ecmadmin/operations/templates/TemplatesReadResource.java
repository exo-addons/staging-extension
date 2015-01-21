package org.exoplatform.management.ecmadmin.operations.templates;

import java.util.LinkedHashSet;
import java.util.Set;

import org.exoplatform.management.common.AbstractOperationHandler;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.exceptions.ResourceNotFoundException;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.ReadResourceModel;

public class TemplatesReadResource extends AbstractOperationHandler {

  @Override
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws ResourceNotFoundException,
      OperationException {
    Set<String> children = new LinkedHashSet<String>(2);
    children.add("applications");
    children.add("nodetypes");
    children.add("metadata");
    resultHandler.completed(new ReadResourceModel("Templates", children));
  }

}
