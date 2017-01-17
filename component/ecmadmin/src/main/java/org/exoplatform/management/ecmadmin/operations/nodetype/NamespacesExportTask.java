/*
 * Copyright (C) 2003-2017 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.exoplatform.management.ecmadmin.operations.nodetype;

import org.exoplatform.container.xml.ComponentPlugin;
import org.exoplatform.container.xml.Configuration;
import org.exoplatform.container.xml.ExternalComponentPlugins;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.PropertiesParam;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.impl.AddNamespacesPlugin;
import org.gatein.management.api.operation.model.ExportTask;
import org.jibx.runtime.BindingDirectory;
import org.jibx.runtime.IBindingFactory;
import org.jibx.runtime.IMarshallingContext;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

import javax.jcr.NamespaceRegistry;

/**
 * The Class NamespacesExportTask.
 *
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class NamespacesExportTask implements ExportTask {

  /** The namespace registry. */
  private NamespaceRegistry namespaceRegistry = null;
  
  /** The type. */
  private String type;

  /**
   * Instantiates a new namespaces export task.
   *
   * @param namespaceRegistry the namespace registry
   * @param type the type
   */
  public NamespacesExportTask(NamespaceRegistry namespaceRegistry, String type) {
    this.namespaceRegistry = namespaceRegistry;
    this.type = type;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getEntry() {
    return "ecmadmin/" + type + "/jcr-namespaces-configuration.xml";
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void export(OutputStream outputStream) throws IOException {
    try {
      ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream();

      IBindingFactory bfact = BindingDirectory.getFactory(Configuration.class);
      IMarshallingContext mctx = bfact.createMarshallingContext();
      mctx.setIndent(2);
      mctx.marshalDocument(getNamespacesConfiguration(), "UTF-8", false, arrayOutputStream);

      // Use ByteArrayOutputStream because the outputStream have to be
      // open, but 'marshalDocument' closes automatically the stream
      outputStream.write(arrayOutputStream.toByteArray());
    } catch (Exception exception) {
      throw new RuntimeException(exception);
    }
  }

  /**
   * Gets the namespaces configuration.
   *
   * @return the namespaces configuration
   * @throws Exception the exception
   */
  private Configuration getNamespacesConfiguration() throws Exception {
    Configuration configuration = new Configuration();

    ExternalComponentPlugins externalComponentPlugins = new ExternalComponentPlugins();
    externalComponentPlugins.setTargetComponent(RepositoryService.class.getName());
    ArrayList<ComponentPlugin> componentPlugins = new ArrayList<ComponentPlugin>();
    externalComponentPlugins.setComponentPlugins(componentPlugins);

    ComponentPlugin componentPlugin = new ComponentPlugin();
    componentPlugin.setName("add.namespaces");
    componentPlugin.setSetMethod("addPlugin");
    componentPlugin.setType(AddNamespacesPlugin.class.getName());

    InitParams initParams = new InitParams();
    componentPlugin.setInitParams(initParams);
    componentPlugins.add(componentPlugin);
    PropertiesParam propertiesParam = new PropertiesParam();
    propertiesParam.setName("namespaces");
    initParams.addParam(propertiesParam);

    String[] namespacePrefixes = namespaceRegistry.getPrefixes();
    for (String namespacePrefix : namespacePrefixes) {
      String namespaceURI = namespaceRegistry.getURI(namespacePrefix);
      propertiesParam.setProperty(namespacePrefix, namespaceURI);
    }
    configuration.addExternalComponentPlugins(externalComponentPlugins);
    return configuration;
  }

}