/*
 * Copyright (C) 2003-2014 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.management.service.handler.wiki;

import java.util.List;
import java.util.Map;
import java.util.zip.ZipOutputStream;

import org.exoplatform.management.service.api.AbstractResourceHandler;
import org.exoplatform.management.service.api.Resource;
import org.exoplatform.management.service.api.TargetServer;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Mar 5, 2014  
 */
public class WikiHandler extends AbstractResourceHandler {

  private String wikiPath;
  
  public WikiHandler(String path) {
    this.wikiPath = path;
  }
  
  @Override
  public String getPath() {
    return wikiPath;
  }

  @Override
  public void synchronize(List<Resource> resources, Map<String, String> exportOptions, Map<String, String> importOptions, TargetServer targetServer) throws Exception {
    super.synchronizeResourcesInFilter(resources, exportOptions, importOptions, targetServer);
  }

  @Override
  public void export(List<Resource> resources, ZipOutputStream exportFileOS, Map<String, String> exportOptions) throws Exception {
    super.exportResourcesInFilter(resources, exportFileOS, exportOptions);
  }

}
