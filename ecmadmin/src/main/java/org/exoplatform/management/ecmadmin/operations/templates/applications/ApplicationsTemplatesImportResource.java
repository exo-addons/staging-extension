package org.exoplatform.management.ecmadmin.operations.templates.applications;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.jcr.Node;

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

/**
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class ApplicationsTemplatesImportResource extends ECMAdminImportResource {
  final private static Logger log = LoggerFactory.getLogger(ApplicationsTemplatesImportResource.class);
  private ApplicationTemplateManagerService applicationTemplateManagerService;
  private TemplateService templateService;

  private Pattern templateEntryPattern = Pattern.compile("templates/applications/(.*)/(.*)/(.*)\\.gtmpl");

  public ApplicationsTemplatesImportResource() {
    super(null);
  }

  public ApplicationsTemplatesImportResource(String filePath) {
    super(filePath);
  }

  @Override
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws OperationException {
    // get attributes and attachement inputstream
    super.execute(operationContext, resultHandler);

    if (applicationTemplateManagerService == null) {
      applicationTemplateManagerService = operationContext.getRuntimeContext().getRuntimeComponent(
          ApplicationTemplateManagerService.class);
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
      final ZipInputStream zis = new ZipInputStream(attachmentInputStream);
      ZipEntry entry;
      while ((entry = zis.getNextEntry()) != null) {
        String filePath = entry.getName();
        if (!filePath.startsWith("templates/applications/")) {
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

        Node category = applicationTemplateManagerService.getApplicationTemplateHome(applicationName,
            SessionProvider.createSystemProvider()).getNode(categoryName);
        if (category.hasNode(templateName)) {
          if (replaceExisting) {
            log.info("Overwrite existing application template '" + applicationName + "/" + categoryName + "/" + templateName
                + "'.");
            Node templateNode = category.getNode(templateName);
            templateNode.remove();
            category.getSession().save();
          } else {
            log.info("Ignore existing application template '" + applicationName + "/" + categoryName + "/" + templateName + "'.");
            continue;
          }
        }
        templateService.createTemplate(category, templateName, templateName, zis, new String[] { "*" });
        zis.closeEntry();
      }
      zis.close();

    } catch (Exception e) {
      throw new OperationException(OperationNames.IMPORT_RESOURCE, "Error while importing applications templates", e);
    }

    resultHandler.completed(NoResultModel.INSTANCE);
  }

}