package org.exoplatform.management.service.handler.ecmadmin;

import org.exoplatform.management.service.api.AbstractResourceHandler;
import org.exoplatform.management.service.api.Resource;
import org.exoplatform.management.service.api.StagingService;
import org.exoplatform.management.service.api.TargetServer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class NodeTypeTemplatesHandler extends AbstractResourceHandler {
  @Override
  public String getPath() {
    return StagingService.ECM_TEMPLATES_DOCUMENT_TYPE_PATH;
  }

  @Override
  public void synchronize(List<Resource> resources, Map<String, String> exportOptions, Map<String, String> importOptions, TargetServer targetServer) {
    for (Resource resource : resources) {
      String resourcePath = resource.getPath().replace(getPath() + "/", "");
      exportOptions.put("filter/" + resourcePath, null);
    }

    List<Resource> allResources = new ArrayList<Resource>();
    allResources.add(new Resource(getPath(), null, null));

    super.synchronize(allResources, exportOptions, importOptions, targetServer);
  }
}
