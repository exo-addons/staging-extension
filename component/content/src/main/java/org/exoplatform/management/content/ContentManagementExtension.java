package org.exoplatform.management.content;

import org.exoplatform.management.common.AbstractManagementExtension;
import org.exoplatform.management.common.DataTransformerService;
import org.exoplatform.management.content.operations.ContentReadResource;
import org.exoplatform.management.content.operations.site.LiveSitesReadResource;
import org.exoplatform.management.content.operations.site.PageCLVTransformer;
import org.exoplatform.management.content.operations.site.PageSCVTransformer;
import org.exoplatform.management.content.operations.site.SiteReadResource;
import org.exoplatform.management.content.operations.site.contents.SiteContentsExportResource;
import org.exoplatform.management.content.operations.site.contents.SiteContentsImportResource;
import org.exoplatform.management.content.operations.site.contents.SiteContentsReadResource;
import org.exoplatform.management.content.operations.site.seo.SiteSEOExportResource;
import org.exoplatform.management.content.operations.site.seo.SiteSEOReadResource;
import org.gatein.management.api.ComponentRegistration;
import org.gatein.management.api.ManagedResource;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.spi.ExtensionContext;

/**
 * @author <a href="mailto:thomas.delhomenie@exoplatform.com">Thomas
 *         Delhoménie</a>
 * @version $Revision$
 */
public class ContentManagementExtension extends AbstractManagementExtension {

  public final static String PATH_CONTENT = "content";
  public final static String PATH_CONTENT_SITES = "sites";
  public final static String PATH_CONTENT_SITES_CONTENTS = "contents";
  public final static String PATH_CONTENT_SITES_SEO = "seo";

  @Override
  public void initialize(ExtensionContext context) {
    ComponentRegistration contentRegistration = context.registerManagedComponent(PATH_CONTENT);

    ManagedResource.Registration content = contentRegistration.registerManagedResource(description("Content Managed Resource, responsible for handling management operations on contents."));
    content.registerOperationHandler(OperationNames.READ_RESOURCE, new ContentReadResource(), description("Lists available contents data"));

    // /content/sites
    ManagedResource.Registration sites = content.registerSubResource(PATH_CONTENT_SITES, description("Sites Managed Resource, responsible for handling management operations on sites contents."));
    sites.registerOperationHandler(OperationNames.READ_RESOURCE, new LiveSitesReadResource(), description("Lists available sites"));
    sites.registerOperationHandler(OperationNames.IMPORT_RESOURCE, new SiteContentsImportResource(), description("Import sites data"));
    sites.registerOperationHandler(OperationNames.EXPORT_RESOURCE, new SiteContentsExportResource(), description("Export site contents"));

    // /content/sites/<site_name>
    ManagedResource.Registration site = sites.registerSubResource("{site-name: [^/]*}", description("Management resource responsible for handling management operations on a specific site."));
    site.registerOperationHandler(OperationNames.READ_RESOURCE, new SiteReadResource(), description("Read site"));
    site.registerOperationHandler(OperationNames.IMPORT_RESOURCE, new SiteContentsImportResource(), description("Import site data"));

    // /content/sites/<site_name>/contents
    ManagedResource.Registration siteContents = site.registerSubResource(PATH_CONTENT_SITES_CONTENTS, description("Management resource responsible for handling management operations on contents of a specific site."));
    siteContents.registerOperationHandler(OperationNames.READ_RESOURCE, new SiteContentsReadResource(), description("Read site contents"));
    siteContents.registerOperationHandler(OperationNames.EXPORT_RESOURCE, new SiteContentsExportResource(), description("Export site contents"));

    // /content/sites/<site_name>/seo
    ManagedResource.Registration seo = site.registerSubResource(PATH_CONTENT_SITES_SEO, description("Management resource responsible for handling management operations on SEO of a specific site."));
    seo.registerOperationHandler(OperationNames.READ_RESOURCE, new SiteSEOReadResource(), description("Read site SEO data"));
    seo.registerOperationHandler(OperationNames.EXPORT_RESOURCE, new SiteSEOExportResource(), description("Export site SEO data"));

    DataTransformerService.addTransformer("Page", new PageSCVTransformer());
    DataTransformerService.addTransformer("Page", new PageCLVTransformer());
  }
}
