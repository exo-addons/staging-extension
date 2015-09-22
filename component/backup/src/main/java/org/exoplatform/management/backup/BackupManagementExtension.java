package org.exoplatform.management.backup;

import org.exoplatform.management.backup.operations.BackupExportResource;
import org.exoplatform.management.backup.operations.BackupReadResource;
import org.exoplatform.management.common.AbstractManagementExtension;
import org.gatein.management.api.ComponentRegistration;
import org.gatein.management.api.ManagedResource;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.spi.ExtensionContext;

/**
 * @author <a href="mailto:boubaker.khanfir@exoplatform.com">Boubaker
 *         Khanfir</a>
 * @version $Revision$
 */
public class BackupManagementExtension extends AbstractManagementExtension {

  public final static String PATH_BACKUP = "backup";

  @Override
  public void initialize(ExtensionContext context) {
    ComponentRegistration backup = context.registerManagedComponent(PATH_BACKUP);

    // /backup
    ManagedResource.Registration backupRegistration = backup.registerManagedResource(description("Backup JCR Content"));
    backupRegistration.registerOperationHandler(OperationNames.READ_RESOURCE, new BackupReadResource(), description("Backup JCR Content - Get list of repositories and workspaces"), true);

    ManagedResource.Registration repositoryBackup = backupRegistration.registerSubResource("{repository: [^/]*}", description("repository export"));
    repositoryBackup.registerOperationHandler(OperationNames.EXPORT_RESOURCE, new BackupExportResource(), description("Backup JCR Content"), true);
    repositoryBackup.registerSubResource("{workspace: [^/]*}", description("repository export"));
  }
}
