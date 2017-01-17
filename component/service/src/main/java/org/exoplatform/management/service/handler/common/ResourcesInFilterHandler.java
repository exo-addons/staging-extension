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
package org.exoplatform.management.service.handler.common;

import org.exoplatform.management.service.api.AbstractResourceHandler;
import org.exoplatform.management.service.api.Resource;
import org.exoplatform.management.service.api.TargetServer;

import java.util.List;
import java.util.Map;
import java.util.zip.ZipOutputStream;

/**
 * The Class ResourcesInFilterHandler.
 */
public class ResourcesInFilterHandler extends AbstractResourceHandler {

  /** The path. */
  private String path;

  /**
   * Instantiates a new resources in filter handler.
   *
   * @param path the path
   */
  public ResourcesInFilterHandler(String path) {
    this.path = path;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getPath() {
    return path;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void synchronize(List<Resource> resources, Map<String, String> exportOptions, Map<String, String> importOptions, TargetServer targetServer) throws Exception {
    super.synchronizeResourcesInFilter(resources, exportOptions, importOptions, targetServer);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void export(List<Resource> resources, ZipOutputStream exportFileOS, Map<String, String> exportOptions) throws Exception {
    super.exportResourcesInFilter(resources, exportFileOS, exportOptions);
  }

}
