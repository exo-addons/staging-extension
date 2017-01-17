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

package org.exoplatform.management.mop.operations.navigation;

import org.exoplatform.management.mop.operations.site.AbstractSiteOperationHandler;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.exceptions.ResourceNotFoundException;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.mop.api.workspace.Navigation;
import org.gatein.mop.api.workspace.Site;

/**
 * The Class AbstractNavigationOperationHandler.
 *
 * @author <a href="mailto:nscavell@redhat.com">Nick Scavelli</a>
 * @version $Revision$
 */
public abstract class AbstractNavigationOperationHandler extends AbstractSiteOperationHandler {
  
  /**
   * {@inheritDoc}
   */
  @Override
  protected void execute(OperationContext operationContext, ResultHandler resultHandler, Site site) throws ResourceNotFoundException, OperationException {
    Navigation navigation = site.getRootNavigation().getChild("default");
    if (navigation == null)
      throw new ResourceNotFoundException("Navigation does not exist for site " + getSiteKey(site));

    execute(operationContext, resultHandler, navigation);
  }

  /**
   * Execute.
   *
   * @param operationContext the operation context
   * @param resultHandler the result handler
   * @param defaultNavigation the default navigation
   */
  protected abstract void execute(OperationContext operationContext, ResultHandler resultHandler, Navigation defaultNavigation);
}
