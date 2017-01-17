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

import com.thoughtworks.xstream.XStream;

import org.exoplatform.management.ecmadmin.operations.ECMAdminImportResource;
import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.services.cms.views.ApplicationTemplateManagerService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.gatein.common.logging.Logger;
import org.gatein.common.logging.LoggerFactory;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.NoResultModel;

import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.jcr.Node;

/**
 * The Class ApplicationsTemplatesImportResource.
 *
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class ApplicationsTemplatesImportResource extends ECMAdminImportResource {
  
  /** The Constant log. */
  final private static Logger log = LoggerFactory.getLogger(ApplicationsTemplatesImportResource.class);
  
  /** The Constant templateEntryPattern. */
  final private static Pattern templateEntryPattern = Pattern.compile("templates/applications/(.*)/(.*)/(.*)\\.gtmpl");

  /** The application template manager service. */
  private ApplicationTemplateManagerService applicationTemplateManagerService;
  
  /** The template service. */
  private TemplateService templateService;

  /**
   * Instantiates a new applications templates import resource.
   */
  public ApplicationsTemplatesImportResource() {
    super(null);
  }

  /**
   * Instantiates a new applications templates import resource.
   *
   * @param filePath the file path
   */
  public ApplicationsTemplatesImportResource(String filePath) {
    super(filePath);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws OperationException {
    // get attributes and attachement inputstream
    super.execute(operationContext, resultHandler);

    if (applicationTemplateManagerService == null) {
      applicationTemplateManagerService = operationContext.getRuntimeContext().getRuntimeComponent(ApplicationTemplateManagerService.class);
      if (applicationTemplateManagerService == null) {
        throw new OperationException(OperationNames.IMPORT_RESOURCE, "ApplicationTemplateManagerService doesn't exist.");
      }
    }
    if (templateService == null) {
      templateService = operationContext.getRuntimeContext().getRuntimeComponent(TemplateService.class);
      if (templateService == null) {
        throw new OperationException(OperationNames.IMPORT_RESOURCE, "TemplateService doesn't exist.");
      }
    }

    try {
      ApplicationTemplatesMetadata metadata = null;
      final ZipInputStream zis = new ZipInputStream(attachmentInputStream);
      try {
        ZipEntry entry;
        while ((entry = zis.getNextEntry()) != null) {
          try {
            String filePath = entry.getName();
            if (!filePath.startsWith("ecmadmin/templates/applications/")) {
              continue;
            }
            if (filePath.endsWith("metadata.xml")) {
              // read NT Templates Metadata
              XStream xStream = new XStream();
              xStream.alias("metadata", ApplicationTemplatesMetadata.class);
              if (metadata == null) {
                metadata = (ApplicationTemplatesMetadata) xStream.fromXML(new InputStreamReader(zis));
              } else {
                ApplicationTemplatesMetadata tmpMetadata = (ApplicationTemplatesMetadata) xStream.fromXML(new InputStreamReader(zis));
                metadata.getTitleMap().putAll(tmpMetadata.getTitleMap());
              }
              continue;
            }

            // Skip directories
            // & Skip empty entries
            // & Skip entries not in sites/zip
            if (entry.isDirectory() || filePath.equals("") || !filePath.endsWith(".gtmpl")) {
              continue;
            }

            Matcher matcher = templateEntryPattern.matcher(filePath);
            if (!matcher.find()) {
              continue;
            }

            String applicationName = matcher.group(1);
            String categoryName = matcher.group(2);
            String templateName = matcher.group(3) + ".gtmpl";

            Node category = applicationTemplateManagerService.getApplicationTemplateHome(applicationName, SessionProvider.createSystemProvider()).getNode(categoryName);
            if (category.hasNode(templateName)) {
              if (replaceExisting) {
                log.info("Overwrite existing application template '" + applicationName + "/" + categoryName + "/" + templateName + "'.");
                Node templateNode = category.getNode(templateName);
                templateNode.remove();
                category.getSession().save();
              } else {
                log.info("Ignore existing application template '" + applicationName + "/" + categoryName + "/" + templateName + "'.");
                continue;
              }
            }
            String templateTitle = templateName;
            if (metadata != null) {
              templateTitle = metadata.getTitle(filePath);
            }
            templateService.createTemplate(category, templateTitle, templateName, zis, new String[] { "*" });
          } finally {
            zis.closeEntry();
          }
        }
      } finally {
        zis.close();
      }

    } catch (Exception e) {
      throw new OperationException(OperationNames.IMPORT_RESOURCE, "Error while importing applications templates", e);
    }

    resultHandler.completed(NoResultModel.INSTANCE);
  }

}