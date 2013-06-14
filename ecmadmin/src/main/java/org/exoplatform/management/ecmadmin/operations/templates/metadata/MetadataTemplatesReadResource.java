package org.exoplatform.management.ecmadmin.operations.templates.metadata;

import java.util.HashSet;
import java.util.Set;

import org.exoplatform.services.cms.metadata.MetadataService;
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
public class MetadataTemplatesReadResource implements OperationHandler {
  
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws OperationException {
    Set<String> nodeTypesTemplates = new HashSet<String>();

    MetadataService metadataService = operationContext.getRuntimeContext().getRuntimeComponent(MetadataService.class);

    try {
      nodeTypesTemplates.addAll(metadataService.getMetadataList());
    } catch (Exception e) {
      throw new OperationException(OperationNames.READ_RESOURCE, "Error while retrieving node types templates", e);
    }

    resultHandler.completed(new ReadResourceModel("Available node types templates", nodeTypesTemplates));
  }
}