package org.exoplatform.extension.generator.service.api;

import java.util.List;
import java.util.Set;
import java.util.zip.ZipOutputStream;

public interface ConfigurationHandler {


  /**
   * Writes XML files corresponding to the set of selected managed resources in Archive.
   * 
   * @param zos Generated WAR output stream
   * @param selectedResources Set of selected managed resources path
   * @return true if some files was written in archive
   */
  public abstract boolean writeData(ZipOutputStream zos, Set<String> selectedResources);

  /**
   * @return list of configuration paths written in archive
   */
  public abstract List<String> getConfigurationPaths();

}
