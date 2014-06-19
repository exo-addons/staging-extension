package org.exoplatform.management.answer.operations;

import java.io.IOException;
import java.io.OutputStream;

import org.exoplatform.faq.service.DataStorage;
import org.exoplatform.faq.service.Utils;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.exceptions.ResourceNotFoundException;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.OperationHandler;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.ExportResourceModel;
import org.gatein.management.api.operation.model.ExportTask;

public class FAQTemplateExportResource implements OperationHandler {

  public static final String TEMPLATE_PATH_ENTRY = new StringBuilder("answer/template/").append(Utils.UI_FAQ_VIEWER).append(".gtmpl").toString();

  @Override
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws ResourceNotFoundException, OperationException {
    DataStorage dataStorage = operationContext.getRuntimeContext().getRuntimeComponent(DataStorage.class);
    resultHandler.completed(new ExportResourceModel(new FAQExportTask(dataStorage)));
  }

  public static class FAQExportTask implements ExportTask {
    private static final Log log = ExoLogger.getLogger(FAQExportTask.class);

    private final DataStorage dataStorage;

    public FAQExportTask(DataStorage dataStorage) {
      this.dataStorage = dataStorage;
    }

    @Override
    public String getEntry() {
      return TEMPLATE_PATH_ENTRY;
    }

    @Override
    public void export(OutputStream outputStream) throws IOException {
      try {
        outputStream.write(dataStorage.getTemplate());
      } catch (Exception e) {
        log.error("Error while exporting FAQ template", e);
      }
    }
  }

}
