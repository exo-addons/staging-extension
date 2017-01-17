/*
 * Copyright (C) 2003-2017 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.exoplatform.management.ecmadmin.operations.templates;

/**
 * The Class NodeTemplate.
 */
public class NodeTemplate {
  
  /** The template file. */
  private String templateFile;
  
  /** The roles. */
  private String roles;

  /**
   * Instantiates a new node template.
   */
  public NodeTemplate() {}

  /**
   * Instantiates a new node template.
   *
   * @param templateFile the template file
   * @param roles the roles
   */
  public NodeTemplate(String templateFile, String roles) {
    super();
    this.templateFile = templateFile;
    this.roles = roles;
  }

  /**
   * Gets the template file.
   *
   * @return the template file
   */
  public String getTemplateFile() {
    return templateFile;
  }

  /**
   * Sets the template file.
   *
   * @param templateFile the new template file
   */
  public void setTemplateFile(String templateFile) {
    this.templateFile = templateFile;
  }

  /**
   * Gets the roles.
   *
   * @return the roles
   */
  public String getRoles() {
    return roles;
  }

  /**
   * Sets the roles.
   *
   * @param roles the new roles
   */
  public void setRoles(String roles) {
    this.roles = roles;
  }
}
