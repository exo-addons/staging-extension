package org.exoplatform.extension.generator.service;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipFile;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.xml.ComponentPlugin;
import org.exoplatform.container.xml.ExternalComponentPlugins;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.Parameter;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.extension.generator.service.api.ConfigurationHandler;
import org.exoplatform.services.log.Log;
import org.gatein.management.api.ContentType;
import org.gatein.management.api.PathAddress;
import org.gatein.management.api.controller.ManagedRequest;
import org.gatein.management.api.controller.ManagedResponse;
import org.gatein.management.api.controller.ManagementController;
import org.gatein.management.api.operation.OperationNames;

public abstract class AbstractConfigurationHandler implements ConfigurationHandler {
  protected static final String DMS_CONFIGURATION_LOCATION = "WEB-INF/conf/custom-extension/dms/";

  private ManagementController managementController = null;

  protected abstract Log getLogger();

  protected ZipFile getExportedFileFromOperation(String path, String... filters) {
    ManagedRequest request = null;
    if (filters != null && filters.length > 0) {
      Map<String, List<String>> attributes = new HashMap<String, List<String>>();
      List<String> filtersList = Arrays.asList(filters);
      attributes.put("filter", filtersList);
      request = ManagedRequest.Factory.create(OperationNames.EXPORT_RESOURCE, PathAddress.pathAddress(path), attributes, ContentType.ZIP);
    } else {
      request = ManagedRequest.Factory.create(OperationNames.EXPORT_RESOURCE, PathAddress.pathAddress(path), ContentType.ZIP);
    }
    try {
      ManagedResponse response = getManagementController().execute(request);
      File tmpFile = File.createTempFile("exo", "-extension-generator");
      FileOutputStream outputStream = new FileOutputStream(tmpFile);
      response.writeResult(outputStream);
      outputStream.flush();
      outputStream.close();
      return new ZipFile(tmpFile);
    } catch (Exception e) {
      throw new RuntimeException("Error while handling Response from GateIN Management, export operation", e);
    }
  }

  protected Set<String> filterSelectedResources(Set<String> selectedResources, String parentPath) {
    Set<String> filteredSelectedResources = new HashSet<String>();
    for (String resourcePath : selectedResources) {
      if (resourcePath.contains(parentPath)) {
        filteredSelectedResources.add(resourcePath);
      }
    }
    return filteredSelectedResources;
  }

  protected void addComponentPlugin(ExternalComponentPlugins externalComponentPlugins, String componentKey, ComponentPlugin componentPlugin) {
    externalComponentPlugins.setTargetComponent(componentKey);
    if (externalComponentPlugins.getComponentPlugins() == null) {
      externalComponentPlugins.setComponentPlugins(new ArrayList<ComponentPlugin>());
    }
    externalComponentPlugins.getComponentPlugins().add(componentPlugin);
  }

  protected ComponentPlugin createComponentPlugin(String name, String type, String methodName, InitParams params, Parameter... parameters) {
    ComponentPlugin plugin = new ComponentPlugin();
    plugin.setName(name);
    plugin.setSetMethod(methodName);
    plugin.setType(type);
    plugin.setInitParams(params);
    addParameter(plugin, parameters);
    return plugin;
  }

  protected void addParameter(ComponentPlugin plugin, Parameter... parameters) {
    InitParams params = plugin.getInitParams();
    if (params == null) {
      params = new InitParams();
      plugin.setInitParams(params);
    }
    if (parameters != null && parameters.length > 0) {
      for (Parameter parameter : parameters) {
        params.addParameter(parameter);
      }
    }
  }

  protected ValueParam getValueParam(String name, String value) {
    ValueParam valueParam = new ValueParam();
    valueParam.setName(name);
    valueParam.setValue(value);
    return valueParam;
  }

  protected ManagementController getManagementController() {
    if (managementController == null) {
      managementController = (ManagementController) PortalContainer.getInstance().getComponentInstanceOfType(ManagementController.class);
    }
    return managementController;
  }
}
