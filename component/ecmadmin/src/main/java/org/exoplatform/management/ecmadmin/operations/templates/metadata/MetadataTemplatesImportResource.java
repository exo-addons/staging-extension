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
package org.exoplatform.management.ecmadmin.operations.templates.metadata;

import com.thoughtworks.xstream.XStream;

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

/**
 * The Class MetadataTemplatesImportResource.
 *
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class MetadataTemplatesImportResource extends ECMAdminImportResource {
  
  /** The Constant log. */
  final private static Logger log = LoggerFactory.getLogger(MetadataTemplatesImportResource.class);
  
  /** The metadata service. */
  private MetadataService metadataService;
  
  /** The repository service. */
  private RepositoryService repositoryService;

  /** The template entry pattern. */
  private Pattern templateEntryPattern = Pattern.compile("ecmadmin/templates/metadata/(.*)/(.*)/(.*)\\.gtmpl");
  
  /** The metadata entry pattern. */
  private Pattern metadataEntryPattern = Pattern.compile("ecmadmin/templates/metadata/(.*)/metadata.xml");

  /** The metadatas. */
  private Map<String, MetadataTemplatesMetaData> metadatas = new HashMap<String, MetadataTemplatesMetaData>();
  
  /** The templates content. */
  private Map<String, byte[]> templatesContent = new HashMap<String, byte[]>();

  /**
   * Instantiates a new metadata templates import resource.
   */
  public MetadataTemplatesImportResource() {
    super(null);
  }

  /**
   * Instantiates a new metadata templates import resource.
   *
   * @param filePath the file path
   */
  public MetadataTemplatesImportResource(String filePath) {
    super(filePath);
  }

  /**
   * {@inheritDoc}
   */
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

    metadatas.clear();

    try {
      final ZipInputStream zis = new ZipInputStream(attachmentInputStream);
      try {
        ZipEntry entry;
        while ((entry = zis.getNextEntry()) != null) {
          try {
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
          } finally {
            zis.closeEntry();
          }
        }
      } finally {
        zis.close();
      }

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
        if (!metadatas.containsKey(nodeTypeName)) {
          continue;
        }
        updateTemplateContent(templateType, nodeTypeName, templateName, new ByteArrayInputStream(templateContentEntry.getValue()));
      }
    } catch (Exception e) {
      throw new OperationException(OperationNames.IMPORT_RESOURCE, "Error while importing metadata templates", e);
    }

    resultHandler.completed(NoResultModel.INSTANCE);
  }

  /**
   * Update template content.
   *
   * @param templateType the template type
   * @param nodeTypeName the node type name
   * @param templateName the template name
   * @param inputStream the input stream
   * @throws Exception the exception
   */
  private void updateTemplateContent(String templateType, String nodeTypeName, String templateName, InputStream inputStream) throws Exception {
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

  /**
   * Put data.
   *
   * @param nodeTypeName the node type name
   * @param contentType the content type
   * @param fileName the file name
   * @param inputStream the input stream
   * @throws Exception the exception
   */
  private void putData(String nodeTypeName, String contentType, String fileName, InputStream inputStream) throws Exception {
    byte[] bytes = IOUtils.toByteArray(inputStream);
    templatesContent.put("ecmadmin/templates/metadata/" + nodeTypeName + "/" + contentType + "/" + fileName, bytes);
  }

}