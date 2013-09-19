@Portlet
@Application(name = "StagingExtension")
@Bindings(
  {
    @Binding(value = ManagementController.class),
    @Binding(value = StagingService.class, implementation = StagingServiceImpl.class),
    @Binding(value = SynchronizationService.class, implementation = SynchronizationServiceImpl.class),
    @Binding(value = RepositoryService.class),
    @Binding(value = ChromatticService.class, implementation = ChromatticServiceImpl.class)
  }
)
package org.exoplatform.management.portlet;

import juzu.Application;
import juzu.plugin.binding.Binding;
import juzu.plugin.binding.Bindings;
import juzu.plugin.portlet.Portlet;
import org.exoplatform.management.service.api.ChromatticService;
import org.exoplatform.management.service.api.StagingService;
import org.exoplatform.management.service.api.SynchronizationService;
import org.exoplatform.management.service.impl.ChromatticServiceImpl;
import org.exoplatform.management.service.impl.StagingServiceImpl;
import org.exoplatform.management.service.impl.SynchronizationServiceImpl;
import org.exoplatform.services.jcr.RepositoryService;
import org.gatein.management.api.controller.ManagementController;