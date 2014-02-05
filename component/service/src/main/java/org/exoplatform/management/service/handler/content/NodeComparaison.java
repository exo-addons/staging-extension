package org.exoplatform.management.service.handler.content;

import java.io.Serializable;
import java.util.ResourceBundle;

public class NodeComparaison implements Comparable<NodeComparaison>, Serializable {
  private static final long serialVersionUID = -3678314428635211056L;
  public static ResourceBundle resourceBundle;

  String title;
  String path;
  String lastModifierUserName;
  String sourceModificationDate;
  String targetModificationDate;
  NodeComparaisonState state;

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getLastModifierUserName() {
    return lastModifierUserName;
  }

  public void setLastModifierUserName(String lastModifierUserName) {
    this.lastModifierUserName = lastModifierUserName;
  }

  public String getSourceModificationDate() {
    return sourceModificationDate;
  }

  public void setSourceModificationDate(String sourceModificationDate) {
    this.sourceModificationDate = sourceModificationDate;
  }

  public String getTargetModificationDate() {
    return targetModificationDate;
  }

  public void setTargetModificationDate(String targetModificationDate) {
    this.targetModificationDate = targetModificationDate;
  }

  public String getStateLocalized() {
    if (resourceBundle != null) {
      return resourceBundle.getString("PushContent.state." + state.getKey());
    }
    return state.getKey();
  }

  // fakeMethod
  public void setStateLocalized(String fake) {
    // nothinh to do here
  }

  public NodeComparaisonState getState() {
    return state;
  }

  public void setState(NodeComparaisonState state) {
    this.state = state;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  @Override
  public int compareTo(NodeComparaison o) {
    return path.compareTo(o.getPath());
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null || !(obj instanceof NodeComparaison)) {
      return false;
    }
    String otherPath = ((NodeComparaison) obj).getPath();
    return path != null && otherPath != null && otherPath.equals(path);
  }
}
