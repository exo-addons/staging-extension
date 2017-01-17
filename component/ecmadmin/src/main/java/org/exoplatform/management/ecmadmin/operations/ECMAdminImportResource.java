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
package org.exoplatform.management.ecmadmin.operations;

import org.exoplatform.management.common.AbstractOperationHandler;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.operation.OperationAttachment;
import org.gatein.management.api.operation.OperationAttributes;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.ResultHandler;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;

/**
 * The Class ECMAdminImportResource.
 *
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public abstract class ECMAdminImportResource extends AbstractOperationHandler {

  /** The attachment input stream. */
  protected InputStream attachmentInputStream = null;
  
  /** The replace existing. */
  protected Boolean replaceExisting = null;
  
  /** The file path. */
  private String filePath = null;

  /**
   * Used for ECMAdminContentImportResource to invoke import on a selected file.
   *
   * @param filePath attachement file path
   */
  public ECMAdminImportResource(String filePath) {
    this.filePath = filePath;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws OperationException {
    // get attribute 'replaceExisting'
    OperationAttributes attributes = operationContext.getAttributes();
    List<String> filters = attributes.getValues("filter");

    // "replace-existing" attribute. Defaults to false.
    replaceExisting = filters.contains("replace-existing:true");

    // get attachement input stream
    try {
      if (filePath == null) {
        OperationAttachment attachment = operationContext.getAttachment(false);
        attachmentInputStream = attachment.getStream();
      } else {
        attachmentInputStream = new FileInputStream(filePath);
      }
    } catch (Throwable exception) {
      throw new OperationException(OperationNames.IMPORT_RESOURCE, "Error while importing data.", exception);
    }
    if (attachmentInputStream == null) {
      throw new OperationException(OperationNames.IMPORT_RESOURCE, "No data stream available for taxonomy import.");
    }
  }

}
