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
package org.exoplatform.management.forum.operations;

import org.exoplatform.forum.common.jcr.KSDataLocation;
import org.exoplatform.forum.service.ForumService;
import org.exoplatform.forum.service.Utils;
import org.exoplatform.management.common.FileEntry;
import org.exoplatform.management.common.importop.AbstractJCRImportOperationHandler;
import org.exoplatform.management.common.importop.FileImportOperationInterface;
import org.exoplatform.services.jcr.RepositoryService;
import org.gatein.common.logging.Logger;
import org.gatein.common.logging.LoggerFactory;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.NoResultModel;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * The Class ForumSettingsImportResource.
 */
public class ForumSettingsImportResource extends AbstractJCRImportOperationHandler implements FileImportOperationInterface {

  /** The Constant FORUM_SETTINGS_LABEL. */
  private static final String FORUM_SETTINGS_LABEL = "FORUM_SETTINGS";

  /** The Constant log. */
  final private static Logger log = LoggerFactory.getLogger(ForumSettingsImportResource.class);

  /** The data location. */
  private KSDataLocation dataLocation;
  
  /** The forum service. */
  private ForumService forumService;

  /**
   * {@inheritDoc}
   */
  @Override
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws OperationException {
    repositoryService = operationContext.getRuntimeContext().getRuntimeComponent(RepositoryService.class);
    dataLocation = operationContext.getRuntimeContext().getRuntimeComponent(KSDataLocation.class);
    forumService = operationContext.getRuntimeContext().getRuntimeComponent(ForumService.class);

    log.info("Importing Forums Data");
    InputStream attachmentInputStream = getAttachementInputStream(operationContext);
    try {
      // extract data from zip
      Map<String, List<FileEntry>> contentsByOwner = extractDataFromZip(attachmentInputStream);
      List<FileEntry> fileEntries = contentsByOwner.get(FORUM_SETTINGS_LABEL);

      String workspace = dataLocation.getWorkspace();
      for (FileEntry fileEntry : fileEntries) {
        String locationPath = fileEntry.getNodePath();
        if (locationPath.endsWith(ForumSettingsExportResource.SYSTEM_ADMINISTRATION)) {
          log.info("Importing forum system administration settings...");
          fileEntry.setNodePath("/" + dataLocation.getAdministrationLocation());
        } else if (locationPath.endsWith(ForumSettingsExportResource.BANNED_IP)) {
          log.info("Importing forum ban IP settings...");
          fileEntry.setNodePath("/" + dataLocation.getBanIPLocation());
        } else if (locationPath.endsWith(ForumSettingsExportResource.BB_CODES)) {
          log.info("Importing forum bb codes settings...");
          fileEntry.setNodePath("/" + dataLocation.getBBCodesLocation());
        } else if (locationPath.endsWith(ForumSettingsExportResource.USER_PROFLES)) {
          log.info("Importing forum user profiles settings...");
          fileEntry.setNodePath("/" + dataLocation.getUserProfilesLocation());
        } else if (locationPath.endsWith(ForumSettingsExportResource.USER_AVATARS)) {
          log.info("Importing forum user avatars settings...");
          fileEntry.setNodePath("/" + dataLocation.getAvatarsLocation());
        } else if (locationPath.endsWith(ForumSettingsExportResource.TAGS)) {
          log.info("Importing forum tag settings...");
          fileEntry.setNodePath("/" + dataLocation.getTagsLocation());
        }
        importNode(fileEntry, workspace, false);
      }
      forumService.calculateDeletedUser("fakeUser" + Utils.DELETED);
      log.info("Forum settings imported successfully.");
    } catch (Exception e) {
      throw new OperationException(OperationNames.IMPORT_RESOURCE, "Error while importing forum settings", e);
    } finally {
      if (attachmentInputStream != null) {
        try {
          attachmentInputStream.close();
        } catch (IOException e) {
          // Nothing to do
        }
      }
    }
    resultHandler.completed(NoResultModel.INSTANCE);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getManagedFilesPrefix() {
    return "forum/settings/";
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isUnKnownFileFormat(String filePath) {
    return !filePath.endsWith(ForumSettingsExportResource.SYSTEM_ADMINISTRATION) && !filePath.endsWith(ForumSettingsExportResource.BANNED_IP)
        && !filePath.endsWith(ForumSettingsExportResource.BB_CODES) && !filePath.endsWith(ForumSettingsExportResource.USER_PROFLES) && !filePath.endsWith(ForumSettingsExportResource.USER_AVATARS)
        && !filePath.endsWith(ForumSettingsExportResource.TAGS);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean addSpecialFile(List<FileEntry> fileEntries, String filePath, File file) {
    return false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String extractIdFromPath(String path) {
    return FORUM_SETTINGS_LABEL;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getNodePath(String filePath) {
    return filePath;
  }
}
