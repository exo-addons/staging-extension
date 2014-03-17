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
package org.exoplatform.management.wiki;

import java.util.HashSet;

import org.exoplatform.management.wiki.operations.WikiDataExportResource;
import org.exoplatform.management.wiki.operations.WikiDataImportResource;
import org.exoplatform.management.wiki.operations.WikiDataReadResource;
import org.exoplatform.management.wiki.operations.WikiReadResource;
import org.exoplatform.wiki.mow.api.WikiType;
import org.gatein.management.api.ComponentRegistration;
import org.gatein.management.api.ManagedDescription;
import org.gatein.management.api.ManagedResource;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.exceptions.ResourceNotFoundException;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.OperationHandler;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.ReadResourceModel;
import org.gatein.management.spi.ExtensionContext;
import org.gatein.management.spi.ManagementExtension;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Mar 5, 2014  
 */
public class WikiExtension implements ManagementExtension {

  @Override
  public void initialize(ExtensionContext context) {
    ComponentRegistration wikiRegistration = context.registerManagedComponent("wiki");

    ManagedResource.Registration wiki = 
        wikiRegistration.registerManagedResource(description("Wiki resources."));
    
    wiki.registerOperationHandler(OperationNames.READ_RESOURCE, new WikiReadResource(), 
                                  description("Lists available wikis"));
    
    ManagedResource.Registration portal = wiki.registerSubResource("portal", description("portal wikis"));
    portal.registerOperationHandler(OperationNames.READ_RESOURCE, new WikiDataReadResource(WikiType.PORTAL), 
                                    description("portal wiki resources"));
    portal.registerOperationHandler(OperationNames.EXPORT_RESOURCE, new WikiDataExportResource(WikiType.PORTAL), 
                                    description("export portal wiki"));
    portal.registerOperationHandler(OperationNames.IMPORT_RESOURCE, new WikiDataImportResource(WikiType.PORTAL), 
                                    description("import portal wiki"));
    
    ManagedResource.Registration group = wiki.registerSubResource("group", description("group wikis"));
    group.registerOperationHandler(OperationNames.READ_RESOURCE, new WikiDataReadResource(WikiType.GROUP), 
                                    description("group wiki resources"));
    group.registerOperationHandler(OperationNames.EXPORT_RESOURCE, new WikiDataExportResource(WikiType.GROUP), 
                                    description("export group wiki"));
    group.registerOperationHandler(OperationNames.IMPORT_RESOURCE, new WikiDataImportResource(WikiType.GROUP), 
                                    description("import groups wiki"));
    
    ManagedResource.Registration user = wiki.registerSubResource("user", description("users wikis"));
    user.registerOperationHandler(OperationNames.READ_RESOURCE, new WikiDataReadResource(WikiType.USER), 
                                    description("users wiki resources"));
    user.registerOperationHandler(OperationNames.EXPORT_RESOURCE, new WikiDataExportResource(WikiType.USER), 
                                    description("export users wiki"));
    user.registerOperationHandler(OperationNames.IMPORT_RESOURCE, new WikiDataImportResource(WikiType.USER), 
                                    description("import users wiki"));
    
  }
  
  @Override
  public void destroy() {
  }

  private static ManagedDescription description(final String description) {
    return new ManagedDescription() {
      @Override
      public String getDescription() {
        return description;
      }
    };
  }

  public static class EmptyReadResource implements OperationHandler {
    @Override
    public void execute(OperationContext operationContext, ResultHandler resultHandler) throws ResourceNotFoundException, OperationException {
      resultHandler.completed(new ReadResourceModel("Empty", new HashSet<String>()));
    }

  }

}