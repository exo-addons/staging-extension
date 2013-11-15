package org.exoplatform.management.ecmadmin.operations.nodetype;

import org.gatein.management.api.operation.model.ExportTask;

import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Collections;

/**
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class NodeTypeExportTask implements ExportTask {

  private NodeType nodeType = null;
  private String type;

  public NodeTypeExportTask(NodeType nodeType, String type) {
    this.nodeType = nodeType;
    this.type = type;
  }

  @Override
  public String getEntry() {
    return "ecmadmin/" + type + "/" + nodeType.getName().replace(":", "_") + "-nodeType.xml";
  }

  @Override
  public void export(OutputStream outputStream) throws IOException {
    try {
      String xmlContent = getNodeTypeXML(Collections.singleton(nodeType));
      outputStream.write(xmlContent.getBytes("UTF-8"));
    } catch (Exception exception) {
      throw new RuntimeException(exception);
    }
  }

  public static String getNodeTypeXML(Collection<NodeType> nodeTypes) {
    StringBuilder nodeTypeXML = new StringBuilder();
    nodeTypeXML.append("<nodeTypes xmlns:nt=").append("\"");
    nodeTypeXML.append("http://www.jcp.org/jcr/nt/1.5").append("\" ");
    nodeTypeXML.append("xmlns:mix=").append("\"");
    nodeTypeXML.append("http://www.jcp.org/jcr/mix/1.5").append("\" ");
    nodeTypeXML.append("xmlns:jcr=").append("\"").append("http://www.jcp.org/jcr/1.5");
    nodeTypeXML.append("\" >").append("\n");
    for (NodeType nodeType : nodeTypes) {

      nodeTypeXML.append("<nodeType ");
      nodeTypeXML.append("name=").append("\"").append(nodeType.getName()).append("\" ");
      String isMixIn = String.valueOf(nodeType.isMixin());
      nodeTypeXML.append("isMixin=").append("\"").append(String.valueOf(isMixIn)).append("\" ");
      String hasOrderable = String.valueOf(nodeType.hasOrderableChildNodes());
      nodeTypeXML.append("hasOrderableChildNodes=\"").append(hasOrderable).append("\" ");
      String primaryItemName = "";
      if (nodeType.getPrimaryItemName() != null)
        primaryItemName = nodeType.getPrimaryItemName();
      nodeTypeXML.append("primaryItemName=").append("\"").append(primaryItemName).append("\" >");
      nodeTypeXML.append("\n");
      // represent supertypes
      String representSuperType = representSuperTypes(nodeType);
      nodeTypeXML.append(representSuperType);
      // represent PropertiesDefinition
      String representPropertiesXML = representPropertyDefinition(nodeType);
      nodeTypeXML.append(representPropertiesXML);
      // represent ChildNodeDefinition
      String representChildXML = representChildNodeDefinition(nodeType);
      nodeTypeXML.append(representChildXML);
      nodeTypeXML.append("</nodeType>").append("\n");
    }
    nodeTypeXML.append("</nodeTypes>");
    return nodeTypeXML.toString();
  }

  public static String representSuperTypes(NodeType nodeType) {
    StringBuilder superTypeXML = new StringBuilder();
    NodeType[] superType = nodeType.getDeclaredSupertypes();
    if (superType != null && superType.length > 0) {
      superTypeXML.append("<supertypes>").append("\n");
      for (int i = 0; i < superType.length; i++) {
        String typeName = superType[i].getName();
        superTypeXML.append("<supertype>").append(typeName).append("</supertype>").append("\n");
      }
      superTypeXML.append("</supertypes>").append("\n");
    }
    return superTypeXML.toString();
  }

  public static String representPropertyDefinition(NodeType nodeType) {
    String[] requireType = { "undefined", "String", "Binary", "Long", "Double", "Date", "Boolean", "Name", "Path", "Reference" };
    String[] onparentVersion = { "", "COPY", "VERSION", "INITIALIZE", "COMPUTE", "IGNORE", "ABORT" };
    StringBuilder propertyXML = new StringBuilder();
    propertyXML.append("<propertyDefinitions>").append("\n");
    PropertyDefinition[] proDef = nodeType.getPropertyDefinitions();
    for (int j = 0; j < proDef.length; j++) {
      propertyXML.append("<propertyDefinition ");
      propertyXML.append("name=").append("\"").append(proDef[j].getName()).append("\" ");
      String requiredValue = null;
      if (proDef[j].getRequiredType() == 100)
        requiredValue = "Permission";
      else
        requiredValue = requireType[proDef[j].getRequiredType()];
      propertyXML.append("requiredType=").append("\"").append(requiredValue).append("\" ");
      String autoCreate = String.valueOf(proDef[j].isAutoCreated());
      propertyXML.append("autoCreated=").append("\"").append(autoCreate).append("\" ");
      String mandatory = String.valueOf(proDef[j].isMandatory());
      propertyXML.append("mandatory=").append("\"").append(mandatory).append("\" ");
      String onVersion = onparentVersion[proDef[j].getOnParentVersion()];
      propertyXML.append("onParentVersion=").append("\"").append(onVersion).append("\" ");
      String protect = String.valueOf(proDef[j].isProtected());
      propertyXML.append("protected=").append("\"").append(protect).append("\" ");
      String multiple = String.valueOf(proDef[j].isMultiple());
      propertyXML.append("multiple=").append("\"").append(multiple).append("\" >").append("\n");
      String[] constraints = proDef[j].getValueConstraints();
      if (constraints != null && constraints.length > 0) {
        propertyXML.append("<valueConstraints>").append("\n");
        for (int k = 0; k < constraints.length; k++) {
          String cons = constraints[k].toString();
          propertyXML.append("<valueConstraint>").append(cons).append("</valueConstraint>");
          propertyXML.append("\n");
        }
        propertyXML.append("</valueConstraints>").append("\n");
      } else {
        propertyXML.append("<valueConstraints/>").append("\n");
      }
      propertyXML.append("</propertyDefinition>").append("\n");
    }
    propertyXML.append("</propertyDefinitions>").append("\n");
    return propertyXML.toString();
  }

  public static String representChildNodeDefinition(NodeType nodeType) {
    String[] onparentVersion = { "", "COPY", "VERSION", "INITIALIZE", "COMPUTE", "IGNORE", "ABORT" };
    StringBuilder childNodeXML = new StringBuilder();
    NodeDefinition[] childDef = nodeType.getChildNodeDefinitions();
    if (childDef != null && childDef.length > 0) {
      childNodeXML.append("<childNodeDefinitions>").append("\n");
      for (int j = 0; j < childDef.length; j++) {
        childNodeXML.append("<childNodeDefinition ");
        childNodeXML.append("name=").append("\"").append(childDef[j].getName()).append("\" ");
        NodeType defaultType = childDef[j].getDefaultPrimaryType();
        if (defaultType != null) {
          String defaultName = defaultType.getName();
          childNodeXML.append("defaultPrimaryType=").append("\"").append(defaultName).append("\" ");
        } else {
          childNodeXML.append("defaultPrimaryType=").append("\"").append("\" ");
        }
        String autoCreate = String.valueOf(childDef[j].isAutoCreated());
        childNodeXML.append("autoCreated=").append("\"").append(autoCreate).append("\" ");
        String mandatory = String.valueOf(childDef[j].isMandatory());
        childNodeXML.append("mandatory=").append("\"").append(mandatory).append("\" ");
        String onVersion = onparentVersion[childDef[j].getOnParentVersion()];
        childNodeXML.append("onParentVersion=").append("\"").append(onVersion).append("\" ");
        String protect = String.valueOf(childDef[j].isProtected());
        childNodeXML.append("protected=").append("\"").append(protect).append("\" ");
        String sameName = String.valueOf(childDef[j].allowsSameNameSiblings());
        childNodeXML.append("sameNameSiblings=").append("\"").append(sameName).append("\" >");
        childNodeXML.append("\n");
        NodeType[] requiredType = childDef[j].getRequiredPrimaryTypes();
        if (requiredType != null && requiredType.length > 0) {
          childNodeXML.append("<requiredPrimaryTypes>").append("\n");
          for (int k = 0; k < requiredType.length; k++) {
            String requiredName = requiredType[k].getName();
            childNodeXML.append("<requiredPrimaryType>").append(requiredName);
            childNodeXML.append("</requiredPrimaryType>").append("\n");
          }
          childNodeXML.append("</requiredPrimaryTypes>").append("\n");
        }
        childNodeXML.append("</childNodeDefinition>").append("\n");
      }
      childNodeXML.append("</childNodeDefinitions>").append("\n");
    }
    return childNodeXML.toString();
  }
}