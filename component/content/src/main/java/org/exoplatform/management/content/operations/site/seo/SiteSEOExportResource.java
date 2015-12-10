package org.exoplatform.management.content.operations.site.seo;

import static org.exoplatform.portal.mop.Utils.objectType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Session;

import org.exoplatform.management.common.exportop.AbstractExportOperationHandler;
import org.exoplatform.portal.config.DataStorage;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.pom.config.POMSession;
import org.exoplatform.portal.pom.config.POMSessionManager;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.resources.LocaleConfig;
import org.exoplatform.services.resources.LocaleConfigService;
import org.exoplatform.services.seo.PageMetadataModel;
import org.exoplatform.services.seo.impl.SEOServiceImpl;
import org.exoplatform.services.wcm.core.WCMConfigurationService;
import org.gatein.management.api.PathAddress;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.ExportResourceModel;
import org.gatein.management.api.operation.model.ExportTask;
import org.gatein.mop.api.workspace.Navigation;
import org.gatein.mop.api.workspace.ObjectType;
import org.gatein.mop.api.workspace.Site;
import org.gatein.mop.api.workspace.Workspace;

/**
 * @author <a href="mailto:thomas.delhomenie@exoplatform.com">Thomas
 *         Delhoménie</a>
 * @author <a href="mailto:boubaker.khanfir@exoplatform.com">Boubaker
 *         Khanfir</a>
 * @version $Revision$
 */
public class SiteSEOExportResource extends AbstractExportOperationHandler {

  private DataStorage dataStorage = null;
  private WCMConfigurationService wcmConfigurationService = null;
  private SessionProviderService sessionProviderService = null;
  private RepositoryService repositoryService = null;
  private POMSessionManager pomSessionManager = null;

  @Override
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws OperationException {
    try {
      String operationName = operationContext.getOperationName();
      PathAddress address = operationContext.getAddress();

      if (dataStorage == null) {
        dataStorage = operationContext.getRuntimeContext().getRuntimeComponent(DataStorage.class);
        wcmConfigurationService = operationContext.getRuntimeContext().getRuntimeComponent(WCMConfigurationService.class);
        sessionProviderService = operationContext.getRuntimeContext().getRuntimeComponent(SessionProviderService.class);
        repositoryService = operationContext.getRuntimeContext().getRuntimeComponent(RepositoryService.class);
        pomSessionManager = operationContext.getRuntimeContext().getRuntimeComponent(POMSessionManager.class);
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
          exportTasks.add(getSEOExportTask(operationContext, siteName, localeConfig.getLocale().toString()));
        }
      }

      resultHandler.completed(new ExportResourceModel(exportTasks));
    } catch (Exception e) {
      throw new OperationException(OperationNames.EXPORT_RESOURCE, "Unable to retrieve the list of the contents sites.", e);
    }
  }

  private SiteSEOExportTask getSEOExportTask(OperationContext operationContext, String siteName, String lang) throws Exception {
    // FIXME comment this until ECMS-4030 get fixed
    // SEOService seoService =
    // operationContext.getRuntimeContext().getRuntimeComponent(SEOService.class);
    List<PageMetadataModel> pageMetadataModels = new ArrayList<PageMetadataModel>();

    List<Navigation> navigations = getSiteNavigations(siteName);

    for (Navigation navigation : navigations) {
      PageMetadataModel pageMetadataModel = null;
      // FIXME use this once ECMS-4030 get fixed
      // pageMetadataModel = seoService.getPageMetadata(page.getPageId(),
      // lang);
      // System.out.println(page.getStorageId());
      pageMetadataModel = getPageMetadata(navigation.getObjectId(), lang);

      if (pageMetadataModel != null && pageMetadataModel.getKeywords() != null && !pageMetadataModel.getKeywords().isEmpty()) {
        pageMetadataModels.add(pageMetadataModel);
      }
    }
    return new SiteSEOExportTask(pageMetadataModels, siteName, lang);
  }

  private List<Navigation> getSiteNavigations(String siteName) {
    POMSession session = pomSessionManager.getSession();
    ObjectType<Site> objectType = objectType(SiteType.PORTAL);
    Workspace workspace = session.getWorkspace();
    Site site = workspace.getSite(objectType, siteName);
    Navigation rootNode = site.getRootNavigation();
    Navigation defaultNode = rootNode.getChild("default");
    List<Navigation> navigations = new ArrayList<Navigation>(defaultNode.getChildren());
    int i = 0;
    computeAllNavigations(navigations, i);
    return navigations;
  }

  private void computeAllNavigations(List<Navigation> navigations, int i) {
    if (i >= navigations.size()) {
      return;
    }
    Navigation navigation = navigations.get(i);
    if (navigation.getChildren() != null && !navigation.getChildren().isEmpty()) {
      navigations.addAll(navigation.getChildren());
    }
  }

  /**
   * {@inheritDoc}
   */
  public PageMetadataModel getPageMetadata(String pageUUID, String language) throws Exception {
    PageMetadataModel metaModel = null;
    if (metaModel == null) {
      SessionProvider sessionProvider = sessionProviderService.getSystemSessionProvider(null);
      Session session = sessionProvider.getSession("portal-system", repositoryService.getCurrentRepository());

      Node pageNode = session.getNodeByUUID(pageUUID);
      if (pageNode != null && pageNode.hasNode(SEOServiceImpl.LANGUAGES + "/" + language)) {
        Node seoNode = pageNode.getNode(SEOServiceImpl.LANGUAGES + "/" + language);
        if (seoNode.isNodeType("exo:pageMetadata")) {
          metaModel = new PageMetadataModel();
          if (seoNode.hasProperty("exo:metaTitle"))
            metaModel.setTitle((seoNode.getProperty("exo:metaTitle")).getString());
          if (seoNode.hasProperty("exo:metaKeywords"))
            metaModel.setKeywords((seoNode.getProperty("exo:metaKeywords")).getString());
          if (seoNode.hasProperty("exo:metaDescription"))
            metaModel.setDescription((seoNode.getProperty("exo:metaDescription")).getString());
          if (seoNode.hasProperty("exo:metaRobots"))
            metaModel.setRobotsContent((seoNode.getProperty("exo:metaRobots")).getString());
          if (seoNode.hasProperty("exo:metaSitemap"))
            metaModel.setSiteMap(Boolean.parseBoolean((seoNode.getProperty("exo:metaSitemap")).getString()));
          if (seoNode.hasProperty("exo:metaPriority"))
            metaModel.setPriority(Long.parseLong((seoNode.getProperty("exo:metaPriority")).getString()));
          if (seoNode.hasProperty("exo:metaFrequency"))
            metaModel.setFrequency((seoNode.getProperty("exo:metaFrequency")).getString());
          if (seoNode.hasProperty("exo:metaFully"))
            metaModel.setFullStatus((seoNode.getProperty("exo:metaFully")).getString());
        }
      }
    }
    return metaModel;
  }

}
