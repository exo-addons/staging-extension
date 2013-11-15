package org.exoplatform.management.ecmadmin.operations.templates.metadata;

import com.thoughtworks.xstream.XStream;
import org.exoplatform.management.ecmadmin.operations.templates.NodeTemplate;
import org.gatein.management.api.operation.model.ExportTask;

import java.io.IOException;
import java.io.OutputStream;

/**
 * @author <a href="mailto:thomas.delhomenie@exoplatform.com">Thomas
 *         Delhoménie</a>
 * @version $Revision$
 */
public class MetadataTemplatesMetaDataExportTask implements ExportTask {

  private MetadataTemplatesMetaData metaData = null;
  private String path = null;

  public MetadataTemplatesMetaDataExportTask(MetadataTemplatesMetaData metaData, String path) {
    this.metaData = metaData;
    this.path = path;
  }

  @Override
  public String getEntry() {
    return path + "/metadata.xml";
  }

  @Override
  public void export(OutputStream outputStream) throws IOException {
    XStream xStream = new XStream();
    xStream.alias("metadata", MetadataTemplatesMetaData.class);
    xStream.alias("template", NodeTemplate.class);
    String xmlContent = xStream.toXML(metaData);
    outputStream.write(xmlContent.getBytes());
  }
}