package org.exoplatform.management.ecmadmin.operations;

import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.operation.*;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Iterator;

/**
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public abstract class ECMAdminImportResource implements OperationHandler {

  protected InputStream attachmentInputStream = null;
  protected Boolean replaceExisting = null;
  private String filePath = null;

  /**
   * Used for ECMAdminContentImportResource to invoke import on a selected
   * file
   * 
   * @param filePath
   *          attachement file path
   */
  public ECMAdminImportResource(String filePath) {
    this.filePath = filePath;
  }

  @Override
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws OperationException {
    // get attribute 'replaceExisting'
    OperationAttributes attributes = operationContext.getAttributes();
    if (attributes != null && attributes.getValues("filter") != null && !attributes.getValues("filter").isEmpty()) {
      Iterator<String> filters = attributes.getValues("filter").iterator();
      while (filters.hasNext() && replaceExisting == null) {
        String filter = filters.next();
        if (filter.startsWith("replace-existing:")) {
          replaceExisting = Boolean.parseBoolean(filter.substring("replace-existing:".length()));
        }
      }
    }
    if (replaceExisting == null) {
      replaceExisting = true;
    }

    // get attachement input stream
    try {
      if (filePath == null) {
        OperationAttachment attachment = operationContext.getAttachment(false);
        attachmentInputStream = attachment.getStream();
      } else {
        attachmentInputStream = new FileInputStream(filePath);
      }
    } catch (Throwable exception) {
      throw new OperationException(OperationNames.IMPORT_RESOURCE, "Error while proceeding taxonomy import.");
    }
    if (attachmentInputStream == null) {
      throw new OperationException(OperationNames.IMPORT_RESOURCE, "No data stream available for taxonomy import.");
    }
  }

}
