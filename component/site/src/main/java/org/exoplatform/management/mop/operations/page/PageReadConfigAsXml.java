/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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

import org.exoplatform.portal.config.DataStorage;
import org.exoplatform.portal.config.model.Page;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.page.PageKey;
import org.exoplatform.portal.mop.page.PageService;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.exceptions.ResourceNotFoundException;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.ResultHandler;

/**
 * @author <a href="mailto:nscavell@redhat.com">Nick Scavelli</a>
 * @version $Revision$
 */
public class PageReadConfigAsXml extends AbstractPageOperationHandler {
  @Override
  protected void execute(OperationContext operationContext, ResultHandler resultHandler, org.gatein.mop.api.workspace.Page rootPage) throws ResourceNotFoundException, OperationException {
    String operationName = operationContext.getOperationName();
    SiteKey siteKey = getSiteKey(rootPage.getSite());
    DataStorage dataStorage = operationContext.getRuntimeContext().getRuntimeComponent(DataStorage.class);
    PageService pageService = operationContext.getRuntimeContext().getRuntimeComponent(PageService.class);

    String pageName = operationContext.getAddress().resolvePathTemplate("page-name");

    // retrieve pages
    if (pageName == null) {
      try {
        resultHandler.completed(PageUtils.getAllPages(dataStorage, pageService, siteKey));
      } catch (Exception e) {
        throw new OperationException(operationName, "Could not retrieve pages for site " + siteKey);
      }
    } else {
      PageKey key = new PageKey(siteKey, pageName);
      try {
        Page page = PageUtils.getPage(dataStorage, pageService, key);
        if (page == null) {
          throw new ResourceNotFoundException("No page found for " + key);
        } else {
          resultHandler.completed(page);
        }
      } catch (ResourceNotFoundException e) {
        throw e;
      } catch (OperationException e) {
        throw e;
      } catch (Exception e) {
        throw new OperationException(operationName, "Operation failed getting page for " + key, e);
      }
    }
  }
}
