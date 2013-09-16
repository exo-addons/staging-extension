package org.exoplatform.management.service.handler.organization;

import org.exoplatform.management.service.api.AbstractResourceHandler;
import org.exoplatform.management.service.api.Resource;
import org.exoplatform.management.service.api.StagingService;
import org.exoplatform.management.service.api.TargetServer;

import java.util.List;
import java.util.Map;

public class GroupsHandler extends AbstractResourceHandler {

  @Override
  public String getPath() {
    return StagingService.GROUPS_PATH;
  }

  @Override
  public void synchronize(List<Resource> resources, Map<String, String> exportOptions, Map<String, String> importOptions, TargetServer targetServer) {
    for (Resource resource : resources) {
      resource.setPath(resource.getPath().replaceAll("//", "/"));
    }

    super.synchronize(resources, exportOptions, importOptions, targetServer);
  }
}