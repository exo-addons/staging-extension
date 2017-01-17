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
package org.exoplatform.management.social.operations;

import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.management.common.AbstractOperationHandler;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.exceptions.ResourceNotFoundException;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.ReadResourceModel;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * The Class SocialDataReadResource.
 *
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class SocialDataReadResource extends AbstractOperationHandler {

  /**
   * {@inheritDoc}
   */
  @Override
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws ResourceNotFoundException, OperationException {
    Set<String> children = new LinkedHashSet<String>();
    SpaceService spaceService = operationContext.getRuntimeContext().getRuntimeComponent(SpaceService.class);

    if (spaceService != null) {
      try {
        ListAccess<Space> spaces = spaceService.getAllSpacesWithListAccess();
        if (spaces != null && spaces.getSize() > 0) {
          Space[] spacesArr = spaces.load(0, spaces.getSize());
          for (Space space : spacesArr) {
            children.add(space.getDisplayName());
          }
        }
      } catch (Exception e) {
        throw new OperationException(OperationNames.READ_RESOURCE, "Error while listing all spaces", e);
      }
    }

    resultHandler.completed(new ReadResourceModel("All spaces:", children));
  }
}
