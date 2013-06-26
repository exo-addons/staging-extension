package org.exoplatform.extension.generator.service.handler;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipOutputStream;

import org.exoplatform.extension.generator.service.api.ConfigurationHandler;
import org.exoplatform.extension.generator.service.api.Utils;

public class WebXMLConfigurationHandler implements ConfigurationHandler {
  private static final String WEB_XML_LOCATION = "WEB-INF/web.xml";
  private static final List<String> configurationPaths = new ArrayList<String>();
  private static String WEB_XML_CONTENT;
  static {
    StringBuilder builder = new StringBuilder();
    builder.append("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>");
    builder.append("<web-app");
    builder.append("      version=\"3.0\"");
    builder.append("      metadata-complete=\"true\"");
    builder.append("      xmlns=\"http://java.sun.com/xml/ns/javaee\"");
    builder.append("      xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
    builder.append("      xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/web-app_3_0.xsd\">");
    builder.append("  <display-name>commons-extension</display-name>");
    builder.append("  <listener>");
    builder.append("    <listener-class>org.exoplatform.container.web.PortalContainerConfigOwner</listener-class>");
    builder.append("  </listener>");
    builder.append("</web-app>");
    WEB_XML_CONTENT = builder.toString();
  }

  @Override
  public boolean writeData(ZipOutputStream zos, Set<String> selectedResources) {
    Utils.writeZipEnry(zos, WEB_XML_LOCATION, WEB_XML_CONTENT);
    return false;
  }

  @Override
  public List<String> getConfigurationPaths() {
    return configurationPaths;
  }
  
}
