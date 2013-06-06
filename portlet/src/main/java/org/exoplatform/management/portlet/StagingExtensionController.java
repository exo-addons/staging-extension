package org.exoplatform.management.portlet;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import juzu.Path;
import juzu.Resource;
import juzu.Response;
import juzu.SessionScoped;
import juzu.View;
import juzu.template.Template;

import org.apache.commons.fileupload.FileItem;
import org.exoplatform.commons.juzu.ajax.Ajax;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

@SessionScoped
public class StagingExtensionController {
  private static Log log = ExoLogger.getLogger(StagingExtensionController.class);

  @Inject
  StagingExtension stagingExtensionService;

  @Inject
  @Path("form.gtmpl")
  Template form;

  @Inject
  @Path("index.gtmpl")
  Template index;

  String selectedPath = "";
  String message = "";
  String messageClass = "";
  TreeNode selectedNode;

  @View
  public Response.Render index() {
    List<TreeNode> treeNodes = stagingExtensionService.getTree();
    if (treeNodes == null) {
      throw new IllegalStateException("Staging Extension Tree is empty.");
    }
    Map<String, Object> parameters = new HashMap<String, Object>();
    parameters.put("selectedPath", selectedPath);
    parameters.put("selectedNode", selectedNode);
    parameters.put("messageClass", messageClass);
    parameters.put("message", message);
    parameters.put("nodes", treeNodes);
    return index.ok(parameters);
  }

  @Resource
  public Response.Content<?> importResource(FileItem file) throws IOException {
    if (file == null || file.getSize() == 0) {
      return Response.content(500, "File is empty.");
    }
    if (selectedNode == null) {
      return Response.content(500, "You have to select a resource.");
    }    try {
      stagingExtensionService.importResource(selectedNode, file);
      return Response.ok("Successfully proceeded!");
    } catch (Exception e) {
      log.error(e);
      return Response.content(500, "Error occured while importing resource. See full stack trace in log file");
    }
  }

  @Ajax
  @Resource
  public void displayForm(String path) {
    Map<String, Object> parameters = new HashMap<String, Object>();
    TreeNode node = stagingExtensionService.getNode(path);
    if (node != null) {
      selectedPath = path;
      selectedNode = node;
    }
    parameters.put("selectedPath", selectedPath);
    parameters.put("selectedNode", selectedNode);
    parameters.put("message", message);
    parameters.put("messageClass", messageClass);
    form.render(parameters);
  }
}
