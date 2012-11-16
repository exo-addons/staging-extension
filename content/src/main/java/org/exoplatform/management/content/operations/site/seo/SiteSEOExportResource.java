package org.exoplatform.management.content.operations.site.seo;

import java.util.ArrayList;
import java.util.List;

import org.exoplatform.commons.utils.LazyPageList;
import org.exoplatform.portal.config.DataStorage;
import org.exoplatform.portal.config.Query;
import org.exoplatform.portal.config.model.Page;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.services.seo.PageMetadataModel;
import org.exoplatform.services.seo.SEOService;
import org.gatein.management.api.PathAddress;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.OperationHandler;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.ExportResourceModel;

/**
 * @author <a href="mailto:thomas.delhomenie@exoplatform.com">Thomas
 *         Delhoménie</a>
 * @version $Revision$
 */
public class SiteSEOExportResource implements OperationHandler {

  @Override
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws OperationException {
    try {
      String operationName = operationContext.getOperationName();
      PathAddress address = operationContext.getAddress();

      String siteName = address.resolvePathTemplate("site-name");
      if (siteName == null) {
        throw new OperationException(operationName, "No site name specified.");
      }

      resultHandler.completed(new ExportResourceModel(getSEOExportTask(operationContext, siteName)));
    } catch (Exception e) {
      throw new OperationException(OperationNames.EXPORT_RESOURCE, "Unable to retrieve the list of the contents sites : "
          + e.getMessage());
    }
  }

  @SuppressWarnings("unused")
  private SiteSEOExportTask getSEOExportTask(OperationContext operationContext, String siteName) throws Exception {
    DataStorage dataStorage = operationContext.getRuntimeContext().getRuntimeComponent(DataStorage.class);
    LazyPageList<Page> pagLazyList = dataStorage.find(new Query<Page>(SiteType.PORTAL.getName(), siteName, Page.class));
    SEOService seoService = operationContext.getRuntimeContext().getRuntimeComponent(SEOService.class);
    List<Page> pageList = pagLazyList.getAll();
    List<PageMetadataModel> pageMetadataModels = new ArrayList<PageMetadataModel>();
    for (Page page : pageList) {
      PageMetadataModel pageMetadataModel = null;// TODO ECMS-4030 ->
                                                 // seoService.getPageMetadata(page.getPageId());
      if (pageMetadataModel != null && pageMetadataModel.getKeywords() != null && !pageMetadataModel.getKeywords().isEmpty()) {
        pageMetadataModels.add(pageMetadataModel);
      }
    }

    return new SiteSEOExportTask(pageMetadataModels, siteName);
  }
}
