package org.exoplatform.management.service.handler.content;

public enum NodeComparaisonState {
  NOT_FOUND_ON_SOURCE("not_found_source"), NOT_FOUND_ON_TARGET("not_found_target"), MODIFIED_ON_SOURCE("modified_source"), MODIFIED_ON_TARGET("modified_target"), SAME("same"), UNKNOWN("unkown");
  String key;

  private NodeComparaisonState(String key) {
    this.key = key;
  }

  public String getKey() {
    return key;
  }
}
