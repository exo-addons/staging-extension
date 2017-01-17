/*
 * Copyright (C) 2003-2017 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.exoplatform.management.service.handler;

import org.exoplatform.management.service.api.ResourceHandler;

import java.util.HashMap;
import java.util.Map;

/**
 * Registry for resources handlers.
 *
 * @author Thomas Delhoménie
 */
public class ResourceHandlerRegistry {
  
  /** The resource handlers. */
  private Map<String, ResourceHandler> resourceHandlers;

  /**
   * Instantiates a new resource handler registry.
   */
  public ResourceHandlerRegistry() {
    resourceHandlers = new HashMap<String, ResourceHandler>();
  }

  /**
   * Gets the.
   *
   * @param name the name
   * @return the resource handler
   */
  public ResourceHandler get(String name) {
    return resourceHandlers.get(name);
  }

  /**
   * Register.
   *
   * @param newResourceHandler the new resource handler
   */
  public void register(ResourceHandler newResourceHandler) {
    resourceHandlers.put(newResourceHandler.getPath(), newResourceHandler);
  }
}
