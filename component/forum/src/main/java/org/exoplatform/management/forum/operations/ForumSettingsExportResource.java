package org.exoplatform.management.forum.operations;

import java.util.ArrayList;
import java.util.List;

import org.exoplatform.forum.common.jcr.KSDataLocation;
import org.exoplatform.management.common.AbstractOperationHandler;
import org.exoplatform.services.jcr.RepositoryService;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.exceptions.ResourceNotFoundException;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.ExportResourceModel;
import org.gatein.management.api.operation.model.ExportTask;

public class ForumSettingsExportResource extends AbstractOperationHandler {
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
      exportTasks.add(new ForumSettingsExportTask(repositoryService, SYSTEM_ADMINISTRATION, workspace, "/" + dataLocation.getAdministrationLocation()));
    }

    if (isExportResource(operationContext, "banned-ip")) {
      exportTasks.add(new ForumSettingsExportTask(repositoryService, BANNED_IP, workspace, "/" + dataLocation.getBanIPLocation()));
    }

    if (isExportResource(operationContext, "user-profiles")) {
      exportTasks.add(new ForumSettingsExportTask(repositoryService, USER_PROFLES, workspace, "/" + dataLocation.getUserProfilesLocation()));
      exportTasks.add(new ForumSettingsExportTask(repositoryService, USER_AVATARS, workspace, "/" + dataLocation.getAvatarsLocation()));
    }

    if (isExportResource(operationContext, "bb-codes")) {
      exportTasks.add(new ForumSettingsExportTask(repositoryService, BB_CODES, workspace, "/" + dataLocation.getBBCodesLocation()));
    }

    if (isExportResource(operationContext, "tags")) {
      exportTasks.add(new ForumSettingsExportTask(repositoryService, TAGS, workspace, "/" + dataLocation.getTagsLocation()));
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
