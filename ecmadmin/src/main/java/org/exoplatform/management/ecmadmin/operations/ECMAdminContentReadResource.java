package org.exoplatform.management.ecmadmin.operations;

import java.util.LinkedHashSet;
import java.util.Set;

import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.exceptions.ResourceNotFoundException;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.OperationHandler;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.ReadResourceModel;

/**
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class ECMAdminContentReadResource implements OperationHandler {

  @Override
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws ResourceNotFoundException,
      OperationException {
    Set<String> children = new LinkedHashSet<String>(1);
    children.add("templates");
    children.add("taxonomy");
    children.add("queries");
    children.add("drive");
    children.add("script");
    children.add("action");
    children.add("nodetype");
    children.add("view");
    resultHandler.completed(new ReadResourceModel("ECM Administration Content", children));
  }

}
