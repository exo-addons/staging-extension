package org.exoplatform.management.ecmadmin.exporttask;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.jcr.Node;

import org.apache.commons.io.IOUtils;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.model.ExportTask;

/**
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class NodeFileExportTask implements ExportTask {
  private final Node node;
  private final String exportPath;

  public NodeFileExportTask(Node node, String exportPath) {
    this.node = node;
    this.exportPath = exportPath;
  }

  @Override
  public String getEntry() {
    return exportPath;
  }

  @Override
  public void export(OutputStream outputStream) throws IOException {
    InputStream nodeFileIS = null;
    try {
      nodeFileIS = node.getNode("jcr:content").getProperty("jcr:data").getStream();
      IOUtils.copy(nodeFileIS, outputStream);
    } catch (Exception e) {
      throw new OperationException(OperationNames.EXPORT_RESOURCE, "Unable to export file of node " + exportPath, e);
    } finally {
      if (nodeFileIS != null) {
        nodeFileIS.close();
      }
    }
  }

}
