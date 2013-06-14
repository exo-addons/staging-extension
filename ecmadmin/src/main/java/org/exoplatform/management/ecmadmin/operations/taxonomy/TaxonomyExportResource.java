package org.exoplatform.management.ecmadmin.operations.taxonomy;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;

import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.services.cms.taxonomy.TaxonomyService;
import org.exoplatform.services.jcr.RepositoryService;
import org.gatein.management.api.PathAddress;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.OperationHandler;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.ExportResourceModel;
import org.gatein.management.api.operation.model.ExportTask;

/**
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class TaxonomyExportResource implements OperationHandler {

  private static final String EXPORT_BASE_PATH = "taxonomy";

  private TaxonomyService taxonomyService;
  private RepositoryService repositoryService;

  
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws OperationException {
    PathAddress address = operationContext.getAddress();
    String taxonomyName = address.resolvePathTemplate("taxonomy-name");

    List<ExportTask> exportTasks = new ArrayList<ExportTask>();
    if (taxonomyService == null) {
      taxonomyService = operationContext.getRuntimeContext().getRuntimeComponent(TaxonomyService.class);
    }
    if (repositoryService == null) {
      repositoryService = operationContext.getRuntimeContext().getRuntimeComponent(RepositoryService.class);
    }
    try {
      Node taxonomyNode = taxonomyService.getTaxonomyTree(taxonomyName, true);
      TaxonomyMetaData taxonomyMetaData = setData(taxonomyNode);
      exportTasks.add(new TaxonomyMetaDataExportTask(taxonomyMetaData, EXPORT_BASE_PATH + "/"
          + taxonomyMetaData.getTaxoTreeName()));
      exportTasks.add(new TaxonomyTreeExportTask(repositoryService, taxonomyMetaData, EXPORT_BASE_PATH + "/"
          + taxonomyMetaData.getTaxoTreeName()));
    } catch (Exception e) {
      throw new OperationException(OperationNames.EXPORT_RESOURCE, "Error while retrieving taxonomy" + taxonomyName, e);
    }
    resultHandler.completed(new ExportResourceModel(exportTasks));
  }

  private TaxonomyMetaData setData(Node node) {
    TaxonomyMetaData taxonomyTreeData = null;
    try {
      if (node != null) {
        taxonomyTreeData = new TaxonomyMetaData();
        taxonomyTreeData.setTaxoTreeName(node.getName());
        if (!Utils.isInTrash(node)) {
          taxonomyTreeData.setEdit(true);
        }
        taxonomyTreeData.setTaxoTreeHomePath(node.getPath());
        taxonomyTreeData.setTaxoTreeWorkspace(node.getSession().getWorkspace().getName());
        Node realTreeNode = taxonomyService.getTaxonomyTree(node.getName(), true);
        Value[] values = realTreeNode.getProperty("exo:permissions").getValues();
        StringBuffer buffer = new StringBuffer(1024);
        try {
          for (Value permission : values) {
            buffer.append(permission.getString()).append(';');
          }
        } catch (ValueFormatException e) {} catch (RepositoryException e) {}
        String permission = buffer.toString();
        taxonomyTreeData.setTaxoTreePermissions(permission.substring(0, permission.length() - 1));
      }
    } catch (RepositoryException e) {
      throw new OperationException(OperationNames.EXPORT_RESOURCE, "Error while retrieving taxonomy content.", e);
    }
    return taxonomyTreeData;
  }
}