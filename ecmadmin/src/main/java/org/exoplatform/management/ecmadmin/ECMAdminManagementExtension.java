package org.exoplatform.management.ecmadmin;

import java.util.HashSet;

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
import org.gatein.management.api.ComponentRegistration;
import org.gatein.management.api.ManagedDescription;
import org.gatein.management.api.ManagedResource;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.exceptions.ResourceNotFoundException;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.OperationHandler;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.ReadResourceModel;
import org.gatein.management.spi.ExtensionContext;
import org.gatein.management.spi.ManagementExtension;

/**
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class ECMAdminManagementExtension implements ManagementExtension {
  @Override
  public void initialize(ExtensionContext context) {
    ComponentRegistration ecmadminRegistration = context.registerManagedComponent("ecmadmin");

    ManagedResource.Registration ecmadmin = ecmadminRegistration
        .registerManagedResource(description("Ecmadmin Managed Resource, responsible for handling management operations on ECMS administration contents."));
    ecmadmin.registerOperationHandler(OperationNames.READ_RESOURCE, new ECMAdminContentReadResource(),
        description("Lists available ECMS administration data"));
    ecmadmin.registerOperationHandler(OperationNames.IMPORT_RESOURCE, new ECMAdminContentImportResource(),
        description("Lists available ECMS administration data"));

    // /ecmadmin/templates
    ManagedResource.Registration templates = ecmadmin.registerSubResource("templates",
        description("Sites Managed Resource, responsible for handling management operations on templates."));
    templates.registerOperationHandler(OperationNames.READ_RESOURCE, new TemplatesReadResource(),
        description("Lists available template types"));

    // /ecmadmin/templates/applications
    ManagedResource.Registration applicationsTemplates = templates.registerSubResource("applications",
        description("Sites Managed Resource, responsible for handling management operations on applications templates."));
    applicationsTemplates.registerOperationHandler(OperationNames.READ_RESOURCE, new ApplicationsTemplatesReadResource(),
        description("Lists available applications containing templates"));
    applicationsTemplates.registerOperationHandler(OperationNames.IMPORT_RESOURCE, new ApplicationsTemplatesImportResource(),
        description("Import applications templates"));

    // /ecmadmin/templates/applications/<application_name>
    ManagedResource.Registration applicationTemplates = applicationsTemplates.registerSubResource("{application-name: .*}",
        description("Sites Managed Resource, responsible for handling management operations on templates of an application."));
    applicationTemplates.registerOperationHandler(OperationNames.READ_RESOURCE, new ApplicationTemplatesReadResource(),
        description("Lists available templates of an application"));
    applicationTemplates.registerOperationHandler(OperationNames.EXPORT_RESOURCE, new ApplicationTemplatesExportResource(),
        description("Exports available templates of an application"));

    // /ecmadmin/templates/nodetypes
    ManagedResource.Registration nodetypesTemplates = templates.registerSubResource("nodetypes",
        description("Sites Managed Resource, responsible for handling management operations on node types templates."));
    nodetypesTemplates.registerOperationHandler(OperationNames.READ_RESOURCE, new NodeTypesTemplatesReadResource(),
        description("Lists available node types templates"));
    nodetypesTemplates.registerOperationHandler(OperationNames.EXPORT_RESOURCE, new NodeTypesTemplatesExportResource(),
        description("Exports available node types templates"));
    nodetypesTemplates.registerOperationHandler(OperationNames.IMPORT_RESOURCE, new NodeTypesTemplatesImportResource(),
        description("Imports available node types templates"));

    // /ecmadmin/templates/metadata
    ManagedResource.Registration metadataTemplates = templates.registerSubResource("metadata",
        description("Sites Managed Resource, responsible for handling management operations on metadata templates."));
    metadataTemplates.registerOperationHandler(OperationNames.READ_RESOURCE, new MetadataTemplatesReadResource(),
        description("Lists available metadata templates"));
    metadataTemplates.registerOperationHandler(OperationNames.EXPORT_RESOURCE, new MetadataTemplatesExportResource(),
        description("Exports available metadata templates"));
    metadataTemplates.registerOperationHandler(OperationNames.IMPORT_RESOURCE, new MetadataTemplatesImportResource(),
        description("Imports available metadata templates"));

    // /ecmadmin/queries
    ManagedResource.Registration queries = ecmadmin.registerSubResource("queries",
        description("Queries Managed Resource, responsible for handling management operations on queries."));
    queries.registerOperationHandler(OperationNames.READ_RESOURCE, new QueriesReadResource(),
        description("Lists available queries"));
    queries.registerOperationHandler(OperationNames.EXPORT_RESOURCE, new QueriesExportResource(),
        description("Exports available queries"));
    queries.registerOperationHandler(OperationNames.IMPORT_RESOURCE, new QueriesImportResource(), description("Imports queries"));

    // /ecmadmin/taxonomy
    ManagedResource.Registration taxonomies = ecmadmin.registerSubResource("taxonomy",
        description("Taxonomy Managed Resource, responsible for handling management operations on taxonomies."));
    taxonomies.registerOperationHandler(OperationNames.READ_RESOURCE, new TaxonomyReadResource(),
        description("Lists available taxonomies"));
    taxonomies.registerOperationHandler(OperationNames.IMPORT_RESOURCE, new TaxonomyImportResource(),
        description("Imports available taxonomies"));

    // /ecmadmin/taxonomy/name
    ManagedResource.Registration taxonomy = taxonomies.registerSubResource("{taxonomy-name: .*}",
        description("Taxonomy Managed Resource, responsible for handling management operations on taxonomies."));
    taxonomy.registerOperationHandler(OperationNames.READ_RESOURCE, new EmptyReadResource(), description("nothing"));
    taxonomy.registerOperationHandler(OperationNames.EXPORT_RESOURCE, new TaxonomyExportResource(),
        description("Exports selected taxonomy"));

    // /ecmadmin/script
    ManagedResource.Registration script = ecmadmin.registerSubResource("script",
        description("ECMS script Managed Resource, responsible for handling management operations on ECMS scripts."));
    script.registerOperationHandler(OperationNames.READ_RESOURCE, new ScriptReadResource(),
        description("Lists available scripts"));
    script.registerOperationHandler(OperationNames.EXPORT_RESOURCE, new ScriptExportResource(),
        description("Exports available scripts"));
    script.registerOperationHandler(OperationNames.IMPORT_RESOURCE, new ScriptImportResource(), description("Imports scripts"));

    // /ecmadmin/action
    ManagedResource.Registration action = ecmadmin.registerSubResource("action",
        description("Action Managed Resource, responsible for handling management operations on actions."));
    action.registerOperationHandler(OperationNames.READ_RESOURCE, new ActionReadResource(),
        description("Lists available actions"));
    action.registerOperationHandler(OperationNames.EXPORT_RESOURCE, new ActionExportResource(),
        description("Exports available actions"));
    action.registerOperationHandler(OperationNames.IMPORT_RESOURCE, new NodeTypeImportResource("nodetype"),
        description("Imports actions"));

    // /ecmadmin/nodetype
    ManagedResource.Registration nodetype = ecmadmin.registerSubResource("nodetype",
        description("Nodetype Managed Resource, responsible for handling management operations on nodetypes."));
    nodetype.registerOperationHandler(OperationNames.READ_RESOURCE, new NodeTypeReadResource(),
        description("Lists available actions"));
    nodetype.registerOperationHandler(OperationNames.EXPORT_RESOURCE, new NodeTypeExportResource(),
        description("Exports available actions"));
    nodetype.registerOperationHandler(OperationNames.IMPORT_RESOURCE, new NodeTypeImportResource("action"),
        description("Imports actions"));

    // /ecmadmin/drive
    ManagedResource.Registration drive = ecmadmin.registerSubResource("drive",
        description("ECMS Drive Managed Resource, responsible for handling management operations on ECMS drives."));
    drive.registerOperationHandler(OperationNames.READ_RESOURCE, new DriveReadResource(), description("Lists available drives"));
    drive.registerOperationHandler(OperationNames.EXPORT_RESOURCE, new DriveExportResource(),
        description("Exports available drives"));
    drive.registerOperationHandler(OperationNames.IMPORT_RESOURCE, new DriveImportResource(), description("Imports drives"));

  }

  @Override
  public void destroy() {}

  private static ManagedDescription description(final String description) {
    return new ManagedDescription() {
      @Override
      public String getDescription() {
        return description;
      }
    };
  }

  public static class EmptyReadResource implements OperationHandler {
    @Override
    public void execute(OperationContext operationContext, ResultHandler resultHandler) throws ResourceNotFoundException,
        OperationException {
      resultHandler.completed(new ReadResourceModel("Empty", new HashSet<String>()));
    }

  }

}
