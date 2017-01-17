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
package org.exoplatform.management.registry.operations;

import org.exoplatform.application.registry.ApplicationRegistryService;
import org.exoplatform.management.common.AbstractOperationHandler;
import org.exoplatform.management.registry.tasks.ApplicationExportTask;
import org.gatein.management.api.PathAddress;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.ExportResourceModel;

/**
 * The Class ApplicationExportResource.
 *
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class ApplicationExportResource extends AbstractOperationHandler {
  
  /** The application registry service. */
  private ApplicationRegistryService applicationRegistryService;

  /**
   * {@inheritDoc}
   */
  @Override
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws OperationException {
    if (applicationRegistryService == null) {
      applicationRegistryService = operationContext.getRuntimeContext().getRuntimeComponent(ApplicationRegistryService.class);
      if (applicationRegistryService == null) {
        throw new OperationException(OperationNames.EXPORT_RESOURCE, "Cannot get ApplicationRegistryService instance.");
      }
    }

    PathAddress address = operationContext.getAddress();
    String categoryName = address.resolvePathTemplate("category-name");
    String applicationName = address.resolvePathTemplate("application-name");

    resultHandler.completed(new ExportResourceModel(new ApplicationExportTask(applicationName, categoryName, applicationRegistryService)));
  }

}