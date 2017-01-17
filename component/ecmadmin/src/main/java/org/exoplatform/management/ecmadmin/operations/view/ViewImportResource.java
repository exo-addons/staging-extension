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
package org.exoplatform.management.ecmadmin.operations.view;

import org.apache.tika.io.IOUtils;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ObjectParameter;
import org.exoplatform.management.ecmadmin.operations.ECMAdminImportResource;
import org.exoplatform.services.cms.BasePath;
import org.exoplatform.services.cms.views.ManageViewService;
import org.exoplatform.services.cms.views.ViewConfig;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.NoResultModel;

import java.util.Iterator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.jcr.Node;

/**
 * The Class ViewImportResource.
 *
 * @author <a href="mailto:boubaker.khanfir@exoplatform.com">Boubaker
 *         Khanfir</a>
 */
public class ViewImportResource extends ECMAdminImportResource {
  
  /** The Constant log. */
  private static final Log log = ExoLogger.getLogger(ViewImportResource.class);
  
  /** The view service. */
  private static ManageViewService viewService = null;
  
  /** The node hierarchy creator. */
  private static NodeHierarchyCreator nodeHierarchyCreator;

  /**
   * Instantiates a new view import resource.
   */
  public ViewImportResource() {
    this(null);
  }

  /**
   * Instantiates a new view import resource.
   *
   * @param filePath the file path
   */
  public ViewImportResource(String filePath) {
    super(filePath);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws OperationException {
    // get attributes and attachement inputstream
    super.execute(operationContext, resultHandler);

    if (viewService == null) {
      viewService = operationContext.getRuntimeContext().getRuntimeComponent(ManageViewService.class);
    }
    if (nodeHierarchyCreator == null) {
      nodeHierarchyCreator = operationContext.getRuntimeContext().getRuntimeComponent(NodeHierarchyCreator.class);
    }

    SessionProvider sessionProvider = SessionProvider.createSystemProvider();
    String templatesHomePath = getTemplatesHomePath() + "/";

    final ZipInputStream zin = new ZipInputStream(attachmentInputStream);
    try {
      ZipEntry entry;
      try {
        while ((entry = zin.getNextEntry()) != null) {
          try {
            String filePath = entry.getName();
            if (!filePath.startsWith("ecmadmin/view/")) {
              continue;
            }
            // Skip directories
            // & Skip empty entries
            // & Skip entries not in sites/zip
            if (entry.isDirectory() || filePath.trim().isEmpty() || !(filePath.endsWith(".gtmpl") || filePath.endsWith(".xml"))) {
              continue;
            }
            if (filePath.endsWith(".gtmpl")) {
              String templateName = extractTemplateName(filePath);

              log.debug("Reading Stream for template: " + templateName);
              String content = IOUtils.toString(zin);

              Node template = null;
              try {
                template = viewService.getTemplate(templatesHomePath + templateName, sessionProvider);
              } catch (Exception e) {
                // template does not exist, ignore the error
              }

              if (template != null) {
                if (replaceExisting) {
                  log.info("Overwrite existing view template: " + templateName);
                  viewService.updateTemplate(templateName, content, templatesHomePath, sessionProvider);
                } else {
                  log.info("Ignore existing view template: " + templateName);
                }
              } else {
                log.info("Add new view template: " + templateName);
                viewService.addTemplate(templateName, content, templatesHomePath, sessionProvider);
              }

            } else if (filePath.endsWith(".xml")) {
              log.debug("Parsing : " + filePath);

              InitParams initParams = Utils.fromXML(IOUtils.toByteArray(zin), InitParams.class);
              Iterator<ObjectParameter> iterator = initParams.getObjectParamIterator();
              while (iterator.hasNext()) {
                ObjectParameter objectParameter = (ObjectParameter) iterator.next();
                if (!(objectParameter.getObject() instanceof ViewConfig)) {
                  continue;
                }
                ViewConfig config = (ViewConfig) objectParameter.getObject();
                if (viewService.hasView(config.getName())) {
                  if (replaceExisting) {
                    log.info("Overwrite existing view: " + config.getName());
                    viewService.removeView(config.getName());
                    viewService.addView(config.getName(), config.getPermissions(), config.isHideExplorerPanel(), config.getTemplate(), config.getTabList());
                  } else {
                    log.info("Ignore existing view: " + config.getName());
                  }
                } else {
                  log.info("Add new view: " + config.getName());
                  viewService.addView(config.getName(), config.getPermissions(), config.getTemplate(), config.getTabList());
                }
              }
            }
          } finally {
            zin.closeEntry();
          }
        }
      } finally {
        zin.close();
      }
      resultHandler.completed(NoResultModel.INSTANCE);
    } catch (Exception e) {
      throw new OperationException(OperationNames.IMPORT_RESOURCE, "Error while reading View Templates from Stream.", e);
    }
  }

  /**
   * Gets the templates home path.
   *
   * @return the templates home path
   */
  private String getTemplatesHomePath() {
    return nodeHierarchyCreator.getJcrPath(BasePath.ECM_EXPLORER_TEMPLATES);
  }

  /**
   * Extract template name.
   *
   * @param filePath the file path
   * @return the string
   */
  private String extractTemplateName(String filePath) {
    return filePath.replace("ecmadmin/view/templates/", "").replace(".gtmpl", "");
  }

}
