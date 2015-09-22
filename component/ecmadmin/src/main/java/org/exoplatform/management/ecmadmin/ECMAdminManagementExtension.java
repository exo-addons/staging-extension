package org.exoplatform.management.ecmadmin;

import java.util.HashSet;

import org.exoplatform.management.common.AbstractManagementExtension;
import org.exoplatform.management.common.AbstractOperationHandler;
import org.exoplatform.management.ecmadmin.operations.ECMAdminContentImportResource;
import org.exoplatform.management.ecmadmin.operations.ECMAdminContentReadResource;
import org.exoplatform.management.ecmadmin.operations.action.ActionExportResource;
import org.exoplatform.management.ecmadmin.operations.action.ActionReadResource;
import org.exoplatform.management.ecmadmin.operations.drive.DriveExportResource;
import org.exoplatform.management.ecmadmin.operations.drive.DriveImportResource;
import org.exoplatform.management.ecmadmin.operations.drive.DriveReadResource;
import org.exoplatform.management.ecmadmin.operations.nodetype.NodeTypeExportResource;
import org.exoplatform.management.ecmadmin.operations.nodetype.NodeTypeImportResource;
import org.exoplatform.management.ecmadmin.operations.nodetype.NodeTypeReadResource;
import org.exoplatform.management.ecmadmin.operations.queries.QueriesExportResource;
import org.exoplatform.management.ecmadmin.operations.queries.QueriesImportResource;
import org.exoplatform.management.ecmadmin.operations.queries.QueriesReadResource;
import org.exoplatform.management.ecmadmin.operations.script.ScriptExportResource;
import org.exoplatform.management.ecmadmin.operations.script.ScriptImportResource;
import org.exoplatform.management.ecmadmin.operations.script.ScriptReadResource;
import org.exoplatform.management.ecmadmin.operations.taxonomy.TaxonomyExportResource;
import org.exoplatform.management.ecmadmin.operations.taxonomy.TaxonomyImportResource;
import org.exoplatform.management.ecmadmin.operations.taxonomy.TaxonomyReadResource;
import org.exoplatform.management.ecmadmin.operations.templates.TemplatesReadResource;
import org.exoplatform.management.ecmadmin.operations.templates.applications.ApplicationTemplatesExportResource;
import org.exoplatform.management.ecmadmin.operations.templates.applications.ApplicationTemplatesReadResource;
import org.exoplatform.management.ecmadmin.operations.templates.applications.ApplicationsTemplatesImportResource;
import org.exoplatform.management.ecmadmin.operations.templates.applications.ApplicationsTemplatesReadResource;
import org.exoplatform.management.ecmadmin.operations.templates.metadata.MetadataTemplatesExportResource;
import org.exoplatform.management.ecmadmin.operations.templates.metadata.MetadataTemplatesImportResource;
import org.exoplatform.management.ecmadmin.operations.templates.metadata.MetadataTemplatesReadResource;
import org.exoplatform.management.ecmadmin.operations.templates.nodetypes.NodeTypesTemplatesExportResource;
import org.exoplatform.management.ecmadmin.operations.templates.nodetypes.NodeTypesTemplatesImportResource;
import org.exoplatform.management.ecmadmin.operations.templates.nodetypes.NodeTypesTemplatesReadResource;
import org.exoplatform.management.ecmadmin.operations.view.ViewConfigurationExportResource;
import org.exoplatform.management.ecmadmin.operations.view.ViewConfigurationReadResource;
import org.exoplatform.management.ecmadmin.operations.view.ViewImportResource;
import org.exoplatform.management.ecmadmin.operations.view.ViewReadResource;
import org.exoplatform.management.ecmadmin.operations.view.ViewTemplatesExportResource;
import org.exoplatform.management.ecmadmin.operations.view.ViewTemplatesReadResource;
import org.gatein.management.api.ComponentRegistration;
import org.gatein.management.api.ManagedDescription;
import org.gatein.management.api.ManagedResource;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.exceptions.ResourceNotFoundException;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.ReadResourceModel;
import org.gatein.management.spi.ExtensionContext;

