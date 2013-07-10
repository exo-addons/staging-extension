@Portlet
@Application(name = "Synchronization")
@Bindings({ @Binding(value = ManagementController.class), @Binding(value = SynchronizationService.class, implementation = SynchronizationServiceImpl.class) })
package org.exoplatform.extension.synchronization.portlet;

import juzu.Application;
import juzu.plugin.binding.Binding;
import juzu.plugin.binding.Bindings;
import juzu.plugin.portlet.Portlet;

import org.exoplatform.extension.synchronization.service.SynchronizationServiceImpl;
import org.exoplatform.extension.synchronization.service.api.SynchronizationService;
import org.gatein.management.api.controller.ManagementController;