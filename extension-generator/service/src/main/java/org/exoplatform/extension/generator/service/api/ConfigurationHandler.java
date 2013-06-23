package org.exoplatform.extension.generator.service.api;

import java.util.List;
import java.util.Set;
import java.util.zip.ZipOutputStream;

public interface ConfigurationHandler {

  public abstract boolean writeData(ZipOutputStream zos, Set<String> selectedResources);

  public abstract List<String> getConfigurationPaths();

}
