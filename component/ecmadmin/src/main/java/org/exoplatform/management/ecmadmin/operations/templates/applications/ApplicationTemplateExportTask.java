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
package org.exoplatform.management.ecmadmin.operations.templates.applications;

import org.apache.commons.io.IOUtils;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.services.cms.views.ApplicationTemplateManagerService;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.model.ExportTask;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.jcr.Node;
import javax.jcr.Value;

/**
 * The Class ApplicationTemplateExportTask.
 *
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class ApplicationTemplateExportTask implements ExportTask {
  
  /** The application template manager service. */
  private final ApplicationTemplateManagerService applicationTemplateManagerService;
  
  /** The application name. */
  private final String applicationName;
  
  /** The template path. */
  private String templatePath;
  
  /** The template category. */
  private final String templateCategory;
  
  /** The template name. */
  private final String templateName;
  
  /** The metadata. */
  private ApplicationTemplatesMetadata metadata;

  /**
   * Instantiates a new application template export task.
   *
   * @param applicationTemplateManagerService the application template manager service
   * @param applicationName the application name
   * @param templateCategory the template category
   * @param templateName the template name
   * @param metadata the metadata
   */
  public ApplicationTemplateExportTask(ApplicationTemplateManagerService applicationTemplateManagerService, String applicationName, String templateCategory, String templateName,
      ApplicationTemplatesMetadata metadata) {
    this.applicationTemplateManagerService = applicationTemplateManagerService;
    this.applicationName = applicationName;
    this.templateCategory = templateCategory;
    this.templateName = templateName;
    this.templatePath = "";
    if (templateCategory != null && !templateCategory.isEmpty()) {
      this.templatePath += templateCategory + "/";
    }
    this.templatePath += templateName;
    this.metadata = metadata;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getEntry() {
    return "ecmadmin/templates/applications/" + this.applicationName + "/" + templatePath;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void export(OutputStream outputStream) throws IOException {
    InputStream templateFileIS = null;
    try {
      Node templateNode = applicationTemplateManagerService.getTemplateByName(this.applicationName, this.templateCategory, this.templateName, WCMCoreUtils.getSystemSessionProvider());

      Node contentNode = templateNode.getNode(Utils.JCR_CONTENT);

      templateFileIS = contentNode.getProperty("jcr:data").getStream();
      IOUtils.copy(templateFileIS, outputStream);

      if (contentNode.hasProperty(NodetypeConstant.DC_TITLE)) {
        Value[] values = contentNode.getProperty(NodetypeConstant.DC_TITLE).getValues();
        if (values != null && values.length > 0) {
          String title = values[0].getString();
          metadata.addTitle(getEntry(), title);
        }
      }
    } catch (Exception e) {
      throw new OperationException(OperationNames.EXPORT_RESOURCE, "Unable to export template " + this.applicationName + "/" + this.templatePath, e);
    } finally {
      if (templateFileIS != null) {
        templateFileIS.close();
      }
    }
  }

}
