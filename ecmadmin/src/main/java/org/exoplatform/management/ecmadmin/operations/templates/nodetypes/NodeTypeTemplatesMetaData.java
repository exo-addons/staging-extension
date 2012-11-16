package org.exoplatform.management.ecmadmin.operations.templates.nodetypes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.exoplatform.management.ecmadmin.operations.templates.NodeTemplate;

public class NodeTypeTemplatesMetaData {

  private String label;
  private boolean documentTemplate;
  private Map<String, List<NodeTemplate>> templates;

  public NodeTypeTemplatesMetaData() {
    this.templates = new HashMap<String, List<NodeTemplate>>();
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public boolean isDocumentTemplate() {
    return documentTemplate;
  }

  public void setDocumentTemplate(boolean documentTemplate) {
    this.documentTemplate = documentTemplate;
  }

  public Map<String, List<NodeTemplate>> getTemplates() {
    return templates;
  }

  public void setTemplates(Map<String, List<NodeTemplate>> templates) {
    this.templates = templates;
  }

  public void addTemplate(String type, NodeTemplate template) {
    List<NodeTemplate> typeTemplates = templates.get(type);
    if (typeTemplates == null) {
      typeTemplates = new ArrayList<NodeTemplate>();
    }

    typeTemplates.add(template);
    templates.put(type, typeTemplates);
  }

}
