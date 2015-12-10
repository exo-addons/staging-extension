package org.exoplatform.management.mop.operations.page;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.exoplatform.portal.mop.page.PageKey;
import org.exoplatform.portal.mop.page.PageService;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.exceptions.ResourceNotFoundException;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.ReadResourceModel;
import org.gatein.mop.api.workspace.Page;

/**
 * @author <a href="mailto:nscavell@redhat.com">Nick Scavelli</a>
 * @version $Revision$
 */
public class PageReadResource extends AbstractPageOperationHandler {
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
