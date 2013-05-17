package org.exoplatform.management.ecmadmin.operations.templates.metadata;

import java.util.ArrayList;
import java.util.List;

import org.exoplatform.management.ecmadmin.exporttask.StringExportTask;
import org.exoplatform.management.ecmadmin.operations.templates.NodeTemplate;
import org.exoplatform.services.cms.BasePath;
import org.exoplatform.services.cms.metadata.MetadataService;
import org.exoplatform.services.cms.metadata.impl.MetadataServiceImpl;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.operation.OperationAttributes;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.OperationHandler;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.ExportResourceModel;
import org.gatein.management.api.operation.model.ExportTask;

/**
 * @author <a href="mailto:thomas.delhomenie@exoplatform.com">Thomas
 *         Delhoménie</a>
 * @version $Revision$
 */
public class MetadataTemplatesExportResource implements OperationHandler {

  private static final String EXPORT_BASE_PATH = "templates/metadata";

  private MetadataService metadataService;
  private NodeHierarchyCreator nodeHierarchyCreator;
  private MetadataTemplatesMetaData metadata;

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
        // metadata templates are not document templates
        metadata.setDocumentTemplate(false);

        for (boolean isDialog : isDialogValues) {
          String metadataPath = metadataService.getMetadataPath(metadataName, isDialog);
          String metadataRoles = metadataService.getMetadataRoles(metadataName, isDialog);
          String metadataTemplate = metadataService.getMetadataTemplate(metadataName, isDialog);
          String templatePath = metadataPath.substring(templatesBasePath.length());

          exportTasks.add(new StringExportTask(metadataTemplate, EXPORT_BASE_PATH + templatePath + ".gtmpl"));

          metadata.addTemplate(isDialog ? MetadataServiceImpl.DIALOGS : MetadataServiceImpl.VIEWS, new NodeTemplate(templatePath
              + ".gtmpl", metadataRoles));
        }
        exportTasks.add(new MetadataTemplatesMetaDataExportTask(metadata, EXPORT_BASE_PATH + "/" + metadataName));
      }
    } catch (Exception e) {
      throw new OperationException(operationName, "Error while retrieving node types templates", e);
    }

    resultHandler.completed(new ExportResourceModel(exportTasks));
  }
}