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
package org.exoplatform.management.answer;

import java.util.HashSet;

import org.exoplatform.management.answer.operations.AnswerDataExportResource;
import org.exoplatform.management.answer.operations.AnswerDataImportResource;
import org.exoplatform.management.answer.operations.AnswerDataReadResource;
import org.exoplatform.management.answer.operations.AnswerReadResource;
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
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class AnswerExtension implements ManagementExtension {

  public static final String PUBLIC_FAQ_TYPE = "public";
  public static final String SPACE_FAQ_TYPE = "space";

  public static final String ROOT_CATEGORY = "Default category";

  @Override
  public void initialize(ExtensionContext context) {
    ComponentRegistration faqRegistration = context.registerManagedComponent("answer");

    ManagedResource.Registration faq = faqRegistration.registerManagedResource(description("Forum resources."));

    faq.registerOperationHandler(OperationNames.READ_RESOURCE, new AnswerReadResource(), description("Lists available faqs"));

//    ManagedResource.Registration settings = faq.registerSubResource("settings", description("portal faq"));
//    settings.registerOperationHandler(OperationNames.READ_RESOURCE, new EmptyReadResource(), description("Forum settings"));
//    settings.registerOperationHandler(OperationNames.EXPORT_RESOURCE, new ForumSettingsExportResource(), description("export faq category"));
//    settings.registerOperationHandler(OperationNames.IMPORT_RESOURCE, new ForumSettingsImportResource(), description("import faq category"));

    ManagedResource.Registration portal = faq.registerSubResource(PUBLIC_FAQ_TYPE, description("public faq"));
    portal.registerOperationHandler(OperationNames.READ_RESOURCE, new AnswerDataReadResource(false), description("Read non spaces faq categories"));
    portal.registerOperationHandler(OperationNames.EXPORT_RESOURCE, new AnswerDataExportResource(false), description("export faq category"));
    portal.registerOperationHandler(OperationNames.IMPORT_RESOURCE, new AnswerDataImportResource(false), description("import faq category"));

    ManagedResource.Registration group = faq.registerSubResource(SPACE_FAQ_TYPE, description("space faqs"));
    group.registerOperationHandler(OperationNames.READ_RESOURCE, new AnswerDataReadResource(true), description("Read spaces faq categories"));
    group.registerOperationHandler(OperationNames.EXPORT_RESOURCE, new AnswerDataExportResource(true), description("export faq category"));
    group.registerOperationHandler(OperationNames.IMPORT_RESOURCE, new AnswerDataImportResource(true), description("import faq category"));
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