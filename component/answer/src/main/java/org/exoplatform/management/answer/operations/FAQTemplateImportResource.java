package org.exoplatform.management.answer.operations;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.pdfbox.io.IOUtils;
import org.exoplatform.faq.service.DataStorage;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.exceptions.ResourceNotFoundException;
import org.gatein.management.api.operation.OperationAttachment;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.OperationHandler;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.NoResultModel;

public class FAQTemplateImportResource implements OperationHandler {
  private static final Log log = ExoLogger.getLogger(FAQTemplateImportResource.class);

  @Override
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws ResourceNotFoundException, OperationException {

    OperationAttachment attachment = operationContext.getAttachment(false);
    if (attachment == null) {
      throw new OperationException(OperationNames.IMPORT_RESOURCE, "No attachment available for FAQ import.");
    }

    InputStream attachmentInputStream = attachment.getStream();
    if (attachmentInputStream == null) {
      throw new OperationException(OperationNames.IMPORT_RESOURCE, "No data stream available for FAQ import.");
    }

    ZipInputStream zis = new ZipInputStream(attachmentInputStream);
    ZipEntry entry;
    try {
      while ((entry = zis.getNextEntry()) != null) {
        String filePath = entry.getName();
        // Skip entries not managed by this extension
        if (filePath.equals("") || !filePath.equals(FAQTemplateExportResource.TEMPLATE_PATH_ENTRY)) {
          continue;
        }

        // Skip directories
        if (entry.isDirectory()) {
          continue;
        }

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        IOUtils.copy(zis, bytes);
        zis.closeEntry();

        DataStorage dataStorage = operationContext.getRuntimeContext().getRuntimeComponent(DataStorage.class);
        dataStorage.saveTemplate(bytes.toString());
        break;
      }
    } catch (Exception e) {
      throw new OperationException(OperationNames.IMPORT_RESOURCE, "Error while importing FAQ template", e);
    } finally {
      try {
        attachmentInputStream.close();
      } catch (IOException e) {
        log.warn("Error while closing attachement stream.", e);
      }
    }
    resultHandler.completed(NoResultModel.INSTANCE);
  }
}