package org.exoplatform.management.mop;

import org.exoplatform.management.mop.binding.CustomMopBindingProvider;
import org.exoplatform.portal.mop.management.operations.MopImportResource;
import org.exoplatform.portal.mop.management.operations.MopReadResource;
import org.exoplatform.portal.mop.management.operations.navigation.NavigationExportResource;
import org.exoplatform.portal.mop.management.operations.navigation.NavigationReadConfigAsXml;
import org.exoplatform.portal.mop.management.operations.navigation.NavigationReadResource;
import org.exoplatform.portal.mop.management.operations.page.PageExportResource;
import org.exoplatform.portal.mop.management.operations.page.PageReadConfigAsXml;
import org.exoplatform.portal.mop.management.operations.page.PageReadResource;
import org.exoplatform.portal.mop.management.operations.site.SiteLayoutExportResource;
import org.exoplatform.portal.mop.management.operations.site.SiteLayoutReadConfigAsXml;
import org.exoplatform.portal.mop.management.operations.site.SiteLayoutReadResource;
import org.exoplatform.portal.mop.management.operations.site.SiteReadResource;
import org.exoplatform.portal.mop.management.operations.site.SiteTypeReadResource;
import org.gatein.management.api.ComponentRegistration;
import org.gatein.management.api.ManagedDescription;
import org.gatein.management.api.ManagedResource;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.spi.ExtensionContext;
import org.gatein.management.spi.ManagementExtension;

public class CustomMopManagementExtension implements ManagementExtension {
    public void initialize(ExtensionContext context) {
        ComponentRegistration registration = context.registerManagedComponent("mop");
        registration.registerBindingProvider(CustomMopBindingProvider.INSTANCE);

        ManagedResource.Registration mop = registration
                .registerManagedResource(description("MOP (Model Object for Portal) Managed Resource, responsible for handling management operations on navigation, pages, and sites."));
        mop.registerOperationHandler(OperationNames.IMPORT_RESOURCE, new MopImportResource(),
                description("Imports mop data from an exported zip file."));

        mop.registerOperationHandler(OperationNames.READ_RESOURCE, new MopReadResource(),
                description("Lists available site types for a portal"));

        ManagedResource.Registration sitetypes = mop
                .registerSubResource(
                        "{site-type}sites",
                        description("Management resource responsible for handling management operations on a specific site type for a portal."));
        sitetypes.registerOperationHandler(OperationNames.READ_RESOURCE, new SiteTypeReadResource(),
                description("Lists available sites for a given site type."));

        ManagedResource.Registration sites = sitetypes.registerSubResource("{site-name: .*}",
                description("Management resource responsible for handling management operations on a specific site."));
        sites.registerOperationHandler(OperationNames.READ_RESOURCE, new SiteReadResource(),
                description("Lists available resources for a given site (ie pages, navigation, site layout)"));

        // Site Layout management
        siteLayoutManagementRegistration(sites);

        // Page management
        pageManagementRegistration(sites);

        // Navigation management
        navigationManagementRegistration(sites);
    }

    private void siteLayoutManagementRegistration(ManagedResource.Registration sites) {
        // This allows us to filter based on path template site-layout.
        ManagedResource.Registration siteLayout = sites.registerSubResource("{site-layout: portal|group|user}",
                description("Management resource responsible for handling management operations for a site's layout."));
        siteLayout.registerOperationHandler(OperationNames.READ_RESOURCE, new SiteLayoutReadResource(),
                description("The site layout resource."));
        siteLayout.registerOperationHandler(OperationNames.READ_CONFIG_AS_XML, new SiteLayoutReadConfigAsXml(),
                description("Reads site layout data for a specific site as configuration xml."));
        siteLayout.registerOperationHandler(OperationNames.EXPORT_RESOURCE, new SiteLayoutExportResource(),
                description("Exports site layout configuration xml as a zip file."));
    }

    private void pageManagementRegistration(ManagedResource.Registration sites) {
        // Pages management resource registration
        ManagedResource.Registration pages = sites.registerSubResource("pages",
                description("Management resource responsible for handling management operations for pages of a site."));

        // Pages management operations
        pages.registerOperationHandler(OperationNames.READ_RESOURCE, new PageReadResource(),
                description("Lists available pages at a specified address."), true);
        pages.registerOperationHandler(OperationNames.READ_CONFIG_AS_XML, new PageReadConfigAsXml(),
                description("Reads pages as configuration xml at a specified address."), true);
        pages.registerOperationHandler(OperationNames.EXPORT_RESOURCE, new PageExportResource(),
                description("Exports pages configuration xml as a zip file."), true);

        // Page name management resource registration
        pages.registerSubResource("{page-name}", description("Page resource representing an individual page of a site."));
    }

    private void navigationManagementRegistration(ManagedResource.Registration sites) {
        // Navigation management resource registration
        ManagedResource.Registration navigation = sites.registerSubResource("navigation",
                description("Management resource responsible for handling management operations on a sites navigation."));

        // Navigation management operations
        navigation.registerOperationHandler(OperationNames.READ_RESOURCE, new NavigationReadResource(),
                description("Available navigation nodes at the specified address."), true);
        navigation.registerOperationHandler(OperationNames.READ_CONFIG_AS_XML, new NavigationReadConfigAsXml(),
                description("Reads navigation as configuration xml at a specified address."), true);
        navigation.registerOperationHandler(OperationNames.EXPORT_RESOURCE, new NavigationExportResource(),
                description("Exports navigation configuration xml as a zip file."), true);

        // Navigation node management resource registration
        navigation
                .registerSubResource(
                        "{nav-uri: .*}",
                        description("Management resource responsible for handling management operations on specific navigation nodes."));
    }

    public void destroy() {
    }

    private static ManagedDescription description(final String description) {
        return new ManagedDescription() {
            public String getDescription() {
                return description;
            }
        };
    }
}
