package org.exoplatform.extension.synchronization.portlet;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

public class Settings implements Serializable {
  private static final long serialVersionUID = -399728419640377185L;

  private Set<String> resources = Collections.synchronizedSet(new HashSet<String>());
  private Map<String, String> options = new Hashtable<String, String>();
  
  public Set<String> getResources() {
    return resources;
  }

  public void setResources(Set<String> resources) {
    this.resources = resources;
  }

  public Map<String, String> getOptions() {
    return options;
  }

  public void setOptions(Map<String, String> options) {
    this.options = options;
  }

  public void clear() {
    resources.clear();
    options.clear();
  }

}
