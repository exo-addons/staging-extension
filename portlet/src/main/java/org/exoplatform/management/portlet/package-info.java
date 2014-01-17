@Portlet
@Application(name = "StagingExtension")
@Bindings(
  {
    @Binding(value = ManagementController.class),
    @Binding(value = ManagementService.class),
    @Binding(value = StagingService.class),
    @Binding(value = SynchronizationService.class),
    @Binding(value = RepositoryService.class),
    @Binding(value = ChromatticService.class)
  }
)
@Assets(
  scripts = {
    // AngularJS is still global, should be AMDified
    @Script(id = "angularjs", src = "js/lib/angular.min.js"),
    // services and controllers js are AMD modules, required by staging.js
    @Script(id = "services", src = "js/services.js"),
    @Script(id = "controllers", src = "js/controllers.js"),
    @Script(id = "staging", src = "js/staging.js")
  },
  stylesheets = {
    @Stylesheet(src = "style/staging.css", location = AssetLocation.APPLICATION)
  }

)
@Less("style/staging.less")

package org.exoplatform.management.portlet;

import juzu.Application;
import juzu.asset.AssetLocation;
import juzu.plugin.asset.Assets;
import juzu.plugin.asset.Script;
import juzu.plugin.asset.Stylesheet;
import juzu.plugin.binding.Binding;
import juzu.plugin.binding.Bindings;
import juzu.plugin.less.Less;
import juzu.plugin.portlet.Portlet;

import org.exoplatform.management.service.api.ChromatticService;
import org.exoplatform.management.service.api.StagingService;
import org.exoplatform.management.service.api.SynchronizationService;
import org.exoplatform.services.jcr.RepositoryService;
import org.gatein.management.api.ManagementService;
import org.gatein.management.api.controller.ManagementController;

