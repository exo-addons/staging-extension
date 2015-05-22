package org.exoplatform.management.ecmadmin.operations.templates.nodetypes;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.jcr.PathNotFoundException;

import org.apache.commons.io.IOUtils;
import org.exoplatform.management.ecmadmin.operations.ECMAdminImportResource;
import org.exoplatform.management.ecmadmin.operations.templates.NodeTemplate;
import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.services.jcr.RepositoryService;
import org.gatein.common.logging.Logger;
import org.gatein.common.logging.LoggerFactory;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.NoResultModel;

import com.thoughtworks.xstream.XStream;

/**
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class NodeTypesTemplatesImportResource extends ECMAdminImportResource {
  final private static Logger log = LoggerFactory.getLogger(NodeTypesTemplatesImportResource.class);
  private TemplateService templateService;
  private RepositoryService repositoryService;

  private Pattern templateEntryPattern = Pattern.compile("ecmadmin/templates/nodetypes/(.*)/(.*)/(.*)\\.gtmpl");
  private Pattern metadataEntryPattern = Pattern.compile("ecmadmin/templates/nodetypes/(.*)/metadata.xml");

  private Map<String, NodeTypeTemplatesMetaData> metadatas = new HashMap<String, NodeTypeTemplatesMetaData>();
  private Map<String, byte[]> templatesContent = new HashMap<String, byte[]>();

  public NodeTypesTemplatesImportResource() {
    super(null);
  }

  public NodeTypesTemplatesImportResource(String filePath) {
    super(filePath);
  }

  @Override
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws OperationException {
    // get attributes and attachement inputstream
    super.execute(operationContext, resultHandler);

    if (templateService == null) {
      templateService = operationContext.getRuntimeContext().getRuntimeComponent(TemplateService.class);
      if (templateService == null) {
        throw new OperationException(OperationNames.IMPORT_RESOURCE, "TemplateService doesn't exist.");
      }
    }
    if (repositoryService == null) {
      repositoryService = operationContext.getRuntimeContext().getRuntimeComponent(RepositoryService.class);
      if (repositoryService == null) {
        throw new OperationException(OperationNames.IMPORT_RESOURCE, "RepositoryService doesn't exist.");
      }
    }

    metadatas.clear();

    try {
      final ZipInputStream zis = new ZipInputStream(attachmentInputStream);
      ZipEntry entry;
      while ((entry = zis.getNextEntry()) != null) {
        String filePath = entry.getName();
        if (!filePath.startsWith("ecmadmin/templates/nodetypes/")) {
          continue;
        }
        // Skip directories
        // & Skip empty entries
        // & Skip entries not in sites/zip
        if (entry.isDirectory() || filePath.equals("") || !(filePath.endsWith(".gtmpl") || filePath.endsWith(".xml"))) {
          continue;
        }
        Matcher matcher = metadataEntryPattern.matcher(filePath);
        if (matcher.find()) {
          // read NT Templates Metadata
          String nodeTypeName = matcher.group(1);

          XStream xStream = new XStream();
          xStream.alias("metadata", NodeTypeTemplatesMetaData.class);
          xStream.alias("template", NodeTemplate.class);
          NodeTypeTemplatesMetaData metadata = (NodeTypeTemplatesMetaData) xStream.fromXML(new InputStreamReader(zis));

          metadatas.put(nodeTypeName, metadata);
          continue;
        }

        matcher = templateEntryPattern.matcher(filePath);
        if (!matcher.find()) {
          continue;
        }

        String nodeTypeName = matcher.group(1);
        String templateType = matcher.group(2);
        String templateName = matcher.group(3);
        if (!metadatas.containsKey(nodeTypeName)) {
          putData(nodeTypeName, templateType, templateName + ".gtmpl", zis);
          continue;
        }
        updateTemplateContent(templateType, nodeTypeName, templateName, replaceExisting, zis);
        zis.closeEntry();
      }
      zis.close();

      Iterator<Map.Entry<String, byte[]>> templatesContentIterator = templatesContent.entrySet().iterator();
      while (templatesContentIterator.hasNext()) {
        Map.Entry<java.lang.String, byte[]> templateContentEntry = (Map.Entry<java.lang.String, byte[]>) templatesContentIterator.next();

        Matcher matcher = templateEntryPattern.matcher(templateContentEntry.getKey());
        if (!matcher.find()) {
          continue;
        }

        String nodeTypeName = matcher.group(1);
        String templateType = matcher.group(2);
        String templateName = matcher.group(3);
        updateTemplateContent(templateType, nodeTypeName, templateName, replaceExisting, new ByteArrayInputStream(templateContentEntry.getValue()));
      }
    } catch (Exception e) {
      throw new OperationException(OperationNames.IMPORT_RESOURCE, "Error while importing nodetype templates", e);
    }

    resultHandler.completed(NoResultModel.INSTANCE);
  }

  private void updateTemplateContent(String templateType, String nodeTypeName, String templateName, boolean replaceExisting, InputStream inputStream) throws Exception {
    String templateContent;
    try {
      templateContent = templateService.getTemplate(templateType, nodeTypeName, templateName);
    } catch (PathNotFoundException exception) {
      templateContent = null;
    }
    if (templateContent != null) {
      if (replaceExisting) {
        log.info("Overwrite existing nodetype template '" + nodeTypeName + "/" + templateType + "/" + templateName + "'.");
      } else {
        log.info("Ignore existing nodetype template '" + nodeTypeName + "/" + templateType + "/" + templateName + "'.");
        return;
      }
    }
    NodeTypeTemplatesMetaData nodeTypeTemplatesMetaData = metadatas.get(nodeTypeName);
    Iterator<NodeTemplate> nodeTemplates = nodeTypeTemplatesMetaData.getTemplates().get(templateType).iterator();
    String[] roles = new String[1];
    while (nodeTemplates.hasNext() && roles[0] == null) {
      NodeTemplate nodeTemplate = (NodeTemplate) nodeTemplates.next();
      if (nodeTemplate.getTemplateFile().endsWith("/" + templateName + ".gtmpl")) {
        roles[0] = nodeTemplate.getRoles();
      }
    }
    if (roles[0] == null) {
      roles[0] = "*";
    }

    // add/update the template content
    templateService.addTemplate(templateType, nodeTypeName, nodeTypeTemplatesMetaData.getLabel(), true, templateName, roles, inputStream);
  }

  private void putData(String nodeTypeName, String contentType, String fileName, InputStream inputStream) throws Exception {
    byte[] bytes = IOUtils.toByteArray(inputStream);
    templatesContent.put("ecmadmin/templates/nodetypes/" + nodeTypeName + "/" + contentType + "/" + fileName, bytes);
  }

}