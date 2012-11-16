package org.exoplatform.management.ecmadmin.operations.nodetype;

import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.exoplatform.management.ecmadmin.operations.ECMAdminImportResource;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.nodetype.ExtendedNodeTypeManager;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeValue;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeValuesList;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.NoResultModel;
import org.jibx.runtime.BindingDirectory;
import org.jibx.runtime.IBindingFactory;
import org.jibx.runtime.IUnmarshallingContext;

/**
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class NodeTypeImportResource extends ECMAdminImportResource {
  private static final Log log = ExoLogger.getLogger(NodeTypeImportResource.class);
  private RepositoryService repositoryService;
  private String pathPrefix = null;

  public NodeTypeImportResource(String pathPrefix) {
    super(null);
    this.pathPrefix = pathPrefix + "/";
  }

  public NodeTypeImportResource(String pathPrefix, String filePath) {
    super(filePath);
    this.pathPrefix = pathPrefix + "/";
  }

  @Override
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws OperationException {
    // get attributes and attachement inputstream
    super.execute(operationContext, resultHandler);

    if (repositoryService == null) {
      repositoryService = operationContext.getRuntimeContext().getRuntimeComponent(RepositoryService.class);
    }
    try {
      ExtendedNodeTypeManager extManager = (ExtendedNodeTypeManager) repositoryService.getCurrentRepository()
          .getNodeTypeManager();

      if (replaceExisting) {
        log.info("Overwiting '" + pathPrefix.substring(0, pathPrefix.length() - 1)
            + "' behavior isn't safe, ignoring existing nodetypes.");
      }
      ZipInputStream zin = new ZipInputStream(attachmentInputStream);
      ZipEntry ze = null;
      while ((ze = zin.getNextEntry()) != null) {
        if (!ze.getName().startsWith(pathPrefix)) {
          continue;
        }
        IBindingFactory factory = BindingDirectory.getFactory(NodeTypeValuesList.class);
        IUnmarshallingContext uctx = factory.createUnmarshallingContext();
        NodeTypeValuesList nodeTypeValuesList = (NodeTypeValuesList) uctx.unmarshalDocument(zin, null);
        ArrayList<?> ntvList = nodeTypeValuesList.getNodeTypeValuesList();
        for (Object object : ntvList) {
          NodeTypeValue nodeTypeValue = (NodeTypeValue) object;
          // This instruction should be:
          // << importBehavior = replaceExisting ?
          // ExtendedNodeTypeManager.REPLACE_IF_EXISTS :
          // ExtendedNodeTypeManager.IGNORE_IF_EXISTS; >>
          // but this behavior isn't safe.
          int importBehavior = ExtendedNodeTypeManager.IGNORE_IF_EXISTS;
          // try {
          extManager.registerNodeType(nodeTypeValue, importBehavior);
          // REPLACE_IF_EXISTS isn't used, thus comment this part
          // } catch (Exception e) {
          // if (replaceExisting &&
          // extManager.getNodeType(nodeTypeValue.getName()) != null) {
          // log.warn("Error while overwriting nodetype:" +
          // e.getMessage());
          // } else {
          // throw new OperationException(OperationNames.IMPORT_RESOURCE,
          // "Error while importing nodetype : "
          // + nodeTypeValue.getName(), e);
          // }
          // }
        }
        zin.closeEntry();
      }
      zin.close();
      resultHandler.completed(NoResultModel.INSTANCE);
    } catch (Exception exception) {
      throw new OperationException(OperationNames.IMPORT_RESOURCE, "Error while importing nodetypes.", exception);
    }
  }
}
