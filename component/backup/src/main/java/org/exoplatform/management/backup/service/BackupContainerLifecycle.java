package org.exoplatform.management.backup.service;

import org.exoplatform.container.BaseContainerLifecyclePlugin;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.organization.idm.PicketLinkIDMService;
import org.picketlink.idm.api.IdentitySession;

public class BackupContainerLifecycle extends BaseContainerLifecyclePlugin {
  @Override
  public void startContainer(ExoContainer container) throws Exception {
    // FIXME workaround for WIKI-1084
    if (container instanceof PortalContainer) {
      PicketLinkIDMService idmService = (PicketLinkIDMService) container.getComponentInstanceOfType(PicketLinkIDMService.class);
      IdentitySession identitySession = idmService.getIdentitySession();
      identitySession.close();
    }
  }
}
