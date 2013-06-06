package org.exoplatform.management.portlet;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class TreeNode implements Serializable {
  private static final long serialVersionUID = 4770044446439824690L;

  private TreeNode parent;
  private List<TreeNode> children;
  private String text;
  private boolean importable = false;
  private boolean exportable = false;

  public TreeNode(String text) {
    this.text = text;
    this.children = new ArrayList<TreeNode>();
  }

  public TreeNode(String text, List<TreeNode> children) {
    this.text = text;
    if (children == null) {
      this.children = new ArrayList<TreeNode>();
    } else {
      this.children = children;
    }
  }

  public List<TreeNode> getChildren() {
    return this.children;
  }

  public void setChildren(List<TreeNode> children) {
    if (children == null) {
      this.children = new ArrayList<TreeNode>();
    } else {
      this.children = children;
    }
  }

  public void addChild(TreeNode child) {
    this.children.add(child);
  }

  public String getText() {
    return text;
  }

  public void setText(String text) {
    this.text = text;
  }

  public String getPathHTMLLink() {
    return (this.parent != null ? this.parent.getPath() : "") + " &raquo; " + this.text;
  }

  public String getPath() {
    return (this.parent != null ? this.parent.getPath() : "") + "/" + this.text;
  }

  public boolean isExportable() {
    return exportable;
  }

  public void setExportable(boolean exportable) {
    this.exportable = exportable;
  }

  public TreeNode getParent() {
    return parent;
  }

  public void setParent(TreeNode parent) {
    this.parent = parent;
  }

  public boolean isImportable() {
    return importable;
  }

  public void setImportable(boolean importable) {
    this.importable = importable;
  }
}
