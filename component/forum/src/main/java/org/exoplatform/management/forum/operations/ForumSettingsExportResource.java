package org.exoplatform.management.forum.operations;

import java.util.ArrayList;
import java.util.List;

import org.exoplatform.forum.common.jcr.KSDataLocation;
import org.exoplatform.management.common.AbstractExportOperationHandler;
import org.exoplatform.management.common.activities.JCRNodeExportTask;
import org.exoplatform.services.jcr.RepositoryService;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.exceptions.ResourceNotFoundException;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.ExportResourceModel;
import org.gatein.management.api.operation.model.ExportTask;

public class ForumSettingsExportResource extends AbstractExportOperationHandler {
  public static final String SYSTEM_ADMINISTRATION = "Administration.xml";
  public static final String BANNED_IP = "BannedIP.xml";
  public static final String USER_PROFLES = "UserProfiles.xml";
  public static final String USER_AVATARS = "UserAvatars.xml";
  public static final String BB_CODES = "BBCode.xml";
  public static final String TAGS = "Tags.xml";

  private RepositoryService repositoryService;
  private KSDataLocation dataLocation;

  private boolean isSingleResource;

  public ForumSettingsExportResource() {}

  public ForumSettingsExportResource(boolean resource) {
    this.isSingleResource = resource;
  }

  @Override
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws ResourceNotFoundException, OperationException {
    repositoryService = operationContext.getRuntimeContext().getRuntimeComponent(RepositoryService.class);
    dataLocation = operationContext.getRuntimeContext().getRuntimeComponent(KSDataLocation.class);

    List<ExportTask> exportTasks = new ArrayList<ExportTask>();
    String workspace = dataLocation.getWorkspace();

    if (isExportResource(operationContext, "general-administration")) {
      String entryPath = "forum/settings/" + SYSTEM_ADMINISTRATION;
      exportTasks.add(new JCRNodeExportTask(repositoryService, workspace, dataLocation.getAdministrationLocation(), entryPath, true, false));
    }

    if (isExportResource(operationContext, "banned-ip")) {
      String entryPath = "forum/settings/" + BANNED_IP;
      exportTasks.add(new JCRNodeExportTask(repositoryService, workspace, dataLocation.getBanIPLocation(), entryPath, true, false));
    }

    if (isExportResource(operationContext, "user-profiles")) {
      String entryPath = "forum/settings/" + USER_PROFLES;
      exportTasks.add(new JCRNodeExportTask(repositoryService, workspace, dataLocation.getUserProfilesLocation(), entryPath, true, false));
      entryPath = "forum/settings/" + USER_AVATARS;
      exportTasks.add(new JCRNodeExportTask(repositoryService, workspace, dataLocation.getAvatarsLocation(), entryPath, true, false));
    }

    if (isExportResource(operationContext, "bb-codes")) {
      String entryPath = "forum/settings/" + BB_CODES;
      exportTasks.add(new JCRNodeExportTask(repositoryService, workspace, dataLocation.getBBCodesLocation(), entryPath, true, false));
    }

    if (isExportResource(operationContext, "tags")) {
      String entryPath = "forum/settings/" + TAGS;
      exportTasks.add(new JCRNodeExportTask(repositoryService, workspace, dataLocation.getTagsLocation(), entryPath, true, false));
    }

    resultHandler.completed(new ExportResourceModel(exportTasks));
  }

  private boolean isExportResource(OperationContext operationContext, String resourceName) {
    String requestedResourceName = operationContext.getAddress().resolvePathTemplate("resource-name");
    return (isSingleResource && resourceName.equals(requestedResourceName)) || (!isSingleResource && getBooleanValue(operationContext, resourceName));
  }

  private boolean getBooleanValue(OperationContext operationContext, String paramName) {
    String paramValueString = operationContext.getAttributes().getValue(paramName);
    return paramValueString != null && paramValueString.trim().equalsIgnoreCase("true");
  }
}
