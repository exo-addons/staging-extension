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
package org.exoplatform.management.wiki.operations;

import org.exoplatform.management.common.AbstractOperationHandler;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.wiki.mow.api.WikiType;
import org.exoplatform.wiki.mow.core.api.MOWService;
import org.exoplatform.wiki.mow.core.api.wiki.WikiImpl;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.exceptions.ResourceNotFoundException;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.ReadResourceModel;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Created by The eXo Platform SAS Author : eXoPlatform exo@exoplatform.com Mar
 * 5, 2014
 */
public class WikiDataReadResource extends AbstractOperationHandler {

  /** The wiki type. */
  private WikiType wikiType;

  /**
   * Instantiates a new wiki data read resource.
   *
   * @param wikiType the wiki type
   */
  public WikiDataReadResource(WikiType wikiType) {
    this.wikiType = wikiType;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws ResourceNotFoundException, OperationException {
    spaceService = operationContext.getRuntimeContext().getRuntimeComponent(SpaceService.class);

    Set<String> children = new LinkedHashSet<String>();
    MOWService mowService = operationContext.getRuntimeContext().getRuntimeComponent(MOWService.class);
    for (WikiImpl wiki : mowService.getWikiStore().getWikiContainer(wikiType).getAllWikis()) {
      String wikiOwner = wiki.getOwner();
      if (wikiType.equals(WikiType.GROUP)) {
        Space space = spaceService.getSpaceByGroupId(wikiOwner);
        if (space != null) {
          wikiOwner = space.getDisplayName();
        }
      }
      children.add(wikiOwner);
    }
    resultHandler.completed(new ReadResourceModel("All wikis:", children));
  }
}
