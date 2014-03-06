/*
 * Copyright (C) 2003-2014 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.management.wiki.operations;

import java.util.ArrayList;
import java.util.List;

import org.exoplatform.wiki.mow.api.Wiki;
import org.exoplatform.wiki.mow.api.WikiType;
import org.exoplatform.wiki.mow.core.api.MOWService;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.exceptions.ResourceNotFoundException;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.OperationHandler;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.ExportResourceModel;
import org.gatein.management.api.operation.model.ExportTask;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Mar 5, 2014  
 */
public class WikiDataExportResource implements OperationHandler {
  
  private MOWService mowService;
  
  private WikiType wikiType;
  
  public WikiDataExportResource(WikiType wikiType) {
    this.wikiType = wikiType;
  }

  @Override
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws ResourceNotFoundException,
                                                                                     OperationException {
    mowService = operationContext.getRuntimeContext()
        .getRuntimeComponent(MOWService.class);
    
    List<ExportTask> exportTasks = new ArrayList<ExportTask>();
    try {
      for (Wiki wiki : mowService.getModel().getWikiStore().getWikiContainer(wikiType).getAllWikis()) {
        exportTasks.add(new WikiExportTask(mowService, wikiType, wiki.getOwner()));
      }
    } catch (Exception exception) {
      throw new OperationException(OperationNames.EXPORT_RESOURCE, "Error while retrieving script", exception);
    }
    resultHandler.completed(new ExportResourceModel(exportTasks));
  }

}
