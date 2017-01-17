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
package org.exoplatform.management.ecmadmin.operations.templates.applications;

import org.exoplatform.management.common.AbstractOperationHandler;
import org.exoplatform.services.cms.views.ApplicationTemplateManagerService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.gatein.management.api.PathAddress;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.ExportResourceModel;
import org.gatein.management.api.operation.model.ExportTask;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.jcr.Node;
import javax.jcr.NodeIterator;

/**
 * The Class ApplicationTemplatesExportResource.
 *
 * @author <a href="mailto:thomas.delhomenie@exoplatform.com">Thomas
 *         Delhoménie</a>
 * @version $Revision$
 */
public class ApplicationTemplatesExportResource extends AbstractOperationHandler {

  /** The template entry pattern. */
  private Pattern templateEntryPattern = Pattern.compile("(.*)/(.*)/(.*)\\.gtmpl");

  /**
   * {@inheritDoc}
   */
  @Override
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws OperationException {
    String operationName = operationContext.getOperationName();
    PathAddress address = operationContext.getAddress();

    String applicationName = address.resolvePathTemplate("application-name");
    if (applicationName == null) {
      throw new OperationException(operationName, "No application name specified.");
    }

    Matcher matcher = templateEntryPattern.matcher(applicationName);
    String categoryName = null;
    String templateName = null;
    if (matcher.find()) {
      applicationName = matcher.group(1);
      categoryName = matcher.group(2);
      templateName = matcher.group(3) + ".gtmpl";
    }

    List<ExportTask> exportTasks = new ArrayList<ExportTask>();

    ApplicationTemplateManagerService applicationTemplateManagerService = operationContext.getRuntimeContext().getRuntimeComponent(ApplicationTemplateManagerService.class);
    try {
      Node templatesHome = applicationTemplateManagerService.getApplicationTemplateHome(applicationName, SessionProvider.createSystemProvider());
      if (templatesHome != null) {
        ApplicationTemplatesMetadata metadata = new ApplicationTemplatesMetadata();
        if (templateName != null) {
          exportTasks.add(new ApplicationTemplateExportTask(applicationTemplateManagerService, applicationName, categoryName, templateName, metadata));
        } else {
          NodeIterator nodeIterator = null;
          if (categoryName == null) {
            nodeIterator = templatesHome.getNodes();
          } else {
            nodeIterator = templatesHome.getNode(categoryName).getNodes();
          }
          while (nodeIterator.hasNext()) {
            Node categoryNode = nodeIterator.nextNode();
            if (categoryNode.getName().endsWith(".gtmpl")) {
              Node templateNode = categoryNode;
              categoryNode = templateNode.getParent();
              exportTasks.add(new ApplicationTemplateExportTask(applicationTemplateManagerService, applicationName, categoryNode.getName(), templateNode.getName(), metadata));
            } else {
              NodeIterator templatesIterator = categoryNode.getNodes();
              while (templatesIterator.hasNext()) {
                Node templateNode = templatesIterator.nextNode();
                exportTasks.add(new ApplicationTemplateExportTask(applicationTemplateManagerService, applicationName, categoryNode.getName(), templateNode.getName(), metadata));
              }
            }
          }
        }
        String applicationPath = applicationName + (categoryName == null ? "" : "/" + categoryName + (templateName == null ? "" : "/" + templateName.replaceAll(".gtmpl", "") + "/"));
        exportTasks.add(new ApplicationTemplatesMetaDataExportTask(metadata, applicationPath));
      }

      resultHandler.completed(new ExportResourceModel(exportTasks));
    } catch (Exception e) {
      throw new OperationException(OperationNames.READ_RESOURCE, "Error while retrieving applications with templates", e);
    }
  }
}