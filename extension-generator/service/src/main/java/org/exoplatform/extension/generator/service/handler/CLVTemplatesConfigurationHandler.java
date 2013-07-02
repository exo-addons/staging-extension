package org.exoplatform.extension.generator.service.handler;

import java.util.ArrayList;
import java.util.List;

import org.exoplatform.extension.generator.service.api.ExtensionGenerator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

public class CLVTemplatesConfigurationHandler extends ApplicationTemplatesConfigurationHandler {
  private static final String APPLICATION_CLV_CONFIGURATION_LOCATION = DMS_CONFIGURATION_LOCATION + "templates/applications/content-list-viewer";
  private static final String APPLICATION_CLV_CONFIGURATION_NAME = "application-clv-templates-configuration.xml";
  private static final List<String> configurationPaths = new ArrayList<String>();
  static {
    configurationPaths.add(DMS_CONFIGURATION_LOCATION.replace("WEB-INF", "war:") + APPLICATION_CLV_CONFIGURATION_NAME);
  }

  public CLVTemplatesConfigurationHandler() {
    super(APPLICATION_CLV_CONFIGURATION_LOCATION, APPLICATION_CLV_CONFIGURATION_NAME, ExtensionGenerator.ECM_TEMPLATES_APPLICATION_CLV_PATH, "content-list-viewer");
  }

  private Log log = ExoLogger.getLogger(this.getClass());

  /**
   * {@inheritDoc}
   */
  @Override
  protected Log getLogger() {
    return log;
  }

  @Override
  public List<String> getConfigurationPaths() {
    return configurationPaths;
  }
}
