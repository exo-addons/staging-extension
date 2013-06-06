package org.exoplatform.management.portlet;

import java.io.IOException;
import java.util.List;

import org.apache.commons.fileupload.FileItem;


public interface StagingExtension {

  List<TreeNode> getTree();

  TreeNode getNode(String path);

  void importResource(TreeNode selectedNode, FileItem file) throws IOException;

}
