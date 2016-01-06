package org.exoplatform.management.ecmadmin.operations.templates.applications;

import java.io.InputStreamReader;
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

import com.thoughtworks.xstream.XStream;

/**
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class ApplicationsTemplatesImportResource extends ECMAdminImportResource {
  final private static Logger log = LoggerFactory.getLogger(ApplicationsTemplatesImportResource.class);
  final private static Pattern templateEntryPattern = Pattern.compile("templates/applications/(.*)/(.*)/(.*)\\.gtmpl");

  private ApplicationTemplateManagerService applicationTemplateManagerService;
  private TemplateService templateService;

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