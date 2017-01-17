/*
 * Copyright (C) 2003-2017 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.exoplatform.management.ecmadmin.operations.script;

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

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;

/**
 * The Class ScriptExportResource.
 *
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class ScriptExportResource extends AbstractOperationHandler {

  /** The script service. */
  private ScriptService scriptService = null;

  /**
   * {@inheritDoc}
   */
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

  /**
   * Generate scripts configuration.
   *
   * @param exportTasks the export tasks
   * @param nodes the nodes
   * @param filters the filters
   * @throws Exception the exception
   */
  private void generateScriptsConfiguration(List<ExportTask> exportTasks, List<Node> nodes, List<String> filters) throws Exception {
    for (Node node : nodes) {
      String scriptPath = node.getPath().replace("/exo:ecm/scripts/", "");
      if (filters.isEmpty() || contains(filters, scriptPath)) {
        String scriptData = node.getNode("jcr:content").getProperty("jcr:data").getString();
        exportTasks.add(new ScriptExportTask(scriptPath, scriptData));
      }
    }
  }

  /**
   * Contains.
   *
   * @param filters the filters
   * @param scriptPath the script path
   * @return true, if successful
   */
  private boolean contains(List<String> filters, String scriptPath) {
    for (String scriptPathTmp : filters) {
      if (scriptPath.endsWith(scriptPathTmp)) {
        return true;
      }
    }
    return false;
  }
}