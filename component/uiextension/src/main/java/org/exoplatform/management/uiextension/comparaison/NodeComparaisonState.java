package org.exoplatform.management.uiextension.comparaison;

import java.util.ResourceBundle;

public enum NodeComparaisonState
{
  NOT_FOUND_ON_TARGET("not_found_target"), MODIFIED_ON_SOURCE("modified_source"), MODIFIED_ON_TARGET("modified_target"), NOT_FOUND_ON_SOURCE("not_found_source"), SAME("same"), UNKNOWN("unkown");
  String key;

  private NodeComparaisonState(String key) {
    this.key = key;
  }

  public String getKey() {
    return key;
  }

  public String getLabel(ResourceBundle resourceBundle) {
    if (resourceBundle != null) {
      return resourceBundle.getString("PushContent.state." + key);
    }
    return key;
  }

}
