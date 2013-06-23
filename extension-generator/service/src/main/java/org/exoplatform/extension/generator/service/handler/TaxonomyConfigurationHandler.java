package org.exoplatform.extension.generator.service.handler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipOutputStream;

import org.exoplatform.extension.generator.service.AbstractConfigurationHandler;
import org.exoplatform.services.cms.taxonomy.impl.TaxonomyConfig;
import org.exoplatform.services.cms.taxonomy.impl.TaxonomyConfig.Permission;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

public class TaxonomyConfigurationHandler extends AbstractConfigurationHandler {
  private Log log = ExoLogger.getLogger(this.getClass());

  @Override
  public boolean writeData(ZipOutputStream zos, Set<String> selectedResources) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public List<String> getConfigurationPaths() {
    return null;
  }

  @Override
  protected Log getLogger() {
    return log;
  }

  @SuppressWarnings({ "rawtypes", "unused" })
  private List<Permission> getPermissions(Map permissions) {
    List<TaxonomyConfig.Permission> listPermissions = new ArrayList<TaxonomyConfig.Permission>();
    Set permEntries = permissions.entrySet();
    for (Object obj : permEntries) {
      Map.Entry permEntry = (Map.Entry) obj;
      TaxonomyConfig.Permission permission = new TaxonomyConfig.Permission();
      permission.setIdentity((String) permEntry.getKey());
      String permExpr = (String) permEntry.getValue();
      permission.setRead("" + permExpr.contains(PermissionType.READ));
      permission.setAddNode("" + permExpr.contains(PermissionType.ADD_NODE));
      permission.setSetProperty("" + permExpr.contains(PermissionType.SET_PROPERTY));
      permission.setRemove("" + permExpr.contains(PermissionType.REMOVE));
      listPermissions.add(permission);
    }
    return listPermissions;
  }

}
