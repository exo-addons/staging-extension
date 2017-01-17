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

/**
 * The Interface SynchronizationService.
 */
public interface SynchronizationService {

  /**
   * Gets the synchonization servers.
   *
   * @return the synchonization servers
   */
  public List<TargetServer> getSynchonizationServers();

  /**
   * Adds the synchonization server.
   *
   * @param targetServer the target server
   */
  public void addSynchonizationServer(TargetServer targetServer);

  /**
   * Removes the synchonization server.
   *
   * @param targetServer the target server
   */
  public void removeSynchonizationServer(TargetServer targetServer);

  /**
   * Synchronize Managed Resources.
   *
   * @param selectedResourcesCategories the selected resources categories
   * @param targetServer the target server
   * @throws Exception the exception
   */
  void synchronize(List<ResourceCategory> selectedResourcesCategories, TargetServer targetServer) throws Exception;

  /**
   * Test Server connection.
   *
   * @param targetServer : the server connection details (host, port, username, password).
   * @throws Exception the exception
   */
  void testServerConnection(TargetServer targetServer) throws Exception;

}
