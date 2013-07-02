package org.exoplatform.management.ecmadmin.operations.templates.applications;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.jcr.Node;
import javax.jcr.Value;

import org.apache.commons.io.IOUtils;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.services.cms.views.ApplicationTemplateManagerService;
import org.exoplatform.services.wcm.core.NodetypeConstant;
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
  private ApplicationTemplatesMetadata metadata;

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

  @Override
  public String getEntry() {
    return "templates/applications/" + this.applicationName + "/" + templatePath;
  }

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
