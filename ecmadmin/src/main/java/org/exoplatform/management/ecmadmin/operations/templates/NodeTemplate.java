package org.exoplatform.management.ecmadmin.operations.templates;

public class NodeTemplate {
  private String templateFile;
  private String roles;

  public NodeTemplate() {}

  public NodeTemplate(String templateFile, String roles) {
    this.templateFile = templateFile;
    this.roles = roles;
  }

  public String getTemplateFile() {
    return templateFile;
  }

  public void setTemplateFile(String templateFile) {
    this.templateFile = templateFile;
  }

  public String getRoles() {
    return roles;
  }

  public void setRoles(String roles) {
    this.roles = roles;
  }
}
