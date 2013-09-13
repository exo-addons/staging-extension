package org.exoplatform.management.content.operations.site.seo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.exoplatform.portal.config.DataStorage;
import org.exoplatform.portal.config.model.Page;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.mop.page.PageContext;
import org.exoplatform.portal.mop.page.PageService;
import org.exoplatform.services.resources.LocaleConfig;
import org.exoplatform.services.resources.LocaleConfigService;
import org.exoplatform.services.seo.PageMetadataModel;
import org.exoplatform.services.seo.SEOService;
import org.exoplatform.services.wcm.core.WCMConfigurationService;
import org.gatein.management.api.PathAddress;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.OperationHandler;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.ExportResourceModel;
import org.gatein.management.api.operation.model.ExportTask;

/**
 * @author <a href="mailto:thomas.delhomenie@exoplatform.com">Thomas
 *         Delhoménie</a>
 * @author <a href="mailto:boubaker.khanfir@exoplatform.com">Boubaker
 *         Khanfir</a>
 * @version $Revision$
 */
public class SiteSEOExportResource implements OperationHandler {

  private DataStorage dataStorage = null;
  private PageService pageService = null;
  private WCMConfigurationService wcmConfigurationService = null;

  @Override
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws OperationException {
    try {
      String operationName = operationContext.getOperationName();
      PathAddress address = operationContext.getAddress();

      if (dataStorage == null) {
        dataStorage = operationContext.getRuntimeContext().getRuntimeComponent(DataStorage.class);
        pageService = operationContext.getRuntimeContext().getRuntimeComponent(PageService.class);
        wcmConfigurationService = operationContext.getRuntimeContext().getRuntimeComponent(WCMConfigurationService.class);
      }
      String siteName = address.resolvePathTemplate("site-name");
      if (siteName == null) {
        throw new OperationException(operationName, "No site name specified.");
      }

      List<ExportTask> exportTasks = new ArrayList<ExportTask>();
      if (!wcmConfigurationService.getSharedPortalName().equals(siteName)) {
        LocaleConfigService localeConfigService = operationContext.getRuntimeContext().getRuntimeComponent(LocaleConfigService.class);
        Collection<LocaleConfig> localeConfigs = localeConfigService.getLocalConfigs();
        for (LocaleConfig localeConfig : localeConfigs) {
          exportTasks.add(getSEOExportTask(operationContext, siteName, localeConfig.getLanguage()));
        }
      }

      resultHandler.completed(new ExportResourceModel(exportTasks));
    } catch (Exception e) {
      throw new OperationException(OperationNames.EXPORT_RESOURCE, "Unable to retrieve the list of the contents sites.", e);
    }
  }

  private SiteSEOExportTask getSEOExportTask(OperationContext operationContext, String siteName, String lang) throws Exception {
    DataStorage dataStorage = operationContext.getRuntimeContext().getRuntimeComponent(DataStorage.class);
    SEOService seoService = operationContext.getRuntimeContext().getRuntimeComponent(SEOService.class);
    List<PageMetadataModel> pageMetadataModels = new ArrayList<PageMetadataModel>();

    // pages
    Iterator<PageContext> pagesQueryResult = pageService.findPages(0, Integer.MAX_VALUE, SiteType.PORTAL, siteName, null, null).iterator();
    while (pagesQueryResult.hasNext()) {
      PageContext pageContext = (PageContext) pagesQueryResult.next();
      Page page = dataStorage.getPage(pageContext.getKey().format());

      PageMetadataModel pageMetadataModel = null;
      try {
        pageMetadataModel = seoService.getPageMetadata(page.getPageId(), lang);
      } catch (Exception e) {
        // TODO: Bug ECMS-4030
      }
      if (pageMetadataModel != null && pageMetadataModel.getKeywords() != null && !pageMetadataModel.getKeywords().isEmpty()) {
        pageMetadataModels.add(pageMetadataModel);
      }
    }

    return new SiteSEOExportTask(pageMetadataModels, siteName, lang);
  }
}
