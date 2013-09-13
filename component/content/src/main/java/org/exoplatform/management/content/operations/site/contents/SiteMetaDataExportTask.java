package org.exoplatform.management.content.operations.site.contents;

import java.io.IOException;
import java.io.OutputStream;

import org.exoplatform.management.content.operations.site.SiteUtil;
import org.gatein.management.api.operation.model.ExportTask;

import com.thoughtworks.xstream.XStream;

/**
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class SiteMetaDataExportTask implements ExportTask {

  public static final String FILENAME = "metadata.xml";

  private SiteMetaData metaData = null;

  public SiteMetaDataExportTask(SiteMetaData metaData) {
    this.metaData = metaData;
  }

  @Override
  public String getEntry() {
    return SiteUtil.getSiteContentsBasePath(metaData.getOptions().get(SiteMetaData.SITE_NAME)) + "/" + FILENAME;
  }

  @Override
  public void export(OutputStream outputStream) throws IOException {
    XStream xStream = new XStream();
    xStream.alias("metadata", SiteMetaData.class);
    String xmlContent = xStream.toXML(metaData);
    outputStream.write(xmlContent.getBytes());
  }
}