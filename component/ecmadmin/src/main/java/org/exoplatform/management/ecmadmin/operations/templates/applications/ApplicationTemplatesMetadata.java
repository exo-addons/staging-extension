package org.exoplatform.management.ecmadmin.operations.templates.applications;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class ApplicationTemplatesMetadata {

  /**
   * Metadata of Application Templates: Map of (Path,Title)
   */
  private Map<String, String> titleMap;

  public ApplicationTemplatesMetadata() {
    this.titleMap = new HashMap<String, String>();
  }

  public Map<String, String> getTitleMap() {
    return titleMap;
  }

  public void addTitle(String path, String title) {
    titleMap.put(path, title);
  }
  
  public String getTitle(String path) {
    return titleMap.get(path);
  }

}
