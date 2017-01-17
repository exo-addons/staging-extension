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
package org.exoplatform.management.ecmadmin.operations.drive;

import org.exoplatform.management.common.AbstractOperationHandler;
import org.exoplatform.services.cms.drives.DriveData;
import org.exoplatform.services.cms.drives.ManageDriveService;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.ReadResourceModel;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The Class DriveReadResource.
 *
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class DriveReadResource extends AbstractOperationHandler {
  
  /** The drive service. */
  private ManageDriveService driveService;

  /**
   * {@inheritDoc}
   */
  @Override
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws OperationException {
    if (driveService == null) {
      driveService = operationContext.getRuntimeContext().getRuntimeComponent(ManageDriveService.class);
    }
    try {
      Set<String> driveNames = new HashSet<String>();
      List<DriveData> driveDataList = driveService.getAllDrives(true);
      for (DriveData driveData : driveDataList) {
        driveNames.add(driveData.getName());
      }
      resultHandler.completed(new ReadResourceModel("Available drives.", driveNames));
    } catch (Exception e) {
      throw new OperationException(OperationNames.READ_RESOURCE, "Error while retrieving ECMS drives.", e);
    }
  }
}