/*
 * Copyright (C) 2003-2017 eXo Platform SAS.
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
package org.exoplatform.management.backup;

import org.exoplatform.management.backup.operations.BackupExportResource;
import org.exoplatform.management.backup.operations.BackupImportResource;
import org.exoplatform.management.backup.operations.BackupReadResource;
import org.exoplatform.management.common.AbstractManagementExtension;
import org.gatein.management.api.ComponentRegistration;
import org.gatein.management.api.ManagedResource;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.spi.ExtensionContext;

/**
 * The Class BackupManagementExtension.
 *
 * @author <a href="mailto:boubaker.khanfir@exoplatform.com">Boubaker
 *         Khanfir</a>
 * @version $Revision$
 */
public class BackupManagementExtension extends AbstractManagementExtension {

  /** The Constant PATH_BACKUP. */
  public final static String PATH_BACKUP = "backup";

  /**
   * {@inheritDoc}
   */
  @Override
  public void initialize(ExtensionContext context) {
    ComponentRegistration backup = context.registerManagedComponent(PATH_BACKUP);

    // /backup
    ManagedResource.Registration backupRegistration = backup.registerManagedResource(description("Backup JCR Content"));
    backupRegistration.registerOperationHandler(OperationNames.READ_RESOURCE, new BackupReadResource(), description("Backup JCR Content - Get list of repositories and workspaces"), true);

    ManagedResource.Registration repositoryBackup = backupRegistration.registerSubResource("{portal: [^/]*}", description("PortalContainer Datasources export"));
    repositoryBackup.registerOperationHandler(OperationNames.EXPORT_RESOURCE, new BackupExportResource(), description("Backup JCR & IDM"), true);
    repositoryBackup.registerOperationHandler(OperationNames.IMPORT_RESOURCE, new BackupImportResource(), description("Restore JCR & IDM"), true);
  }
}
