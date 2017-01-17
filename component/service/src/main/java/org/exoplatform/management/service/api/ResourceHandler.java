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
package org.exoplatform.management.service.api;

import java.util.List;
import java.util.Map;
import java.util.zip.ZipOutputStream;

/**
 * The Interface ResourceHandler.
 */
public interface ResourceHandler {

  /**
   * Gets the path.
   *
   * @return paren path resource
   */
  public String getPath();

  /**
   * Synchronise selected resources with host identified by host and port, by
   * using the selected options.
   *
   * @param resourcesPaths the resources paths
   * @param exportOptions the export options
   * @param importOptions the import options
   * @param targetServer the target server
   * @throws Exception the exception
   */
  public abstract void synchronize(List<Resource> resourcesPaths, Map<String, String> exportOptions, Map<String, String> importOptions, TargetServer targetServer) throws Exception;

  /**
   * Export selected resources with selected options as filter.
   * 
   * @param list
   *          : List of resources to export
   * @param exportFileOS
   *          : Write in this zip file the entries
   * @param exportOptions
   *          export options
   * 
   * @throws Exception
   *           if an error occurs
   */
  public void exportResourcesInFilter(List<Resource> list, ZipOutputStream exportFileOS, Map<String, String> exportOptions) throws Exception;

  /**
   * Synchronise selected resources as filter with host identified by host and
   * port, by using the selected options.
   *
   * @param resourcesPaths the resources paths
   * @param exportOptions the export options
   * @param importOptions the import options
   * @param targetServer the target server
   * @throws Exception the exception
   */
  public abstract void synchronizeResourcesInFilter(List<Resource> resourcesPaths, Map<String, String> exportOptions, Map<String, String> importOptions, TargetServer targetServer) throws Exception;

  /**
   * Export selected resources with selected options.
   * 
   * @param list
   *          : List of resources to export
   * @param exportFileOS
   *          : Write in this zip file the entries
   * @param exportOptions
   *          export options
   * 
   * @throws Exception
   *           if an error occurs
   */
  public void export(List<Resource> list, ZipOutputStream exportFileOS, Map<String, String> exportOptions) throws Exception;
}