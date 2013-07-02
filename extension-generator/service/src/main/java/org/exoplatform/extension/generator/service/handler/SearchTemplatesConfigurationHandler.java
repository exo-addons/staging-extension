package org.exoplatform.extension.generator.service.handler;

import java.util.ArrayList;
import java.util.List;

import org.exoplatform.extension.generator.service.api.ExtensionGenerator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

public class SearchTemplatesConfigurationHandler extends ApplicationTemplatesConfigurationHandler {
  private static final String APPLICATION_SEARCH_CONFIGURATION_LOCATION = DMS_CONFIGURATION_LOCATION + "templates/applications/search";
  private static final String APPLICATION_SEARCH_CONFIGURATION_NAME = "application-search-templates-configuration.xml";
  private static final List<String> configurationPaths = new ArrayList<String>();
  static {
    configurationPaths.add(DMS_CONFIGURATION_LOCATION.replace("WEB-INF", "war:") + APPLICATION_SEARCH_CONFIGURATION_NAME);
  }

  private Log log = ExoLogger.getLogger(this.getClass());

  public SearchTemplatesConfigurationHandler() {
    super(APPLICATION_SEARCH_CONFIGURATION_LOCATION, APPLICATION_SEARCH_CONFIGURATION_NAME, ExtensionGenerator.ECM_TEMPLATES_APPLICATION_SEARCH_PATH, "search");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<String> getConfigurationPaths() {
    return configurationPaths;
  }

  @Override
  protected Log getLogger() {
    return log;
  }
}
