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
package org.exoplatform.management.mop.operations.page;

import org.exoplatform.portal.mop.page.PageKey;
import org.exoplatform.portal.mop.page.PageService;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.exceptions.ResourceNotFoundException;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.ReadResourceModel;
import org.gatein.mop.api.workspace.Page;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * The Class PageReadResource.
 *
 * @author <a href="mailto:nscavell@redhat.com">Nick Scavelli</a>
 * @version $Revision$
 */
public class PageReadResource extends AbstractPageOperationHandler {
  
  /**
   * {@inheritDoc}
   */
  @Override
  protected void execute(OperationContext operationContext, ResultHandler resultHandler, Page rootPage) throws ResourceNotFoundException, OperationException {
    String pageName = operationContext.getAddress().resolvePathTemplate("page-name");
    if (pageName == null) {
      Collection<Page> pageList = rootPage.getChildren();
      Set<String> children = new LinkedHashSet<String>(pageList.size());
      for (Page page : pageList) {
        children.add(page.getName());
      }

      resultHandler.completed(new ReadResourceModel("List of all available pages for site '" + rootPage.getSite().getName() + "'", children));
    } else {
      PageService pageService = operationContext.getRuntimeContext().getRuntimeComponent(PageService.class);
      PageKey pageKey = new PageKey(getSiteKey(rootPage.getSite()), pageName);

      if (pageService.loadPage(pageKey) == null) {
        throw new ResourceNotFoundException("No page found for " + pageKey);
      }

      resultHandler.completed(new ReadResourceModel("List of child pages for page '" + pageName + "'", Collections.<String> emptySet()));
    }
  }
}
