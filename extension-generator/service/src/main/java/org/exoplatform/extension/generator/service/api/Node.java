package org.exoplatform.extension.generator.service.api;

import java.io.Serializable;

public class Node implements Serializable {
  private static final long serialVersionUID = -8330709372395885654L;

  private String path;
  private String text;
  private String description;

  public Node(String text, String description, String path) {
    this.text = text;
    this.path = path;
    this.description = description;
  }

  public String getText() {
    return text;
  }

  public void setText(String text) {
    this.text = text;
  }

  public String setPath() {
    return this.path;
  }

  public String getPath() {
    return this.path;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    } else if (obj instanceof Node) {
      return text.equals(((Node) obj).getPath());
    } else if (obj instanceof String) {
      return text.equals(obj);
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return path.hashCode();
  }
}
