package org.exoplatform.management.ecmadmin.operations.nodetype;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.jcr.AccessDeniedException;
import javax.jcr.NamespaceException;
import javax.jcr.NamespaceRegistry;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;

import org.exoplatform.container.xml.ComponentPlugin;
import org.exoplatform.container.xml.Configuration;
import org.exoplatform.container.xml.ExternalComponentPlugins;
import org.exoplatform.container.xml.PropertiesParam;
import org.exoplatform.container.xml.Property;
import org.exoplatform.management.ecmadmin.operations.ECMAdminImportResource;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.nodetype.ExtendedNodeTypeManager;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeValue;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeValuesList;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.NoResultModel;
import org.jibx.runtime.BindingDirectory;
import org.jibx.runtime.IBindingFactory;
import org.jibx.runtime.IUnmarshallingContext;
import org.jibx.runtime.JiBXException;

/**
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class NodeTypeImportResource extends ECMAdminImportResource {
  private static final Log log = ExoLogger.getLogger(NodeTypeImportResource.class);
  private RepositoryService repositoryService;
  private String pathPrefix = null;

  public NodeTypeImportResource(String pathPrefix) {
    super(null);
    this.pathPrefix = pathPrefix + "/";
  }

  public NodeTypeImportResource(String pathPrefix, String filePath) {
    super(filePath);
    this.pathPrefix = pathPrefix + "/";
  }

  @Override
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws OperationException {
    // get attributes and attachement inputstream
    super.execute(operationContext, resultHandler);

    if (repositoryService == null) {
      repositoryService = operationContext.getRuntimeContext().getRuntimeComponent(RepositoryService.class);
    }
    try {
      ExtendedNodeTypeManager extManager = (ExtendedNodeTypeManager) repositoryService.getCurrentRepository()
          .getNodeTypeManager();

      if (replaceExisting) {
        log.info("Overwrite '" + pathPrefix.substring(0, pathPrefix.length() - 1)
            + "' behavior isn't safe, ignoring existing nodetypes.");
      }
      ZipInputStream zin = new ZipInputStream(attachmentInputStream);
      ZipEntry ze = null;
      List<NodeTypeValue> nodeTypeValues = new ArrayList<NodeTypeValue>();
      while ((ze = zin.getNextEntry()) != null) {
        if (!ze.getName().startsWith(pathPrefix)) {
          continue;
        }
        if (ze.getName().endsWith("-nodeType.xml")) {
          IBindingFactory factory = BindingDirectory.getFactory(NodeTypeValuesList.class);
          IUnmarshallingContext uctx = factory.createUnmarshallingContext();
          NodeTypeValuesList nodeTypeValuesList = (NodeTypeValuesList) uctx.unmarshalDocument(zin, null);
          ArrayList<?> ntvList = nodeTypeValuesList.getNodeTypeValuesList();
          for (Object object : ntvList) {
            nodeTypeValues.add((NodeTypeValue) object);
          }
        } else if (ze.getName().endsWith("namespaces-configuration.xml")) {
          // Import namespaces
          registerNamespaces(zin);
        }
        zin.closeEntry();
      }
      zin.close();

      // Import nodetypes after import namespaces is done
      for (NodeTypeValue nodeTypeValue : nodeTypeValues) {
        // REPLACE_IF_EXISTS behavior isn't safe, so use this one
        extManager.registerNodeType(nodeTypeValue, ExtendedNodeTypeManager.IGNORE_IF_EXISTS);
      }

      resultHandler.completed(NoResultModel.INSTANCE);
    } catch (Exception exception) {
      throw new OperationException(OperationNames.IMPORT_RESOURCE, "Error while importing nodetypes.", exception);
    }
  }

  private void registerNamespaces(ZipInputStream zin) throws JiBXException, RepositoryException, NamespaceException,
      UnsupportedRepositoryOperationException, AccessDeniedException {
    IBindingFactory bfact = BindingDirectory.getFactory(Configuration.class);
    IUnmarshallingContext uctx = bfact.createUnmarshallingContext();

    Configuration configuration = (Configuration) uctx.unmarshalDocument(zin, "UTF-8");
    ExternalComponentPlugins externalComponentPlugins = configuration.getExternalComponentPlugins(RepositoryService.class
        .getName());
    List<ComponentPlugin> componentPlugins = externalComponentPlugins.getComponentPlugins();
    if (componentPlugins == null || componentPlugins.isEmpty()) {
      log.warn("Wrong Namespaces configuration, no namespace will be imported");
      return;
    }
    ComponentPlugin componentPlugin = componentPlugins.get(0);
    PropertiesParam propertiesParam = componentPlugin.getInitParams().getPropertiesParam("namespaces");
    if (propertiesParam == null || propertiesParam.getProperties().isEmpty()) {
      log.warn("Wrong Namespaces configuration, no namespace will be imported");
      return;
    }

    NamespaceRegistry namespaceRegistry = repositoryService.getCurrentRepository().getNamespaceRegistry();

    Iterator<Property> propertiesIterator = propertiesParam.getPropertyIterator();
    while (propertiesIterator.hasNext()) {
      Property property = (Property) propertiesIterator.next();
      String namespacePrefix = property.getName();
      String namespaceURI = null;
      try {
        namespaceURI = namespaceRegistry.getURI(namespacePrefix);
      } catch (Exception exception) {
        // Nothing to do
      }
      if (namespaceURI == null) {
        namespaceURI = property.getValue();
        namespaceRegistry.registerNamespace(namespacePrefix, namespaceURI);
      }
    }
  }
}
