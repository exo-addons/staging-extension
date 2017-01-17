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
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.ReadResourceModel;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.jcr.Node;

/**
 * The Class ScriptReadResource.
 *
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class ScriptReadResource extends AbstractOperationHandler {
  
  /** The script service. */
  private ScriptService scriptService = null;

  /**
   * {@inheritDoc}
   */
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

  /**
   * Generate script names.
   *
   * @param scriptNames the script names
   * @param nodes the nodes
   * @throws Exception the exception
   */
  private void generateScriptNames(Set<String> scriptNames, List<Node> nodes) throws Exception {
    for (Node node : nodes) {
      String scriptPath = node.getPath().replace("/exo:ecm/scripts/", "");
      scriptNames.add(scriptPath);
    }
  }
}
