package org.exoplatform.management.ecmadmin.operations.nodetype;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

import javax.jcr.NamespaceRegistry;

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

/**
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class NamespacesExportTask implements ExportTask {

  private NamespaceRegistry namespaceRegistry = null;
  private String type;

  public NamespacesExportTask(NamespaceRegistry namespaceRegistry, String type) {
    this.namespaceRegistry = namespaceRegistry;
    this.type = type;
  }

  @Override
  public String getEntry() {
    return type + "/jcr-namespaces-configuration.xml";
  }

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