package org.exoplatform.management.service.handler.ecmadmin;

import java.util.List;
import java.util.Map;
import java.util.zip.ZipOutputStream;

import org.exoplatform.management.service.api.AbstractResourceHandler;
import org.exoplatform.management.service.api.Resource;
import org.exoplatform.management.service.api.StagingService;
import org.exoplatform.management.service.api.TargetServer;

public class ScriptsHandler extends AbstractResourceHandler {
  @Override
  public String getPath() {
    return StagingService.ECM_SCRIPT_PATH;
  }

  @Override
  public void synchronize(List<Resource> resources, Map<String, String> exportOptions, Map<String, String> importOptions, TargetServer targetServer) throws Exception {
    super.synchronizeResourcesInFilter(resources, exportOptions, importOptions, targetServer);
  }

  @Override
  public void export(List<Resource> resources, ZipOutputStream exportFileOS, Map<String, String> exportOptions) throws Exception {
    super.exportResourcesInFilter(resources, exportFileOS, exportOptions);
  }
}
