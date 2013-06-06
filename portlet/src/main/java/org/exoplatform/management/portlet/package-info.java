@Portlet
@Application(name = "StagingExtension")
@Bindings({ @Binding(value = ManagementController.class), @Binding(value = StagingExtension.class, implementation = StagingExtensionImpl.class) })
package org.exoplatform.management.portlet;

import juzu.Application;
import juzu.plugin.binding.Binding;
import juzu.plugin.binding.Bindings;
import juzu.plugin.portlet.Portlet;
import org.gatein.management.api.controller.ManagementController;