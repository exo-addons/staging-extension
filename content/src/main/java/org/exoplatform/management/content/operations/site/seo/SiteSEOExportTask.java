package org.exoplatform.management.content.operations.site.seo;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import org.exoplatform.management.content.operations.site.SiteUtil;
import org.exoplatform.services.seo.PageMetadataModel;
import org.gatein.management.api.operation.model.ExportTask;

import com.thoughtworks.xstream.XStream;

/**
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class SiteSEOExportTask implements ExportTask {
  public static final String FILENAME = "seo.xml";

  private final List<PageMetadataModel> models;
  private final String siteName;
  private final String lang;

  public SiteSEOExportTask(List<PageMetadataModel> models, String siteName, String lang) {
    this.models = models;
    this.siteName = siteName;
    this.lang = lang;
  }

  @Override
  public String getEntry() {
    return SiteUtil.getSiteBasePath(siteName) + "/" + lang + "_" + FILENAME;
  }

  @Override
  public void export(OutputStream outputStream) throws IOException {
    XStream xStream = new XStream();
    xStream.alias("seo", List.class);
    String xmlContent = xStream.toXML(models);
    outputStream.write(xmlContent.getBytes());
  }
}