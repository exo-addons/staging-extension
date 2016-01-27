@Portlet
@Application(name = "StagingExtension")
@Bindings({ @Binding(value = ManagementController.class), @Binding(value = ManagementService.class), @Binding(value = StagingService.class), @Binding(value = SynchronizationService.class),
    @Binding(value = RepositoryService.class) })
@Scripts({ @Script(id = "jQueryUI", value = "js/lib/jquery-ui.js"), @Script(id = "jQueryFileDownload", value = "js/lib/jquery.fileDownload.js"),
    // AngularJS is still global, should be AMDified
    @Script(id = "angularjs", value = "js/lib/angular.min.js"), @Script(id = "ngSanitize", value = "js/lib/angular-sanitize.js", depends = "angularjs"),
    // services and controllers js are AMD modules, required by staging.js
    @Script(id = "services", value = "js/services.js", depends = "angularjs"), @Script(id = "controllers", value = "js/controllers.js", depends = { "angularjs" }),
    @Script(id = "staging", value = "js/staging.js", depends = { "controllers", "services" }) })
@Less("style/StagingSkin.less")
@Stylesheets({ @Stylesheet(id = "stagingSkin", value = "style/staging.css"), @Stylesheet(id = "jQueryUISkin", value = "style/jquery-ui.css") })
@Assets("*")
package org.exoplatform.management.portlet;

import juzu.Application;
import juzu.plugin.asset.Assets;
import juzu.plugin.asset.Script;
import juzu.plugin.asset.Scripts;
import juzu.plugin.asset.Stylesheet;
import juzu.plugin.asset.Stylesheets;
import juzu.plugin.binding.Binding;
import juzu.plugin.binding.Bindings;
import juzu.plugin.less.Less;
import juzu.plugin.portlet.Portlet;

import org.exoplatform.management.service.api.StagingService;
import org.exoplatform.management.service.api.SynchronizationService;
import org.exoplatform.services.jcr.RepositoryService;
import org.gatein.management.api.ManagementService;
import org.gatein.management.api.controller.ManagementController;

