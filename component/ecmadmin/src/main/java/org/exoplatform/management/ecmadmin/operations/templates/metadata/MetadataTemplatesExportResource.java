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
package org.exoplatform.management.ecmadmin.operations.templates.metadata;

import org.exoplatform.management.common.AbstractOperationHandler;
import org.exoplatform.management.ecmadmin.exporttask.StringExportTask;
import org.exoplatform.management.ecmadmin.operations.templates.NodeTemplate;
import org.exoplatform.services.cms.BasePath;
import org.exoplatform.services.cms.metadata.MetadataService;
import org.exoplatform.services.cms.metadata.impl.MetadataServiceImpl;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.operation.OperationAttributes;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.ExportResourceModel;
import org.gatein.management.api.operation.model.ExportTask;

import java.util.ArrayList;
import java.util.List;

/**
 * The Class MetadataTemplatesExportResource.
 *
 * @author <a href="mailto:thomas.delhomenie@exoplatform.com">Thomas
 *         Delhoménie</a>
 * @version $Revision$
 */
public class MetadataTemplatesExportResource extends AbstractOperationHandler {

  /** The Constant EXPORT_BASE_PATH. */
  private static final String EXPORT_BASE_PATH = "ecmadmin/templates/metadata";

  /** The metadata service. */
  private MetadataService metadataService;
  
  /** The node hierarchy creator. */
  private NodeHierarchyCreator nodeHierarchyCreator;
  
  /** The metadata. */
  private MetadataTemplatesMetaData metadata;

  /**
   * {@inheritDoc}
   */
  @Override
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws OperationException {

    String operationName = operationContext.getOperationName();
    OperationAttributes attributes = operationContext.getAttributes();
    List<String> filters = attributes.getValues("filter");

    List<ExportTask> exportTasks = new ArrayList<ExportTask>();

    metadataService = operationContext.getRuntimeContext().getRuntimeComponent(MetadataService.class);
    nodeHierarchyCreator = operationContext.getRuntimeContext().getRuntimeComponent(NodeHierarchyCreator.class);

    try {
      // MetadataService does not expose the templates base node path,
      // "internal" method is used...
      String templatesBasePath = nodeHierarchyCreator.getJcrPath(BasePath.METADATA_PATH);

      // loop over all metadata and export templates. Only views and
      // dialogs exist for metadata templates, no skins templates.
      boolean[] isDialogValues = new boolean[] { true, false };
      List<String> metadataList = metadataService.getMetadataList();
      for (String metadataName : metadataList) {
        if (filters != null && !filters.isEmpty() && !filters.contains(metadataName)) {
          continue;
        }
        metadata = new MetadataTemplatesMetaData();

        // TODO label is not exposed by the API...
        metadata.setLabel("");
        metadata.setNodeTypeName(metadataName);
        // metadata templates are not document templates
        metadata.setDocumentTemplate(false);

        for (boolean isDialog : isDialogValues) {
          String metadataPath = metadataService.getMetadataPath(metadataName, isDialog);
          String metadataRoles = metadataService.getMetadataRoles(metadataName, isDialog);
          String metadataTemplate = metadataService.getMetadataTemplate(metadataName, isDialog);
          String templatePath = metadataPath.substring(templatesBasePath.length());

          exportTasks.add(new StringExportTask(metadataTemplate, EXPORT_BASE_PATH + templatePath + ".gtmpl"));

          metadata.addTemplate(isDialog ? MetadataServiceImpl.DIALOGS : MetadataServiceImpl.VIEWS, new NodeTemplate(templatePath + ".gtmpl", metadataRoles));
        }
        exportTasks.add(new MetadataTemplatesMetaDataExportTask(metadata, EXPORT_BASE_PATH + "/" + metadataName));
      }
    } catch (Exception e) {
      throw new OperationException(operationName, "Error while retrieving metadata templates", e);
    }

    resultHandler.completed(new ExportResourceModel(exportTasks));
  }
}