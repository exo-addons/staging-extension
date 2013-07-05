package org.exoplatform.extension.generator.service.handler;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.xml.ComponentPlugin;
import org.exoplatform.container.xml.ExternalComponentPlugins;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ObjectParameter;
import org.exoplatform.extension.generator.service.api.AbstractConfigurationHandler;
import org.exoplatform.extension.generator.service.api.ExtensionGenerator;
import org.exoplatform.extension.generator.service.api.Utils;
import org.exoplatform.management.ecmadmin.operations.taxonomy.TaxonomyMetaData;
import org.exoplatform.services.cms.actions.ActionServiceContainer;
import org.exoplatform.services.cms.actions.impl.ActionConfig;
import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.services.cms.taxonomy.TaxonomyService;
import org.exoplatform.services.cms.taxonomy.impl.TaxonomyConfig;
import org.exoplatform.services.cms.taxonomy.impl.TaxonomyConfig.Permission;
import org.exoplatform.services.cms.taxonomy.impl.TaxonomyConfig.Taxonomy;
import org.exoplatform.services.cms.taxonomy.impl.TaxonomyPlugin;
import org.exoplatform.services.deployment.WCMContentInitializerService;
import org.exoplatform.services.deployment.plugins.LinkDeploymentDescriptor;
import org.exoplatform.services.deployment.plugins.LinkDeploymentPlugin;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.access.AccessControlEntry;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import com.thoughtworks.xstream.XStream;

public class TaxonomyConfigurationHandler extends AbstractConfigurationHandler {
  private static final String EXO_TAXONOMY = "exo:taxonomy";
  private static final String EXO_TAXONOMY_ACTION = "exo:taxonomyAction";
  private static final String WCM_TAXONOMY_CONFIGURATION_LOCATION = "WEB-INF/conf/custom-extension/wcm/";
  private static final String WCM_TAXONOMY_CONFIGURATION_NAME = "taxonomy-configuration.xml";
  private static final List<String> configurationPaths = new ArrayList<String>();
  static {
    configurationPaths.add(WCM_TAXONOMY_CONFIGURATION_LOCATION.replace("WEB-INF", "war:") + WCM_TAXONOMY_CONFIGURATION_NAME);
  }
  private static LinkManager linkManager = null;
  private static RepositoryService repositoryService = null;
  private static ActionServiceContainer actionServiceContainer = null;

  private Log log = ExoLogger.getLogger(this.getClass());

