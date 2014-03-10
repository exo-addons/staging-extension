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
package org.exoplatform.management.wiki.operations;

import java.io.IOException;
import java.io.OutputStream;

import javax.jcr.Node;

import org.exoplatform.wiki.mow.api.Wiki;
import org.exoplatform.wiki.mow.api.WikiType;
import org.exoplatform.wiki.mow.core.api.MOWService;
import org.exoplatform.wiki.mow.core.api.wiki.WikiContainer;
import org.gatein.management.api.operation.model.ExportTask;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Mar 5, 2014  
 */
public class WikiExportTask implements ExportTask {

  private String wikiName;
  private WikiType wikiType;
  private MOWService mowService;

  public WikiExportTask(MOWService mowService, WikiType wikiType, String wikiName) {
    this.mowService = mowService;
    this.wikiName = wikiName;
    this.wikiType = wikiType;
  }

  @Override
  public String getEntry() {
    return new StringBuilder("wiki/").append(wikiType.toString().toLowerCase())
                                     .append("/").append(wikiName).append(".xml").toString();
  }

  @Override
  public void export(OutputStream outputStream) throws IOException {
    try {
      WikiContainer<Wiki> wikiContainer = mowService.getModel().getWikiStore().getWikiContainer(wikiType);
      Wiki wiki = wikiContainer.getWiki(this.wikiName, true);
      Node wikiNode = wiki.getWikiHome().getJCRPageNode();
      wikiNode.getSession().exportSystemView(wikiNode.getPath(), outputStream, false, false);
    } catch (Exception exception) {
      throw new RuntimeException(exception);
    }
  }
}
