package org.exoplatform.management.ecmadmin.operations.templates.metadata;

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

import org.apache.commons.io.IOUtils;
import org.exoplatform.management.ecmadmin.operations.ECMAdminImportResource;
import org.exoplatform.management.ecmadmin.operations.templates.NodeTemplate;
import org.exoplatform.services.cms.metadata.MetadataService;
import org.exoplatform.services.cms.metadata.impl.MetadataServiceImpl;
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
public class MetadataTemplatesImportResource extends ECMAdminImportResource {
  final private static Logger log = LoggerFactory.getLogger(MetadataTemplatesImportResource.class);
  private MetadataService metadataService;
  private RepositoryService repositoryService;

  private Pattern templateEntryPattern = Pattern.compile("ecmadmin/templates/metadata/(.*)/(.*)/(.*)\\.gtmpl");
  private Pattern metadataEntryPattern = Pattern.compile("ecmadmin/templates/metadata/(.*)/metadata.xml");

  private Map<String, MetadataTemplatesMetaData> metadatas = new HashMap<String, MetadataTemplatesMetaData>();
  private Map<String, byte[]> templatesContent = new HashMap<String, byte[]>();

  public MetadataTemplatesImportResource() {
    super(null);
  }

  public MetadataTemplatesImportResource(String filePath) {
    super(filePath);
  }

  @Override
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws OperationException {
    // get attributes and attachement inputstream
    super.execute(operationContext, resultHandler);

    if (metadataService == null) {
      metadataService = operationContext.getRuntimeContext().getRuntimeComponent(MetadataService.class);
      if (metadataService == null) {
        throw new OperationException(OperationNames.IMPORT_RESOURCE, "MetadataService doesn't exist.");
      }
    }
    if (repositoryService == null) {
      repositoryService = operationContext.getRuntimeContext().getRuntimeComponent(RepositoryService.class);
      if (repositoryService == null) {
        throw new OperationException(OperationNames.EXPORT_RESOURCE, "RepositoryService doesn't exist.");
      }
    }

    try {
      final ZipInputStream zis = new ZipInputStream(attachmentInputStream);
      ZipEntry entry;
      while ((entry = zis.getNextEntry()) != null) {
        String filePath = entry.getName();
        if (!filePath.startsWith("ecmadmin/templates/metadata/")) {
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
          xStream.alias("metadata", MetadataTemplatesMetaData.class);
          xStream.alias("template", NodeTemplate.class);
          MetadataTemplatesMetaData metadata = (MetadataTemplatesMetaData) xStream.fromXML(new InputStreamReader(zis));

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
        updateTemplateContent(templateType, nodeTypeName, templateName, zis);
        zis.closeEntry();
      }
      zis.close();

      Iterator<Map.Entry<String, byte[]>> templatesContentIterator = templatesContent.entrySet().iterator();
      while (templatesContentIterator.hasNext()) {
        Map.Entry<java.lang.String, byte[]> templateContentEntry = (Map.Entry<java.lang.String, byte[]>) templatesContentIterator
            .next();

        Matcher matcher = templateEntryPattern.matcher(templateContentEntry.getKey());
        if (!matcher.find()) {
          continue;
        }

        String nodeTypeName = matcher.group(1);
        String templateType = matcher.group(2);
        String templateName = matcher.group(3);
        updateTemplateContent(templateType, nodeTypeName, templateName, new ByteArrayInputStream(templateContentEntry.getValue()));
      }
    } catch (Exception e) {
      throw new OperationException(OperationNames.IMPORT_RESOURCE, "Error while importing applications templates", e);
    }

    resultHandler.completed(NoResultModel.INSTANCE);
  }

  private void updateTemplateContent(String templateType, String nodeTypeName, String templateName, InputStream inputStream)
      throws Exception {
    boolean isExists = metadataService.hasMetadata(nodeTypeName);
    if (isExists) {
      if (replaceExisting) {
        log.info("Overwrite existing metadata template '" + nodeTypeName + "/" + templateType + "/" + templateName + "'.");
      } else {
        log.info("Ignore existing metadata template '" + nodeTypeName + "/" + templateType + "/" + templateName + "'.");
        return;
      }
    }
    MetadataTemplatesMetaData nodeTypeTemplatesMetaData = metadatas.get(nodeTypeName);
    Iterator<NodeTemplate> nodeTemplates = nodeTypeTemplatesMetaData.getTemplates().get(templateType).iterator();
    String role = null;
    while (nodeTemplates.hasNext() && role == null) {
      NodeTemplate nodeTemplate = (NodeTemplate) nodeTemplates.next();
      if (nodeTemplate.getTemplateFile().endsWith("/" + templateName + ".gtmpl")) {
        role = nodeTemplate.getRoles();
      }
    }
    if (role == null) {
      role = "*";
    }
    boolean isDialog = templateType.equals(MetadataServiceImpl.DIALOGS);
    String content = IOUtils.toString(inputStream);

    // add/update the metadata template content
    metadataService.addMetadata(nodeTypeName, isDialog, role, content, false);
  }

  private void putData(String nodeTypeName, String contentType, String fileName, InputStream inputStream) throws Exception {
    byte[] bytes = IOUtils.toByteArray(inputStream);
    templatesContent.put("ecmadmin/templates/metadata/" + nodeTypeName + "/" + contentType + "/" + fileName, bytes);
  }

}