package org.exoplatform.management.ecmadmin.operations.templates.nodetypes;

import java.io.IOException;
import java.io.OutputStream;

import org.exoplatform.management.ecmadmin.operations.templates.NodeTemplate;
import org.gatein.management.api.operation.model.ExportTask;

import com.thoughtworks.xstream.XStream;

/**
 * @author <a href="mailto:thomas.delhomenie@exoplatform.com">Thomas
 *         Delhoménie</a>
 * @version $Revision$
 */
public class NodeTypeTemplatesMetaDataExportTask implements ExportTask {

  private NodeTypeTemplatesMetaData metaData = null;
  private String path = null;

  public NodeTypeTemplatesMetaDataExportTask(NodeTypeTemplatesMetaData metaData, String path) {
    this.metaData = metaData;
    this.path = path;
  }

  
  public String getEntry() {
    return path + "/metadata.xml";
  }

  
  public void export(OutputStream outputStream) throws IOException {
    XStream xStream = new XStream();
    xStream.alias("metadata", NodeTypeTemplatesMetaData.class);
    xStream.alias("template", NodeTemplate.class);
    String xmlContent = xStream.toXML(metaData);
    outputStream.write(xmlContent.getBytes());
  }
}