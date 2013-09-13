package org.exoplatform.management.ecmadmin.operations.script;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.jcr.Node;

import org.exoplatform.services.cms.scripts.ScriptService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
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
public class ScriptReadResource implements OperationHandler {
  private ScriptService scriptService = null;

  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws OperationException {
    if (scriptService == null) {
      scriptService = operationContext.getRuntimeContext().getRuntimeComponent(ScriptService.class);
    }

    Set<String> scriptNames = new TreeSet<String>();
    try {
      SessionProvider systemSessionProvider = SessionProvider.createSystemProvider();

      List<Node> ecmActionScripts = scriptService.getECMActionScripts(systemSessionProvider);
      generateScriptNames(scriptNames, ecmActionScripts);

      List<Node> ecmInterceptorScripts = scriptService.getECMInterceptorScripts(systemSessionProvider);
      generateScriptNames(scriptNames, ecmInterceptorScripts);

      List<Node> ecmWidgetScripts = scriptService.getECMWidgetScripts(systemSessionProvider);
      generateScriptNames(scriptNames, ecmWidgetScripts);

      resultHandler.completed(new ReadResourceModel("Available scripts.", scriptNames));
    } catch (Exception e) {
      throw new OperationException(OperationNames.READ_RESOURCE, "Error while retrieving scripts.", e);
    }
  }

  private void generateScriptNames(Set<String> scriptNames, List<Node> nodes) throws Exception {
    for (Node node : nodes) {
      String scriptPath = node.getPath().replace("/exo:ecm/scripts/", "");
      scriptNames.add(scriptPath);
    }
  }
}
