package org.exoplatform.management.ecmadmin.operations.view;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.commons.io.IOUtils;
import org.exoplatform.ecm.webui.utils.Utils;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.model.ExportTask;

/**
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class ViewTemplateExportTask implements ExportTask {
  private final Node templateNode;

  public ViewTemplateExportTask(Node templateNode) {
    this.templateNode = templateNode;
  }

  @Override
  public String getEntry() {
    try {
      return "view/templates/" + this.templateNode.getName() + ".gtmpl";
    } catch (RepositoryException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void export(OutputStream outputStream) throws IOException {
    InputStream templateStream = null;
    try {
      Node contentNode = templateNode.getNode(Utils.JCR_CONTENT);
      templateStream = contentNode.getProperty("jcr:data").getStream();
      IOUtils.copy(templateStream, outputStream);
    } catch (Exception e) {
      throw new OperationException(OperationNames.EXPORT_RESOURCE, "Unable to export view template .", e);
    } finally {
      if (templateStream != null) {
        templateStream.close();
      }
    }
  }

}