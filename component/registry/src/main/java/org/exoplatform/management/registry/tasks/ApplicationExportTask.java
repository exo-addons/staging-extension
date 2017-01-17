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
package org.exoplatform.management.registry.tasks;

import org.exoplatform.application.registry.Application;
import org.exoplatform.application.registry.ApplicationRegistryService;
import org.exoplatform.container.xml.ObjectParameter;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.model.ExportTask;
import org.jibx.runtime.BindingDirectory;
import org.jibx.runtime.IBindingFactory;
import org.jibx.runtime.IMarshallingContext;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * The Class ApplicationExportTask.
 *
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class ApplicationExportTask implements ExportTask {
  
  /** The Constant APPLICATION_FILE_BASE_PATH. */
  public static final String APPLICATION_FILE_BASE_PATH = "registry/";
  
  /** The Constant APPLICATION_FILE_SUFFIX. */
  public static final String APPLICATION_FILE_SUFFIX = "_ApplicationRegistry.xml";
  
  /** The application registry service. */
  private ApplicationRegistryService applicationRegistryService;
  
  /** The category name. */
  private String categoryName;
  
  /** The application name. */
  private String applicationName;

  /**
   * Instantiates a new application export task.
   *
   * @param applicationName the application name
   * @param categoryName the category name
   * @param applicationRegistryService the application registry service
   */
  public ApplicationExportTask(String applicationName, String categoryName, ApplicationRegistryService applicationRegistryService) {
    this.categoryName = categoryName;
    this.applicationName = applicationName;
    this.applicationRegistryService = applicationRegistryService;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getEntry() {
    return APPLICATION_FILE_BASE_PATH + categoryName + "_" + applicationName + APPLICATION_FILE_SUFFIX;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void export(OutputStream outputStream) throws IOException {
    Application application = applicationRegistryService.getApplication(categoryName, applicationName);
    if (application == null) {
      throw new OperationException(OperationNames.EXPORT_RESOURCE, "Error while exporting application data: " + getEntry());
    }
    try {
      byte[] bytes = toXML(application);
      outputStream.write(bytes);
    } catch (Exception e) {
      throw new OperationException(OperationNames.EXPORT_RESOURCE, "Error while exporting application data: " + getEntry(), e);
    }
  }

  /**
   * To XML.
   *
   * @param obj the obj
   * @return the byte[]
   * @throws Exception the exception
   */
  protected byte[] toXML(Object obj) throws Exception {
    ObjectParameter objectParameter = new ObjectParameter();
    objectParameter.setName("object");
    objectParameter.setObject(obj);
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    try {
      IBindingFactory bfact = BindingDirectory.getFactory(objectParameter.getClass());
      IMarshallingContext mctx = bfact.createMarshallingContext();
      mctx.setIndent(2);
      mctx.marshalDocument(objectParameter, "UTF-8", null, out);
      return out.toByteArray();
    } catch (Exception ie) {
      throw new OperationException(OperationNames.EXPORT_RESOURCE, "Error while exporting application data: " + getEntry(), ie);
    }
  }
}
