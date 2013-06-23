@Portlet
@Application(name = "ExtensionGenerator")
@Bindings({ @Binding(value = ManagementController.class), @Binding(value = ExtensionGenerator.class, implementation = ExtensionGeneratorImpl.class) })
package org.exoplatform.extension.generator.portlet;

import juzu.Application;
import juzu.plugin.binding.Binding;
import juzu.plugin.binding.Bindings;
import juzu.plugin.portlet.Portlet;

import org.exoplatform.extension.generator.service.ExtensionGeneratorImpl;
import org.exoplatform.extension.generator.service.api.ExtensionGenerator;
import org.gatein.management.api.controller.ManagementController;