/**
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class ECMAdminManagementExtension extends AbstractManagementExtension {
  @Override
  public void initialize(ExtensionContext context) {
    ComponentRegistration ecmadminRegistration = context.registerManagedComponent("ecmadmin");

    ManagedResource.Registration ecmadmin = ecmadminRegistration.registerManagedResource(description("ECMS (Enterprise Content Management Suite) administration resources."));
    ecmadmin.registerOperationHandler(OperationNames.READ_RESOURCE, new ECMAdminContentReadResource(), description("Lists available ECMS administration data"));
    ecmadmin.registerOperationHandler(OperationNames.IMPORT_RESOURCE, new ECMAdminContentImportResource(), description("Lists available ECMS administration data"));

    // /ecmadmin/templates
    ManagedResource.Registration templates = ecmadmin.registerSubResource("templates",
        description("ECMS Groovy templates for 'Content List' and 'Advanced Search' Portlets, metadata, nodetypes and JCR Action."));
    templates.registerOperationHandler(OperationNames.READ_RESOURCE, new TemplatesReadResource(), description("Lists available template types"));

    // /ecmadmin/templates/applications
    ManagedResource.Registration applicationsTemplates = templates.registerSubResource("applications", description("ECMS Groovy templates for Content List Portlet and Advanced Search Portlet"));
    applicationsTemplates.registerOperationHandler(OperationNames.READ_RESOURCE, new ApplicationsTemplatesReadResource(), description("Lists available applications containing templates"));
    applicationsTemplates.registerOperationHandler(OperationNames.IMPORT_RESOURCE, new ApplicationsTemplatesImportResource(), description("Import applications templates"));

    // /ecmadmin/templates/applications/<application_name>
    ManagedResource.Registration applicationTemplates = applicationsTemplates.registerSubResource("{application-name: .*}", description("ECMS Groovy templates for {application-name} Portlet."));
    applicationTemplates.registerOperationHandler(OperationNames.READ_RESOURCE, new ApplicationTemplatesReadResource(), description("Lists available templates of an application"));
    applicationTemplates.registerOperationHandler(OperationNames.EXPORT_RESOURCE, new ApplicationTemplatesExportResource(), description("Exports available templates of an application"));
    applicationTemplates.registerOperationHandler(OperationNames.IMPORT_RESOURCE, new ApplicationsTemplatesImportResource(), description("Import applications templates"));

    // /ecmadmin/templates/nodetypes
    ManagedResource.Registration nodetypesTemplates = templates.registerSubResource("nodetypes", description("ECMS Groovy templates for nodetypes."));
    nodetypesTemplates.registerOperationHandler(OperationNames.READ_RESOURCE, new NodeTypesTemplatesReadResource(), description("Lists available node types templates"));
    nodetypesTemplates.registerOperationHandler(OperationNames.EXPORT_RESOURCE, new NodeTypesTemplatesExportResource(), description("Exports available node types templates"));
    nodetypesTemplates.registerOperationHandler(OperationNames.IMPORT_RESOURCE, new NodeTypesTemplatesImportResource(), description("Imports available node types templates"));

    // /ecmadmin/templates/metadata
    ManagedResource.Registration metadataTemplates = templates.registerSubResource("metadata", description("Nodetypes for metadata."));
    metadataTemplates.registerOperationHandler(OperationNames.READ_RESOURCE, new MetadataTemplatesReadResource(), description("Lists available metadata templates"));
    metadataTemplates.registerOperationHandler(OperationNames.EXPORT_RESOURCE, new MetadataTemplatesExportResource(), description("Exports available metadata templates"));
    metadataTemplates.registerOperationHandler(OperationNames.IMPORT_RESOURCE, new MetadataTemplatesImportResource(), description("Imports available metadata templates"));

    // /ecmadmin/queries
    ManagedResource.Registration queries = ecmadmin.registerSubResource("queries", description("JCR saved 'public' and 'private' Queries."));
    queries.registerOperationHandler(OperationNames.READ_RESOURCE, new QueriesReadResource(), description("Lists available queries"));
    queries.registerOperationHandler(OperationNames.EXPORT_RESOURCE, new QueriesExportResource(), description("Exports available queries"));
    queries.registerOperationHandler(OperationNames.IMPORT_RESOURCE, new QueriesImportResource(), description("Imports queries"));

    // /ecmadmin/taxonomy
    ManagedResource.Registration taxonomies = ecmadmin.registerSubResource("taxonomy", description("Taxonomies / Categories."));
    taxonomies.registerOperationHandler(OperationNames.READ_RESOURCE, new TaxonomyReadResource(), description("Lists available taxonomies"));
    taxonomies.registerOperationHandler(OperationNames.IMPORT_RESOURCE, new TaxonomyImportResource(), description("Imports available taxonomies"));

    // /ecmadmin/taxonomy/name
    ManagedResource.Registration taxonomy = taxonomies.registerSubResource("{taxonomy-name: .*}", description("Taxonomy '{taxonomy-name}'."));
    taxonomy.registerOperationHandler(OperationNames.READ_RESOURCE, new EmptyReadResource(), description("nothing"));
    taxonomy.registerOperationHandler(OperationNames.EXPORT_RESOURCE, new TaxonomyExportResource(), description("Exports selected taxonomy"));

    // /ecmadmin/script
    ManagedResource.Registration script = ecmadmin.registerSubResource("script", description("ECMS scripts of types : interceptor, widget and action."));
    script.registerOperationHandler(OperationNames.READ_RESOURCE, new ScriptReadResource(), description("Lists available scripts"));
    script.registerOperationHandler(OperationNames.EXPORT_RESOURCE, new ScriptExportResource(), description("Exports available scripts"));
    script.registerOperationHandler(OperationNames.IMPORT_RESOURCE, new ScriptImportResource(), description("Imports scripts"));

    // /ecmadmin/action
    ManagedResource.Registration action = ecmadmin.registerSubResource("action", description("NodeTypes of type JCR action."));
    action.registerOperationHandler(OperationNames.READ_RESOURCE, new ActionReadResource(), description("Lists available actions"));
    action.registerOperationHandler(OperationNames.EXPORT_RESOURCE, new ActionExportResource(), description("Exports available actions"));
    action.registerOperationHandler(OperationNames.IMPORT_RESOURCE, new NodeTypeImportResource("action"), description("Imports actions"));

    // /ecmadmin/nodetype
    ManagedResource.Registration nodetype = ecmadmin.registerSubResource("nodetype", description("All JCR Nodetypes and Namespaces."));
    nodetype.registerOperationHandler(OperationNames.READ_RESOURCE, new NodeTypeReadResource(), description("Lists available node types"));
    nodetype.registerOperationHandler(OperationNames.EXPORT_RESOURCE, new NodeTypeExportResource(), description("Exports available node types"));
    nodetype.registerOperationHandler(OperationNames.IMPORT_RESOURCE, new NodeTypeImportResource("nodetype"), description("Imports node types"));

    // /ecmadmin/drive
    ManagedResource.Registration drive = ecmadmin.registerSubResource("drive", description("ECMS General and group Drives configuration."));
    drive.registerOperationHandler(OperationNames.READ_RESOURCE, new DriveReadResource(), description("Lists available drives"));
    drive.registerOperationHandler(OperationNames.EXPORT_RESOURCE, new DriveExportResource(), description("Exports available drives"));
    drive.registerOperationHandler(OperationNames.IMPORT_RESOURCE, new DriveImportResource(), description("Imports drives"));

    // /ecmadmin/view
    ManagedResource.Registration views = ecmadmin.registerSubResource("view", description("Sites Explorer Views."));
    views.registerOperationHandler(OperationNames.READ_RESOURCE, new ViewReadResource(), description("Sites Explorer Views."));
    views.registerOperationHandler(OperationNames.IMPORT_RESOURCE, new ViewImportResource(), description("Import Sites Explorer Views."));

    // /ecmadmin/view/configuration
    ManagedResource.Registration viewsConfiguration = views.registerSubResource("configuration", description("Sites Explorer Views configuration."));
    viewsConfiguration.registerOperationHandler(OperationNames.READ_RESOURCE, new ViewConfigurationReadResource(), description("Lists available Views."));
    viewsConfiguration.registerOperationHandler(OperationNames.EXPORT_RESOURCE, new ViewConfigurationExportResource(), description("Exports selected views."));
    viewsConfiguration.registerOperationHandler(OperationNames.IMPORT_RESOURCE, new ViewImportResource(), description("Import Sites Explorer Views."));

    // /ecmadmin/view/configuration/<configuration_name>
    ManagedResource.Registration viewConfiguration = viewsConfiguration.registerSubResource("{configuration-name: .*}", description("Sites Explorer {configuration-name} View configuration."));
    viewConfiguration.registerOperationHandler(OperationNames.READ_RESOURCE, new EmptyReadResource(), description("Nothing to read."));
    viewConfiguration.registerOperationHandler(OperationNames.EXPORT_RESOURCE, new ViewConfigurationExportResource(), description("Exports selected views."));

    // /ecmadmin/view/templates
    ManagedResource.Registration viewsTemplate = views.registerSubResource("templates", description("Sites Explorer Views templates."));
    viewsTemplate.registerOperationHandler(OperationNames.READ_RESOURCE, new ViewTemplatesReadResource(), description("Lists available templates of Sites Explorer Portlet"));
    viewsTemplate.registerOperationHandler(OperationNames.EXPORT_RESOURCE, new ViewTemplatesExportResource(), description("Exports available templates of Sites Explorer Portlet"));
    viewsTemplate.registerOperationHandler(OperationNames.IMPORT_RESOURCE, new ViewImportResource(), description("Import Sites Explorer Views."));

    // /ecmadmin/view/templates/<template-name>
    ManagedResource.Registration viewTemplate = viewsTemplate.registerSubResource("{template-name: .*}", description("Sites Explorer {template-name} View template."));
    viewTemplate.registerOperationHandler(OperationNames.READ_RESOURCE, new EmptyReadResource(), description("Nothing to read."));
    viewTemplate.registerOperationHandler(OperationNames.EXPORT_RESOURCE, new ViewTemplatesExportResource(), description("Exports {template-name} template of Sites Explorer Portlet"));
  }

}
