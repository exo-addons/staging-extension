package org.exoplatform.management.ecmadmin.operations.templates.applications;

import java.io.IOException;
import java.io.OutputStream;

import org.gatein.management.api.operation.model.ExportTask;

import com.thoughtworks.xstream.XStream;

/**
 * @author <a href="mailto:boubaker.khanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class ApplicationTemplatesMetaDataExportTask implements ExportTask {

  private ApplicationTemplatesMetadata metaData = null;
  private String applicationName = null;

  public ApplicationTemplatesMetaDataExportTask(ApplicationTemplatesMetadata metaData, String applicationName) {
    this.metaData = metaData;
    this.applicationName = applicationName;
  }

  @Override
  public String getEntry() {
    return "templates/applications/" + applicationName + "/metadata.xml";
  }

  @Override
  public void export(OutputStream outputStream) throws IOException {
    XStream xStream = new XStream();
    xStream.alias("metadata", ApplicationTemplatesMetadata.class);
    String xmlContent = xStream.toXML(metaData);
    outputStream.write(xmlContent.getBytes());
  }
}