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

import org.exoplatform.portal.config.model.PageNavigation;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.description.DescriptionService;
import org.exoplatform.portal.mop.navigation.NavigationService;
import org.gatein.management.api.exceptions.ResourceNotFoundException;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.mop.api.workspace.Navigation;
import org.gatein.mop.api.workspace.Site;

/**
 * The Class NavigationReadConfigAsXml.
 *
 * @author <a href="mailto:nscavell@redhat.com">Nick Scavelli</a>
 * @version $Revision$
 */
public class NavigationReadConfigAsXml extends AbstractNavigationOperationHandler {
  
  /**
   * {@inheritDoc}
   */
  @Override
  protected void execute(OperationContext operationContext, ResultHandler resultHandler, Navigation defaultNavigation) {
    String navUri = operationContext.getAddress().resolvePathTemplate("nav-uri");

    Site site = defaultNavigation.getSite();
    SiteKey siteKey = getSiteKey(site);

    // TODO: If there's any benefit in creating our own node model to use with
    // navigation service, lets do it

    DescriptionService descriptionService = operationContext.getRuntimeContext().getRuntimeComponent(DescriptionService.class);
    NavigationService navigationService = operationContext.getRuntimeContext().getRuntimeComponent(NavigationService.class);

    PageNavigation pageNavigation = NavigationUtils.loadPageNavigation(new NavigationKey(siteKey, navUri), navigationService, descriptionService);
    if (pageNavigation == null)
      throw new ResourceNotFoundException("Navigation node not found for navigation uri '" + navUri + "'");

    resultHandler.completed(pageNavigation);
  }
}
