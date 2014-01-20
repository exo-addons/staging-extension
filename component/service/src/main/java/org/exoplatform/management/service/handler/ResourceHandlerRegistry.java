package org.exoplatform.management.service.handler;

import org.exoplatform.management.service.api.ResourceHandler;

import java.util.HashMap;
import java.util.Map;

/**
 * Registry for resources handlers
 *
 * @author Thomas Delhoménie
 */
public class ResourceHandlerRegistry {
  private Map<String, ResourceHandler> resourceHandlers;

  public ResourceHandlerRegistry() {
    resourceHandlers = new HashMap<String, ResourceHandler>();
  }

  public ResourceHandler get(String name) {
    return resourceHandlers.get(name);
  }

  public void register(ResourceHandler newResourceHandler) {
    resourceHandlers.put(newResourceHandler.getPath(), newResourceHandler);
  }
}
