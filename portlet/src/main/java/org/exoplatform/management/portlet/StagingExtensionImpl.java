package org.exoplatform.management.portlet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Singleton;

import org.apache.commons.fileupload.FileItem;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.gatein.management.api.ContentType;
import org.gatein.management.api.PathAddress;
import org.gatein.management.api.controller.ManagedRequest;
import org.gatein.management.api.controller.ManagedResponse;
import org.gatein.management.api.controller.ManagementController;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.exceptions.ResourceNotFoundException;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.model.NamedDescription;
import org.gatein.management.api.operation.model.ReadResourceModel;

@Singleton
public class StagingExtensionImpl implements StagingExtension {
  private Log log = ExoLogger.getLogger(StagingExtension.class);
  private ManagementController managementController = null;
  private Map<String, TreeNode> managedResources = new HashMap<String, TreeNode>();

  @Override
  public List<TreeNode> getTree() {
    ManagedRequest request = ManagedRequest.Factory.create(OperationNames.READ_RESOURCE, PathAddress.pathAddress(), ContentType.JSON);
    ManagedResponse response = getManagementController().execute(request);
    if (!response.getOutcome().isSuccess()) {
      throw new RuntimeException(response.getOutcome().getFailureDescription());
    }

    ReadResourceModel result = (ReadResourceModel) response.getResult();
    List<TreeNode> children = new ArrayList<TreeNode>(result.getChildren().size());
    for (String childName : result.getChildren()) {
      TreeNode child = new TreeNode(childName);
      child.setExportable(true);
      if(child.getText().equals("mop")) {
        continue;
      }
      children.add(child);
      computeDataRecursively(child);
    }
    return children;
  }

  @Override
  public TreeNode getNode(String path) {
    return path == null ? null : managedResources.get(path);
  }

  @Override
  public void importResource(TreeNode selectedNode, FileItem file) throws IOException {
    Map<String, List<String>> attributes = new HashMap<String, List<String>>(0);
    ManagedRequest request = ManagedRequest.Factory.create(OperationNames.IMPORT_RESOURCE, PathAddress.pathAddress(selectedNode.getPath()), attributes, file.getInputStream(), ContentType.ZIP);

    ManagedResponse response = managementController.execute(request);
    if (!response.getOutcome().isSuccess()) {
      throw new RuntimeException(response.getOutcome().getFailureDescription());
    }
  }

  private void computeDataRecursively(TreeNode parentNode) {
    managedResources.put(parentNode.getPath(), parentNode);
    ManagedRequest request = ManagedRequest.Factory.create(OperationNames.READ_RESOURCE, PathAddress.pathAddress(parentNode.getPath()), ContentType.JSON);
    ManagedResponse response = null;
    try {
      response = getManagementController().execute(request);
    } catch (ResourceNotFoundException e) {
      // This is expected for some resources.
      if (log.isDebugEnabled()) {
        log.debug(e);
      }
      return;
    } catch (OperationException e) {
      // This is expected for some resources.
      if (log.isDebugEnabled()) {
        log.debug(e);
      }
      return;
    }
    if (!response.getOutcome().isSuccess()) {
      log.warn(response.getOutcome().getFailureDescription());
      return;
    }

    ReadResourceModel result = (ReadResourceModel) response.getResult();
    computeAllowedOperations(parentNode, result);
    computeChildren(parentNode, result);
  }

  private void computeAllowedOperations(TreeNode parentNode, ReadResourceModel result) {
    List<NamedDescription> operations = result.getOperations();
    for (NamedDescription namedDescription : operations) {
      if (namedDescription.getName().equals(OperationNames.EXPORT_RESOURCE)) {
        parentNode.setExportable(true);
      } else if (namedDescription.getName().equals(OperationNames.IMPORT_RESOURCE)) {
        parentNode.setImportable(true);
      }
    }
  }

  private void computeChildren(TreeNode parentNode, ReadResourceModel result) {
    List<TreeNode> children = new ArrayList<TreeNode>(result.getChildren().size());
    parentNode.setChildren(children);
    for (String childName : result.getChildren()) {
      TreeNode child = new TreeNode(childName);
      child.setParent(parentNode);
      children.add(child);
      computeDataRecursively(child);
    }
  }

  public ManagementController getManagementController() {
    if (managementController == null) {
      managementController = (ManagementController) PortalContainer.getInstance().getComponentInstanceOfType(ManagementController.class);
    }
    return managementController;
  }

}
