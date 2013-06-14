package org.exoplatform.management.ecmadmin.operations.templates.applications;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.jcr.Node;

import org.apache.commons.io.IOUtils;
import org.exoplatform.services.cms.views.ApplicationTemplateManagerService;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.model.ExportTask;

/**
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class ApplicationTemplateExportTask implements ExportTask {
  private final ApplicationTemplateManagerService applicationTemplateManagerService;
  private final String applicationName;
  private String templatePath;
  private final String templateCategory;
  private final String templateName;

  public ApplicationTemplateExportTask(ApplicationTemplateManagerService applicationTemplateManagerService,
      String applicationName, String templateCategory, String templateName) {
    this.applicationTemplateManagerService = applicationTemplateManagerService;
    this.applicationName = applicationName;
    this.templateCategory = templateCategory;
    this.templateName = templateName;
    templatePath = "";
    if (templateCategory != null && !templateCategory.isEmpty()) {
      templatePath += templateCategory + "/";
    }
    templatePath += templateName;
  }

  
  public String getEntry() {
    return "templates/applications/" + this.applicationName + "/" + templatePath;
  }

  
  public void export(OutputStream outputStream) throws IOException {
    InputStream templateFileIS = null;
    try {
      Node templateNode = applicationTemplateManagerService.getTemplateByName(this.applicationName, this.templateCategory,
          this.templateName, WCMCoreUtils.getSystemSessionProvider());

      templateFileIS = templateNode.getNode("jcr:content").getProperty("jcr:data").getStream();
      IOUtils.copy(templateFileIS, outputStream);
    } catch (Exception e) {
      throw new OperationException(OperationNames.EXPORT_RESOURCE, "Unable to export template " + this.applicationName + "/"
          + this.templatePath, e);
    } finally {
      if (templateFileIS != null) {
        templateFileIS.close();
      }
    }
  }

}
