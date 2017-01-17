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
import org.exoplatform.management.common.exportop.AbstractExportOperationHandler;
import org.exoplatform.management.common.exportop.JCRNodeExportTask;
import org.exoplatform.services.jcr.RepositoryService;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.exceptions.ResourceNotFoundException;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.ExportResourceModel;
import org.gatein.management.api.operation.model.ExportTask;

import java.util.ArrayList;
import java.util.List;

/**
 * The Class ForumSettingsExportResource.
 */
public class ForumSettingsExportResource extends AbstractExportOperationHandler {
  
  /** The Constant SYSTEM_ADMINISTRATION. */
  public static final String SYSTEM_ADMINISTRATION = "Administration.xml";
  
  /** The Constant BANNED_IP. */
  public static final String BANNED_IP = "BannedIP.xml";
  
  /** The Constant USER_PROFLES. */
  public static final String USER_PROFLES = "UserProfiles.xml";
  
  /** The Constant USER_AVATARS. */
  public static final String USER_AVATARS = "UserAvatars.xml";
  
  /** The Constant BB_CODES. */
  public static final String BB_CODES = "BBCode.xml";
  
  /** The Constant TAGS. */
  public static final String TAGS = "Tags.xml";

  /** The repository service. */
  private RepositoryService repositoryService;
  
  /** The data location. */
  private KSDataLocation dataLocation;

  /** The is single resource. */
  private boolean isSingleResource;

  /**
   * Instantiates a new forum settings export resource.
   */
  public ForumSettingsExportResource() {}

  /**
   * Instantiates a new forum settings export resource.
   *
   * @param resource the resource
   */
  public ForumSettingsExportResource(boolean resource) {
    this.isSingleResource = resource;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws ResourceNotFoundException, OperationException {
    repositoryService = operationContext.getRuntimeContext().getRuntimeComponent(RepositoryService.class);
    dataLocation = operationContext.getRuntimeContext().getRuntimeComponent(KSDataLocation.class);

    List<ExportTask> exportTasks = new ArrayList<ExportTask>();
    String workspace = dataLocation.getWorkspace();

    if (isExportResource(operationContext, "general-administration")) {
      String entryPath = "forum/settings/" + SYSTEM_ADMINISTRATION;
      String path = sanitizePath(dataLocation.getAdministrationLocation());
      exportTasks.add(new JCRNodeExportTask(repositoryService, workspace, path, entryPath, true, false));
    }

    if (isExportResource(operationContext, "banned-ip")) {
      String entryPath = "forum/settings/" + BANNED_IP;
      String path = sanitizePath(dataLocation.getBanIPLocation());
      exportTasks.add(new JCRNodeExportTask(repositoryService, workspace, path, entryPath, true, false));
    }

    if (isExportResource(operationContext, "user-profiles")) {
      String entryPath = "forum/settings/" + USER_PROFLES;
      String path = sanitizePath(dataLocation.getUserProfilesLocation());
      exportTasks.add(new JCRNodeExportTask(repositoryService, workspace, path, entryPath, true, false));
      entryPath = "forum/settings/" + USER_AVATARS;
      path = sanitizePath(dataLocation.getAvatarsLocation());
      exportTasks.add(new JCRNodeExportTask(repositoryService, workspace, path, entryPath, true, false));
    }

    if (isExportResource(operationContext, "bb-codes")) {
      String entryPath = "forum/settings/" + BB_CODES;
      String path = sanitizePath(dataLocation.getBBCodesLocation());
      exportTasks.add(new JCRNodeExportTask(repositoryService, workspace, path, entryPath, true, false));
    }

    if (isExportResource(operationContext, "tags")) {
      String entryPath = "forum/settings/" + TAGS;
      String path = sanitizePath(dataLocation.getTagsLocation());
      exportTasks.add(new JCRNodeExportTask(repositoryService, workspace, path, entryPath, true, false));
    }

    resultHandler.completed(new ExportResourceModel(exportTasks));
  }

  /**
   * Sanitize path.
   *
   * @param path the path
   * @return the string
   */
  private String sanitizePath(String path) {
    if (path.startsWith("/")) {
      return path;
    } else {
      return "/" + path;
    }
  }

  /**
   * Checks if is export resource.
   *
   * @param operationContext the operation context
   * @param resourceName the resource name
   * @return true, if is export resource
   */
  private boolean isExportResource(OperationContext operationContext, String resourceName) {
    String requestedResourceName = operationContext.getAddress().resolvePathTemplate("resource-name");
    return (isSingleResource && resourceName.equals(requestedResourceName)) || (!isSingleResource && getBooleanValue(operationContext, resourceName));
  }

  /**
   * Gets the boolean value.
   *
   * @param operationContext the operation context
   * @param paramName the param name
   * @return the boolean value
   */
  private boolean getBooleanValue(OperationContext operationContext, String paramName) {
    String paramValueString = operationContext.getAttributes().getValue(paramName);
    return paramValueString != null && paramValueString.trim().equalsIgnoreCase("true");
  }
}
