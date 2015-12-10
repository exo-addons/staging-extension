package org.exoplatform.management.content.operations.site;

import java.util.LinkedHashSet;
import java.util.Set;

import org.exoplatform.management.common.AbstractOperationHandler;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.exceptions.ResourceNotFoundException;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.ReadResourceModel;

public class SiteReadResource extends AbstractOperationHandler {

  @Override
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws ResourceNotFoundException, OperationException {
    Set<String> children = new LinkedHashSet<String>(2);
    children.add("contents");
    children.add("seo");
    resultHandler.completed(new ReadResourceModel("Site", children));
  }

}
