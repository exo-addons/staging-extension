package org.exoplatform.management.ecmadmin.operations.templates.nodetypes;

import java.util.HashSet;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;

import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
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
public class NodeTypesTemplatesReadResource implements OperationHandler {
  @Override
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws OperationException {
    Set<String> nodeTypesTemplates = new HashSet<String>();

    TemplateService templateService = operationContext.getRuntimeContext().getRuntimeComponent(TemplateService.class);

    try {
      Node templatesHome = templateService.getTemplatesHome(WCMCoreUtils.getSystemSessionProvider());
      if (templatesHome != null) {
        NodeIterator templatesNodes = templatesHome.getNodes();
        while (templatesNodes.hasNext()) {
          Node node = templatesNodes.nextNode();
          nodeTypesTemplates.add(node.getName());
        }
      } else {
        throw new Exception("Unable to retrieve templates root node");
      }
    } catch (Exception e) {
      throw new OperationException(OperationNames.READ_RESOURCE, "Error while retrieving node types templates", e);
    }

    resultHandler.completed(new ReadResourceModel("Available node types templates", nodeTypesTemplates));
  }
}