  public TaxonomyConfigurationHandler() {
    linkManager = (LinkManager) PortalContainer.getInstance().getComponentInstanceOfType(LinkManager.class);
    repositoryService = (RepositoryService) PortalContainer.getInstance().getComponentInstanceOfType(RepositoryService.class);
    actionServiceContainer = (ActionServiceContainer) PortalContainer.getInstance().getComponentInstanceOfType(ActionServiceContainer.class);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean writeData(ZipOutputStream zos, Collection<String> selectedResources) {
    Set<String> filteredSelectedResources = filterSelectedResources(selectedResources, ExtensionGenerator.ECM_TAXONOMY_PATH);
    if (filteredSelectedResources.isEmpty()) {
      return false;
    }

    List<TaxonomyMetaData> taxonomiesMetaData = new ArrayList<TaxonomyMetaData>();
    try {
      for (String filteredResource : filteredSelectedResources) {
        ZipFile zipFile = getExportedFileFromOperation(filteredResource);

        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        while (entries.hasMoreElements()) {
          ZipEntry zipEntry = (ZipEntry) entries.nextElement();
          try {
            InputStream inputStream = zipFile.getInputStream(zipEntry);
            if (zipEntry.getName().endsWith("metadata.xml")) {
              XStream xStream = new XStream();
              xStream.alias("metadata", TaxonomyMetaData.class);
              InputStreamReader isr = new InputStreamReader(inputStream, "UTF-8");
              TaxonomyMetaData taxonomyMetaData = (TaxonomyMetaData) xStream.fromXML(isr);
              taxonomiesMetaData.add(taxonomyMetaData);
            }
          } catch (Exception e) {
            log.error("Error while serializing Taxonomy Configuration data", e);
            return false;
          }
        }
      }
    } finally {
      clearTempFiles();
    }

    ExternalComponentPlugins taxonomyExternalComponentPlugin = new ExternalComponentPlugins();
    for (TaxonomyMetaData taxonomyMetaData : taxonomiesMetaData) {
      InitParams params = new InitParams();
      ComponentPlugin plugin = createComponentPlugin(taxonomyMetaData.getTaxoTreeName() + " TaxonomyPlugin", TaxonomyPlugin.class.getName(), "addTaxonomyPlugin", params);
      addComponentPlugin(taxonomyExternalComponentPlugin, TaxonomyService.class.getName(), plugin);

      params.addParam(getValueParam("autoCreateInNewRepository", "false"));
      params.addParam(getValueParam("workspace", taxonomyMetaData.getTaxoTreeWorkspace()));
      String treeHomePath = taxonomyMetaData.getTaxoTreeHomePath();
      int lastindex = treeHomePath.lastIndexOf("/" + taxonomyMetaData.getTaxoTreeName());
      if(lastindex > 0) {
        if(treeHomePath.length() == (lastindex + taxonomyMetaData.getTaxoTreeName().length() + 1)) {
          treeHomePath = treeHomePath.substring(0, lastindex);
        }
      }
      params.addParam(getValueParam("path", treeHomePath));
      params.addParam(getValueParam("treeName", taxonomyMetaData.getTaxoTreeName()));
      {
        ActionConfig actionConfig = new ActionConfig();
        actionConfig.setActions(getActions(taxonomyMetaData.getTaxoTreeHomePath(), taxonomyMetaData.getTaxoTreeWorkspace()));
        ObjectParameter actionObjectParameter = new ObjectParameter();
        actionObjectParameter.setName("predefined.actions");
        actionObjectParameter.setObject(actionConfig);
        addParameter(plugin, actionObjectParameter);
      }
      {
        TaxonomyConfig permissionConfig = new TaxonomyConfig();
        Taxonomy permissionTaxonomy = new Taxonomy();
        List<Taxonomy> permissionTaxonomyList = new ArrayList<TaxonomyConfig.Taxonomy>();
        permissionTaxonomyList.add(permissionTaxonomy);

        permissionConfig.setTaxonomies(permissionTaxonomyList);
        permissionTaxonomy.setPermissions(getPermissions(taxonomyMetaData.getPermissions()));
        ObjectParameter permObjectParameter = new ObjectParameter();
        permObjectParameter.setName("permission.configuration");
        permObjectParameter.setObject(permissionConfig);
        addParameter(plugin, permObjectParameter);
      }
      {
        List<Taxonomy> subNodes = null;
        try {
          subNodes = getTaxonomyTreeNodes(taxonomyMetaData.getTaxoTreeHomePath(), taxonomyMetaData.getTaxoTreeWorkspace());
        } catch (Exception e) {
          log.error("Error while serializing Taxonomy data", e);
          continue;
        }
        TaxonomyConfig taxonomyConfig = new TaxonomyConfig();
        taxonomyConfig.setTaxonomies(subNodes);
        ObjectParameter taxonomyobjectParameter = new ObjectParameter();
        taxonomyobjectParameter.setName("taxonomy.configuration");
        taxonomyobjectParameter.setObject(taxonomyConfig);
        addParameter(plugin, taxonomyobjectParameter);
      }
    }

    ExternalComponentPlugins linkExternalComponentPlugin = new ExternalComponentPlugins();
    for (TaxonomyMetaData taxonomyMetaData : taxonomiesMetaData) {
      InitParams params = new InitParams();
      ComponentPlugin plugin = createComponentPlugin(taxonomyMetaData.getTaxoTreeName() + " taxonomy links Initializer", LinkDeploymentPlugin.class.getName(), "addPlugin", params);
      addComponentPlugin(linkExternalComponentPlugin, WCMContentInitializerService.class.getName(), plugin);
      {
        List<LinkDeploymentDescriptor> linkDeploymentDescriptors = getSymLinksFromTreeNode(taxonomyMetaData.getTaxoTreeHomePath(), taxonomyMetaData.getTaxoTreeWorkspace());
        for (LinkDeploymentDescriptor linkDeploymentDescriptor : linkDeploymentDescriptors) {
          ObjectParameter linkObjectParameter = new ObjectParameter();
          linkObjectParameter.setName("" + linkObjectParameter.hashCode());
          linkObjectParameter.setObject(linkDeploymentDescriptor);
          addParameter(plugin, linkObjectParameter);
        }
      }
    }

    return Utils.writeConfiguration(zos, WCM_TAXONOMY_CONFIGURATION_LOCATION + WCM_TAXONOMY_CONFIGURATION_NAME, taxonomyExternalComponentPlugin, linkExternalComponentPlugin);
  }

  private List<LinkDeploymentDescriptor> getSymLinksFromTreeNode(String treeHomePath, String treeWorkspace) {
    SessionProvider sessionProvider = SessionProvider.createSystemProvider();
    List<LinkDeploymentDescriptor> descriptors = new ArrayList<LinkDeploymentDescriptor>();
    Session session = null;
    try {
      session = sessionProvider.getSession(treeWorkspace, repositoryService.getCurrentRepository());
      Node rootNode = (Node) session.getItem(treeHomePath);
      String repository = repositoryService.getCurrentRepository().getConfiguration().getName();
      computeLinkTreeNodes(repository, treeWorkspace, descriptors, rootNode);
    } catch (Exception e) {
      if (session != null && session.isLive()) {
        session.logout();
      }
    }
    return descriptors;
  }

  private void computeLinkTreeNodes(String repository, String workspace, List<LinkDeploymentDescriptor> descriptors, Node rootNode) throws Exception {
    NodeIterator nodeIterator = rootNode.getNodes();
    while (nodeIterator.hasNext()) {
      ExtendedNode childNode = (ExtendedNode) nodeIterator.nextNode();
      if (childNode.isNodeType(EXO_TAXONOMY)) {
        computeLinkTreeNodes(repository, workspace, descriptors, childNode);
      } else if (linkManager.isLink(childNode)) {
        LinkDeploymentDescriptor linkDeploymentDescriptor = new LinkDeploymentDescriptor();
        linkDeploymentDescriptor.setSourcePath(repository + ":" + workspace + ":" + linkManager.getTarget(childNode, true).getPath());
        linkDeploymentDescriptor.setTargetPath(repository + ":" + workspace + ":" + childNode.getParent().getPath());
        descriptors.add(linkDeploymentDescriptor);
      }
    }
  }

  private List<Taxonomy> getTaxonomyTreeNodes(String treeHomePath, String treeWorkspace) throws Exception {
    SessionProvider sessionProvider = SessionProvider.createSystemProvider();
    List<Taxonomy> taxonomies = new ArrayList<Taxonomy>();
    Session session = null;
    try {
      session = sessionProvider.getSession(treeWorkspace, repositoryService.getCurrentRepository());
      Node rootNode = (Node) session.getItem(treeHomePath);
      computeTaxonomyTreeNodes(taxonomies, rootNode, treeHomePath);
    } catch (Exception e) {
      if (session != null && session.isLive()) {
        session.logout();
      }
    }
    return taxonomies;
  }

  private void computeTaxonomyTreeNodes(List<Taxonomy> taxonomies, Node rootNode, String rootPath) throws Exception {
    NodeIterator nodeIterator = rootNode.getNodes();
    while (nodeIterator.hasNext()) {
      ExtendedNode childNode = (ExtendedNode) nodeIterator.nextNode();
      if (childNode.isNodeType(EXO_TAXONOMY)) {
        String childPath = childNode.getPath();
        childPath = childPath.replace(rootPath, "");
        childPath = childPath.replace(rootPath, "");
        List<AccessControlEntry> aclEntries = childNode.getACL().getPermissionEntries();
        List<Permission> permissionsList = getPermissions(aclEntries);
        Taxonomy taxonomy = new Taxonomy();
        taxonomy.setName(childPath);
        taxonomy.setPath(childPath);
        taxonomy.setPermissions(permissionsList);
        taxonomies.add(taxonomy);
        computeTaxonomyTreeNodes(taxonomies, childNode, rootPath);
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<String> getConfigurationPaths() {
    return configurationPaths;
  }

  @Override
  protected Log getLogger() {
    return log;
  }

  @SuppressWarnings("unchecked")
  private List<ActionConfig.TaxonomyAction> getActions(String rootNodePath, String workspaceName) {
    List<ActionConfig.TaxonomyAction> taxonomyActions = new ArrayList<ActionConfig.TaxonomyAction>();
    if (rootNodePath == null) {
      if (log.isDebugEnabled()) {
        log.warn("Argument srcNode is null.");
      }
      return taxonomyActions;
    }
    SessionProvider sessionProvider = SessionProvider.createSystemProvider();
    List<Node> actionNodes = null;
    try {
      Session session = sessionProvider.getSession(workspaceName, repositoryService.getCurrentRepository());
      Node rootNode = (Node) session.getItem(rootNodePath);
      actionNodes = actionServiceContainer.getActions(rootNode);
    } catch (Exception e1) {
      log.error("Error while retrieving Taxonomy Action informations", e1);
    }
    if (actionNodes == null) {
      return taxonomyActions;
    }
    for (Node node : actionNodes) {
      try {
        if (!node.isNodeType(EXO_TAXONOMY_ACTION)) {
          continue;
        }
        ActionConfig.TaxonomyAction action = new ActionConfig.TaxonomyAction();
        action.setName(getProperty(node, "exo:name"));
        action.setDescription(getProperty(node, "exo:description"));
        action.setLifecyclePhase(getPropertyValues(node, "exo:lifecyclePhase"));
        action.setHomePath(getProperty(node, "exo:storeHomePath"));
        action.setTargetPath(getProperty(node, "exo:targetPath"));
        action.setTargetWspace(getProperty(node, "exo:targetWorkspace"));
        List<String> roles = getPropertyValues(node, "exo:roles");
        action.setRoles(StringUtils.join(roles, ";"));
        action.setType(node.getPrimaryNodeType().getName());
        action.setMixins(new ArrayList<ActionConfig.Mixin>());
        NodeType[] mixinTypes = node.getMixinNodeTypes();
        for (NodeType mixinType : mixinTypes) {
          if (mixinType.getName().equals("mix:referenceable") || mixinType.getName().equals("exo:owneable")) {
            continue;
          }
          ActionConfig.Mixin mixin = new ActionConfig.Mixin();
          mixin.setName(mixinType.getName());
          String properties = getProperties(node, mixinType);
          if (properties.isEmpty()) {
            continue;
          }
          mixin.setProperties(properties);
          action.getMixins().add(mixin);
        }

        // Addt o list
        taxonomyActions.add(action);
      } catch (Exception e) {
        log.error("Error while retrieving Taxonomy Action informations", e);
      }
    }
    return taxonomyActions;
  }

  private String getProperties(Node node, NodeType mixinType) throws RepositoryException {
    StringBuilder builder = new StringBuilder();
    PropertyDefinition[] propertyDefinitions = mixinType.getPropertyDefinitions();
    for (PropertyDefinition propertyDefinition : propertyDefinitions) {
      if (node.hasProperty(propertyDefinition.getName()) && !propertyDefinition.isProtected()) {
        if (builder.length() > 0) {
          builder.append(";");
        }
        builder.append(propertyDefinition.getName());
        builder.append("=");
        if (propertyDefinition.isMultiple()) {
          List<String> values = getPropertyValues(node, propertyDefinition.getName());
          builder.append(StringUtils.join(values, ","));
        } else {
          builder.append(getProperty(node, propertyDefinition.getName()));
        }
      }
    }
    return builder.toString();
  }

  private String getProperty(Node node, String propertyName) throws RepositoryException {
    if (propertyName != null && node != null && node.hasProperty(propertyName)) {
      return node.getProperty(propertyName).getString();
    } else {
      return null;
    }
  }

  private List<String> getPropertyValues(Node node, String propertyName) throws RepositoryException {
    if (propertyName != null && node != null && node.hasProperty(propertyName)) {
      Value[] values = node.getProperty(propertyName).getValues();
      List<String> valuesString = new ArrayList<String>();
      for (Value value : values) {
        if (value != null) {
          valuesString.add(value.getString());
        }
      }
      return valuesString;
    } else {
      return null;
    }
  }

  private List<Permission> getPermissions(List<AccessControlEntry> aclEntries) {
    List<TaxonomyConfig.Permission> listPermissions = new ArrayList<TaxonomyConfig.Permission>();
    Map<String, List<String>> permissionsMap = new HashMap<String, List<String>>();
    for (AccessControlEntry aclEntry : aclEntries) {
      if (permissionsMap.get(aclEntry.getIdentity()) == null) {
        permissionsMap.put(aclEntry.getIdentity(), new ArrayList<String>());
      }
      permissionsMap.get(aclEntry.getIdentity()).add(aclEntry.getPermission());
    }

    Set<Map.Entry<String, List<String>>> permissionsEntries = permissionsMap.entrySet();
    for (Entry<String, List<String>> permEntry : permissionsEntries) {
      TaxonomyConfig.Permission permission = new TaxonomyConfig.Permission();
      permission.setIdentity((String) permEntry.getKey());
      permission.setRead("" + permEntry.getValue().contains(PermissionType.READ));
      permission.setAddNode("" + permEntry.getValue().contains(PermissionType.ADD_NODE));
      permission.setSetProperty("" + permEntry.getValue().contains(PermissionType.SET_PROPERTY));
      permission.setRemove("" + permEntry.getValue().contains(PermissionType.REMOVE));
      listPermissions.add(permission);
    }
    return listPermissions;
  }

  private List<Permission> getPermissions(String permissions) {
    List<TaxonomyConfig.Permission> listPermissions = new ArrayList<TaxonomyConfig.Permission>();
    if (permissions == null || permissions.trim().isEmpty()) {
      return listPermissions;
    }

    Map<String, List<String>> permissionsMap = new HashMap<String, List<String>>();
    String[] permEntries = permissions.split(";");
    for (String permExpr : permEntries) {
      String[] permExprEntries = permExpr.trim().split(" ");
      if (permissionsMap.get(permExprEntries[0]) == null) {
        permissionsMap.put(permExprEntries[0], new ArrayList<String>());
      }
      permissionsMap.get(permExprEntries[0]).add(permExprEntries[1]);
    }

    Set<Map.Entry<String, List<String>>> permissionsEntries = permissionsMap.entrySet();
    for (Entry<String, List<String>> permEntry : permissionsEntries) {
      TaxonomyConfig.Permission permission = new TaxonomyConfig.Permission();
      permission.setIdentity((String) permEntry.getKey());
      permission.setRead("" + permEntry.getValue().contains(PermissionType.READ));
      permission.setAddNode("" + permEntry.getValue().contains(PermissionType.ADD_NODE));
      permission.setSetProperty("" + permEntry.getValue().contains(PermissionType.SET_PROPERTY));
      permission.setRemove("" + permEntry.getValue().contains(PermissionType.REMOVE));
      listPermissions.add(permission);
    }
    return listPermissions;
  }

  public static boolean isSymLink(Node node) throws RepositoryException {
    return linkManager.isLink(node);
  }

}
