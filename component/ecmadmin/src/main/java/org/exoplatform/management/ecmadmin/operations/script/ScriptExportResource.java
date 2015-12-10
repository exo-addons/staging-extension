package org.exoplatform.management.ecmadmin.operations.script;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;

import org.exoplatform.management.common.AbstractOperationHandler;
import org.exoplatform.services.cms.scripts.ScriptService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.operation.OperationAttributes;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.ExportResourceModel;
import org.gatein.management.api.operation.model.ExportTask;

/**
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class ScriptExportResource extends AbstractOperationHandler {

  private ScriptService scriptService = null;

  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws OperationException {
    OperationAttributes attributes = operationContext.getAttributes();
    List<String> filters = attributes.getValues("filter");
    if (scriptService == null) {
      scriptService = operationContext.getRuntimeContext().getRuntimeComponent(ScriptService.class);
    }
    List<ExportTask> exportTasks = new ArrayList<ExportTask>();
    try {
      // Add script definition into the InitParams of the Component
      SessionProvider systemSessionProvider = SessionProvider.createSystemProvider();

      List<Node> ecmActionScripts = scriptService.getECMActionScripts(systemSessionProvider);
      generateScriptsConfiguration(exportTasks, ecmActionScripts, filters);

      List<Node> ecmInterceptorScripts = scriptService.getECMInterceptorScripts(systemSessionProvider);
      generateScriptsConfiguration(exportTasks, ecmInterceptorScripts, filters);

      List<Node> ecmWidgetScripts = scriptService.getECMWidgetScripts(systemSessionProvider);
      generateScriptsConfiguration(exportTasks, ecmWidgetScripts, filters);
    } catch (Exception exception) {
      throw new OperationException(OperationNames.EXPORT_RESOURCE, "Error while retrieving script", exception);
    }
    resultHandler.completed(new ExportResourceModel(exportTasks));
  }

  private void generateScriptsConfiguration(List<ExportTask> exportTasks, List<Node> nodes, List<String> filters) throws Exception {
    for (Node node : nodes) {
      String scriptPath = node.getPath().replace("/exo:ecm/scripts/", "");
      if (filters.isEmpty() || contains(filters, scriptPath)) {
        String scriptData = node.getNode("jcr:content").getProperty("jcr:data").getString();
        exportTasks.add(new ScriptExportTask(scriptPath, scriptData));
      }
    }
  }

  private boolean contains(List<String> filters, String scriptPath) {
    for (String scriptPathTmp : filters) {
      if (scriptPath.endsWith(scriptPathTmp)) {
        return true;
      }
    }
    return false;
  }
}