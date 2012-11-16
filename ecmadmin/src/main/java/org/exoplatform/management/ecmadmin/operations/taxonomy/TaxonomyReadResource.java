package org.exoplatform.management.ecmadmin.operations.taxonomy;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.jcr.Node;

import org.exoplatform.services.cms.taxonomy.TaxonomyService;
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
public class TaxonomyReadResource implements OperationHandler {
  private TaxonomyService taxonomyService;

  @Override
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws OperationException {
    Set<String> taxonomies = new HashSet<String>();
    try {
      if (taxonomyService == null) {
        taxonomyService = operationContext.getRuntimeContext().getRuntimeComponent(TaxonomyService.class);
      }
      List<Node> lstTaxonomyTreeNode = taxonomyService.getAllTaxonomyTrees(true);
      for (Node node : lstTaxonomyTreeNode) {
        taxonomies.add(node.getName());
      }
    } catch (Exception e) {
      throw new OperationException(OperationNames.READ_RESOURCE, "Error while retrieving taxonomies", e);
    }
    resultHandler.completed(new ReadResourceModel("Available taxonomies", taxonomies));
  }
}